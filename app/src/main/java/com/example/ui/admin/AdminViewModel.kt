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
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
) : ViewModel() {

    private val _publishState = MutableStateFlow<AdminPublishState>(AdminPublishState.Idle)
    val publishState: StateFlow<AdminPublishState> = _publishState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _uploadedPreviewAlert = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 4)
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
        val cleanDescription = description.trim()
        val parsedTags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(10)

        viewModelScope.launch {
            val storedUpload: StoredTestUpload = if (selectedUri != null) {
                _publishState.value = AdminPublishState.UploadingToImgBb
                val imgBbResult = ImgBbUploadHelper.uploadUriToImgBb(
                    context = context.applicationContext,
                    apiService = imgBbApiService,
                    imageUri = selectedUri,
                    title = cleanTitle,
                    category = cleanCategory,
                )
                val uploaded = imgBbResult.getOrElse { err ->
                    _publishState.value = AdminPublishState.Error(
                        err.localizedMessage ?: "ImgBB upload failed. Check internet connection."
                    )
                    return@launch
                }
                uploaded.copy(
                    description = cleanDescription,
                    featured = featured,
                    tags = parsedTags,
                    isHidden = isHidden,
                    revealExpiresAtEpochMs = 0L,
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
                    aspectRatio = 1.0,
                    createdAtEpochMs = System.currentTimeMillis(),
                    description = cleanDescription,
                    featured = featured,
                    tags = parsedTags,
                    isHidden = isHidden,
                    revealExpiresAtEpochMs = 0L,
                )
            }

            uploadsRepository.addUpload(storedUpload)
            _uploadedPreviewAlert.tryEmit(storedUpload.toGalleryImage())
            _publishState.value = AdminPublishState.Success(
                message = if (isHidden) {
                    "Hidden photo uploaded! Deletes 20s after reveal."
                } else {
                    "Photo uploaded! Live in gallery."
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
            _events.tryEmit(if (nowFeatured) "Marked as Spotlight" else "Removed from Spotlight")
        }
    }

    fun updateMetadata(
        imageId: String,
        title: String,
        description: String,
        category: String,
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
                featured = featured,
                tags = parsedTags,
                isHidden = isHidden,
            )
            _events.tryEmit("Artwork updated")
        }
    }

    fun removeUpload(imageId: String) {
        viewModelScope.launch {
            uploadsRepository.removeUpload(imageId)
            _events.tryEmit("Removed uploaded photo")
        }
    }

    fun clearAllUploads() {
        viewModelScope.launch {
            uploadsRepository.clearAll()
            _events.tryEmit("Cleared uploaded photos")
        }
    }
}
