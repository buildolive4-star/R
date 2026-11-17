package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass

@Keep
@IgnoreExtraProperties
data class GalleryImage(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Date Night",
    val imageUrl: String = "",
    val thumbUrl: String = "",
    val aspectRatio: Double = 9.0 / 16.0,
    val price: String = "",
    val status: String = "published",
    val featured: Boolean = false,
    val uploadedBy: String = "",
    val uploaderName: String = "Aria · Companion",
    val tags: List<String> = emptyList(),
    val isHidden: Boolean = false,
    val revealExpiresAtEpochMs: Long = 0L,
    val isExpired: Boolean = false,
    val refreshEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val displayThumbnailUrl: String
        get() = thumbUrl.ifBlank { imageUrl }

    val safeAspectRatio: Float
        get() = aspectRatio.toFloat().coerceIn(0.5f, 2.2f)

    val displayPrice: String
        get() = price.trim().ifBlank { "Free" }

    val isRevealStarted: Boolean
        get() = isHidden && !isExpired && revealExpiresAtEpochMs > 0L

    val isDeletedBlurred: Boolean
        get() = isExpired || (isHidden && revealExpiresAtEpochMs > 0L && System.currentTimeMillis() >= revealExpiresAtEpochMs)

    fun remainingSeconds(nowMs: Long = System.currentTimeMillis()): Int {
        if (isExpired) return 0
        if (!isRevealStarted) return 20
        val diffMs = (revealExpiresAtEpochMs - nowMs).coerceAtLeast(0L)
        return ((diffMs + 999L) / 1000L).toInt()
    }
}

@JsonClass(generateAdapter = true)
data class SavedBookmark(
    val imageId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val thumbUrl: String = "",
    val aspectRatio: Double = 9.0 / 16.0,
    val price: String = "",
    val uploaderName: String = "Aria · Companion",
    val tags: List<String> = emptyList(),
    val savedAtEpochMs: Long = 0L,
) {
    fun toGalleryImage(): GalleryImage = GalleryImage(
        id = imageId,
        title = title,
        description = description,
        category = category,
        imageUrl = imageUrl,
        thumbUrl = thumbUrl,
        aspectRatio = aspectRatio,
        price = price,
        uploaderName = uploaderName,
        tags = tags,
    )
}

data class CategoryCollection(
    val name: String,
    val count: Int,
    val coverUrl: String,
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
