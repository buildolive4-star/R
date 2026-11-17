package com.example.ui.admin

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AdminAuthHelper
import com.example.data.model.GalleryImage
import com.example.data.model.UiState
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.remote.ImgBbApiService
import com.example.data.remote.ImgBbUploadHelper
import com.example.data.repository.GalleryRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AdminPublishState {
    data object Idle : AdminPublishState
    data object UploadingToImgBb : AdminPublishState
    data object PublishingToCloud : AdminPublishState
    data class Success(val message: String, val imageUrl: String) : AdminPublishState
    data class Error(val message: String) : AdminPublishState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val repository: GalleryRepository,
    private val testUploadsRepository: LocalTestUploadsRepository,
    private val notifiedImagesStore: NotifiedImagesStore,
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
) : ViewModel() {

    val adminUser: StateFlow<FirebaseUser?> = AdminAuthHelper.observeAdminUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    private val _publishState = MutableStateFlow<AdminPublishState>(AdminPublishState.Idle)
    val publishState: StateFlow<AdminPublishState> = _publishState.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _uploadedPreviewAlert = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 4)
    val uploadedPreviewAlert: SharedFlow<GalleryImage> = _uploadedPreviewAlert.asSharedFlow()

    val adminImagesState: StateFlow<UiState<List<GalleryImage>>> = adminUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllImagesForAdmin()
                    .map<List<GalleryImage>, UiState<List<GalleryImage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Admin stream error", rootError)
                        emit(UiState.Error(rootError.message ?: "Unable to load admin gallery"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val devicePreviewUploads: StateFlow<List<GalleryImage>> = testUploadsRepository.testUploads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    fun attemptSilentAdminSignIn(context: Context) {
        viewModelScope.launch {
            AdminAuthHelper.attemptSilentSignIn(context)
        }
    }

    fun signInAdminWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            val result = AdminAuthHelper.signInWithGoogle(activityContext)
            _isAuthLoading.value = false
            result.onSuccess { user ->
                _events.tryEmit("Connected as ${user.email ?: "Admin"}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Google sign-in cancelled")
            }
        }
    }

    fun signOutAdmin(context: Context) {
        viewModelScope.launch {
            AdminAuthHelper.signOut(context)
            _events.tryEmit("Disconnected admin Google session")
        }
    }

    fun uploadAndPublish(
        context: Context,
        selectedUri: Uri?,
        manualImageUrl: String,
        title: String,
        description: String,
        category: String,
        tagsRaw: String,
        featured: Boolean,
        publishImmediately: Boolean,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            _publishState.value = AdminPublishState.Error("Please enter an image title")
            return
        }
        if (selectedUri == null && manualImageUrl.trim().isEmpty()) {
            _publishState.value = AdminPublishState.Error("Select a photo or enter an https:// image URL")
            return
        }

        val parsedTags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        viewModelScope.launch {
            var resolvedImageUrl = manualImageUrl.trim()
            var resolvedThumbUrl = resolvedImageUrl
            var resolvedAspect = 1.0

            if (selectedUri != null) {
                _publishState.value = AdminPublishState.UploadingToImgBb
                val imgBbResult = ImgBbUploadHelper.uploadUriToImgBb(
                    context = context.applicationContext,
                    apiService = imgBbApiService,
                    imageUri = selectedUri,
                    title = cleanTitle,
                    category = category,
                )
                val uploaded = imgBbResult.getOrElse { err ->
                    _publishState.value = AdminPublishState.Error(
                        err.localizedMessage ?: "ImgBB upload failed"
                    )
                    return@launch
                }
                resolvedImageUrl = uploaded.imageUrl
                resolvedThumbUrl = uploaded.thumbUrl
                resolvedAspect = uploaded.aspectRatio

                // Always keep device preview if Google Admin isn't signed in yet
                if (adminUser.value == null) {
                    testUploadsRepository.addUpload(uploaded)
                    val galleryItem = uploaded.toGalleryImage().copy(
                        description = description.trim(),
                        featured = featured,
                        tags = parsedTags,
                    )
                    _uploadedPreviewAlert.tryEmit(galleryItem)
                    _publishState.value = AdminPublishState.Success(
                        message = "Uploaded to ImgBB (Device Preview). Connect Admin Google Account to publish to all users via Cloud.",
                        imageUrl = resolvedImageUrl,
                    )
                    return@launch
                }
            }

            if (adminUser.value == null) {
                _publishState.value = AdminPublishState.Error(
                    "Connect Admin Google Account first to publish URLs to Cloud Firestore."
                )
                return@launch
            }

            _publishState.value = AdminPublishState.PublishingToCloud
            val status = if (publishImmediately) "published" else "archived"
            val createResult = repository.createImage(
                title = cleanTitle,
                description = description,
                category = category,
                imageUrl = resolvedImageUrl,
                thumbUrl = resolvedThumbUrl,
                aspectRatio = resolvedAspect,
                featured = featured,
                status = status,
                tags = parsedTags,
            )

            createResult.onSuccess { newDocId ->
                if (status == "published") {
                    // Don't skip immediate preview alert on this device unless desired;
                    // Firestore real-time listener will fire the alert cleanly and mark ID known.
                    notifiedImagesStore.markIdsKnown(emptyList())
                }
                _publishState.value = AdminPublishState.Success(
                    message = if (status == "published") {
                        "Published live to Cloud Gallery ($newDocId)"
                    } else {
                        "Saved as archived draft ($newDocId)"
                    },
                    imageUrl = resolvedImageUrl,
                )
            }.onFailure { err ->
                _publishState.value = AdminPublishState.Error(
                    err.localizedMessage ?: "Failed to save to Firestore. Verify Admin Google Account."
                )
            }
        }
    }

    fun resetPublishState() {
        _publishState.value = AdminPublishState.Idle
    }

    fun toggleFeatured(image: GalleryImage) {
        viewModelScope.launch {
            val result = repository.updateImage(
                imageId = image.id,
                title = image.title,
                description = image.description,
                category = image.category,
                featured = !image.featured,
                status = image.status,
                tags = image.tags,
            )
            result.onSuccess {
                _events.tryEmit(if (!image.featured) "Marked as Spotlight" else "Removed from Spotlight")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update image")
            }
        }
    }

    fun toggleStatus(image: GalleryImage) {
        val nextStatus = if (image.status == "published") "archived" else "published"
        viewModelScope.launch {
            val result = repository.updateImage(
                imageId = image.id,
                title = image.title,
                description = image.description,
                category = image.category,
                featured = image.featured,
                status = nextStatus,
                tags = image.tags,
            )
            result.onSuccess {
                _events.tryEmit(if (nextStatus == "published") "Published live" else "Moved to archived drafts")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to change status")
            }
        }
    }

    fun updateMetadata(
        imageId: String,
        title: String,
        description: String,
        category: String,
        tagsRaw: String,
        featured: Boolean,
        status: String,
    ) {
        val parsedTags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        viewModelScope.launch {
            val result = repository.updateImage(
                imageId = imageId,
                title = title,
                description = description,
                category = category,
                featured = featured,
                status = status,
                tags = parsedTags,
            )
            result.onSuccess {
                _events.tryEmit("Artwork updated")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to update artwork")
            }
        }
    }

    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            val result = repository.deleteImage(imageId)
            result.onSuccess {
                _events.tryEmit("Deleted from cloud gallery")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete artwork")
            }
        }
    }

    fun clearDevicePreviews() {
        viewModelScope.launch {
            testUploadsRepository.clearAll()
            _events.tryEmit("Cleared local device previews")
        }
    }

    companion object {
        private const val TAG = "AdminVM"
    }
}
