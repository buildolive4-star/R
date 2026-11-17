package com.example.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GalleryImage
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.StoredTestUpload
import com.example.data.remote.ImgBbApiService
import com.example.data.remote.ImgBbUploadHelper
import com.example.data.repository.GalleryRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface AdminPublishState {
    data object Idle : AdminPublishState
    data object UploadingToImgBb : AdminPublishState
    data class Success(val message: String, val imageUrl: String) : AdminPublishState
    data class Error(val message: String) : AdminPublishState
}

class AdminViewModel(
    private val uploadsRepository: LocalTestUploadsRepository,
    private val galleryRepository: GalleryRepository? = null,
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
) : ViewModel() {

    private val _publishState = MutableStateFlow<AdminPublishState>(AdminPublishState.Idle)
    val publishState: StateFlow<AdminPublishState> = _publishState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _uploadedPreviewAlert = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 8)
    val uploadedPreviewAlert: SharedFlow<GalleryImage> = _uploadedPreviewAlert.asSharedFlow()

    val uploadedImages: StateFlow<List<GalleryImage>> = uploadsRepository.testUploads
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    fun uploadPicture(
        context: Context,
        selectedUri: Uri?,
        manualImageUrl: String,
        title: String,
        description: String,
        category: String,
        price: String,
        tagsRaw: String,
        featured: Boolean,
        isHidden: Boolean,
    ) {
        val rawManual = manualImageUrl.trim()
        if (selectedUri == null && rawManual.isEmpty()) {
            _publishState.value = AdminPublishState.Error("Please select a photo from your device first")
            return
        }

        val cleanTitle = title.trim().ifBlank {
            val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
            if (isHidden) "Secret Drop · $stamp" else "Aria · $stamp"
        }
        val cleanCategory = category.trim().ifBlank { "Date Night" }
        val cleanPrice = price.trim().ifBlank { "₹299" }.take(32)
        val cleanDescription = description.trim()
        val parsedTags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        viewModelScope.launch {
            val storedUpload: StoredTestUpload = if (selectedUri != null) {
                _publishState.value = AdminPublishState.UploadingToImgBb
                val uploadResult = if (ImgBbApiService.isApiKeyConfigured()) {
                    ImgBbUploadHelper.uploadUriToImgBb(
                        context = context.applicationContext,
                        apiService = imgBbApiService,
                        imageUri = selectedUri,
                        title = cleanTitle,
                        category = cleanCategory,
                    )
                } else {
                    ImgBbUploadHelper.saveUriLocallyForDeviceFeed(
                        context = context.applicationContext,
                        imageUri = selectedUri,
                        title = cleanTitle,
                        category = cleanCategory,
                    )
                }
                val uploaded = uploadResult.getOrElse { err ->
                    _publishState.value = AdminPublishState.Error(
                        err.localizedMessage ?: "Unable to upload selected photo."
                    )
                    return@launch
                }
                uploaded.copy(
                    description = cleanDescription,
                    price = cleanPrice,
                    featured = featured,
                    tags = parsedTags,
                    isHidden = isHidden,
                    revealExpiresAtEpochMs = 0L,
                    isExpired = false,
                    refreshEpochMs = System.currentTimeMillis(),
                )
            } else {
                val normalized = if (rawManual.startsWith("http://")) {
                    "https://" + rawManual.removePrefix("http://")
                } else {
                    rawManual
                }
                if (!normalized.startsWith("https://") || normalized.length < 10) {
                    _publishState.value = AdminPublishState.Error("Enter a valid https:// image link or select a photo")
                    return@launch
                }
                StoredTestUpload(
                    id = "url_${UUID.randomUUID().toString().take(8)}",
                    title = cleanTitle,
                    category = cleanCategory,
                    imageUrl = normalized,
                    thumbUrl = normalized,
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = System.currentTimeMillis(),
                    description = cleanDescription,
                    price = cleanPrice,
                    featured = featured,
                    tags = parsedTags,
                    isHidden = isHidden,
                    revealExpiresAtEpochMs = 0L,
                    isExpired = false,
                    refreshEpochMs = System.currentTimeMillis(),
                )
            }

            uploadsRepository.addUpload(storedUpload)

            // Best-effort cloud sync if an admin Firebase session is active
            if (galleryRepository != null && Firebase.auth.currentUser != null) {
                galleryRepository.createImage(
                    title = storedUpload.title,
                    description = storedUpload.description,
                    category = storedUpload.category,
                    imageUrl = storedUpload.imageUrl,
                    thumbUrl = storedUpload.thumbUrl,
                    aspectRatio = storedUpload.aspectRatio,
                    featured = storedUpload.featured,
                    status = "published",
                    tags = storedUpload.tags,
                    price = storedUpload.price,
                    isHidden = storedUpload.isHidden,
                    refreshEpochMs = storedUpload.refreshEpochMs,
                )
            }

            _uploadedPreviewAlert.tryEmit(storedUpload.toGalleryImage())
            _publishState.value = AdminPublishState.Success(
                message = if (isHidden) {
                    "Secret 20s drop uploaded ($cleanPrice)! Blurs 20s after unlock."
                } else {
                    "Moment uploaded ($cleanPrice)! Live in gallery."
                },
                imageUrl = storedUpload.imageUrl,
            )
        }
    }

    fun resetPublishState() {
        _publishState.value = AdminPublishState.Idle
    }

    fun toggleFeatured(image: GalleryImage) {
        viewModelScope.launch {
            val nowFeatured = uploadsRepository.toggleFeatured(image.id)
            _events.tryEmit(if (nowFeatured) "Pinned to Featured Drop" else "Removed from Featured Drop")
        }
    }

    fun refreshBlurredUpload(imageId: String) {
        viewModelScope.launch {
            val refreshed = uploadsRepository.refreshUpload(imageId)
            if (galleryRepository != null && Firebase.auth.currentUser != null) {
                galleryRepository.refreshRemoteImage(imageId)
            }
            if (refreshed != null) {
                _uploadedPreviewAlert.tryEmit(refreshed)
                _events.tryEmit("Refreshed! Unlock option is back for users.")
            }
        }
    }

    fun refreshAllBlurredUploads() {
        viewModelScope.launch {
            val refreshedList = uploadsRepository.refreshAllExpiredUploads()
            if (refreshedList.isEmpty()) {
                _events.tryEmit("All secret photos are ready to open & unlock!")
            } else {
                if (galleryRepository != null && Firebase.auth.currentUser != null) {
                    refreshedList.forEach { galleryRepository.refreshRemoteImage(it.id) }
                }
                refreshedList.firstOrNull()?.let { _uploadedPreviewAlert.tryEmit(it) }
                _events.tryEmit("Refreshed ${refreshedList.size} photo(s) — unlock is open again!")
            }
        }
    }

    fun updateMetadata(
        imageId: String,
        title: String,
        description: String,
        category: String,
        price: String,
        tagsRaw: String,
        featured: Boolean,
        isHidden: Boolean,
    ) {
        val parsedTags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        viewModelScope.launch {
            uploadsRepository.updateUpload(
                imageId = imageId,
                title = title,
                description = description,
                category = category,
                price = price,
                featured = featured,
                tags = parsedTags,
                isHidden = isHidden,
            )
            _events.tryEmit("Moment updated")
        }
    }

    fun removeUpload(imageId: String) {
        viewModelScope.launch {
            uploadsRepository.removeUpload(imageId)
            _events.tryEmit("Removed uploaded moment")
        }
    }

    fun clearAllUploads() {
        viewModelScope.launch {
            uploadsRepository.clearAll()
            _events.tryEmit("Cleared uploaded moments")
        }
    }
}
