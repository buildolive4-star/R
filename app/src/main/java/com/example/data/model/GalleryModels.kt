package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass

object ProfileTier {
    const val VIP_16_9 = "vip_16_9"
    const val GRID_9_16 = "grid_9_16"
}

object LeadTag {
    const val NONE = "none"
    const val HOT = "hot"
    const val WARM = "warm"
    const val COLD = "cold"
}

object ModerationStatus {
    const val ACTIVE = "active"
    const val BLOCKED = "blocked"
    const val SUSPENDED = "suspended"
}

object StaffStatus {
    const val NONE = "none"
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
}

@Keep
data class PhotoRevealState(
    val expiresAtEpochMs: Long = 0L,
    val isExpired: Boolean = false,
)

@Keep
@IgnoreExtraProperties
data class CompanionProfile(
    @DocumentId val id: String = "",
    val name: String = "",
    val age: Int = 21,
    val price: String = "₹299",
    val role: String = "GF",
    val category: String = "Companion",
    val tier: String = ProfileTier.GRID_9_16,
    val bio: String = "",
    val city: String = "",
    val photoUrls: List<String> = emptyList(),
    val coverUrl: String = "",
    val aspectRatio: Double = 9.0 / 16.0,
    val hiddenTimed: Boolean = false,
    val refreshEpochMs: Long = 0L,
    val status: String = "published",
    val ownerUid: String = "",
    val staffVerified: Boolean = true,
    val staffOnline: Boolean = false,
    val createdByUid: String = "",
    val filterTags: List<String> = emptyList(),
    val advanceBookingAmount: Int = 299,
    val bookingPlans: List<String> = emptyList(),
    val videoCallEnabled: Boolean = true,
    val voiceCallPricePerMin: Int = 0,
    val videoCallPricePerMin: Int = 0,
    val chatPricePerMin: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val primaryPhotoUrl: String
        get() = coverUrl.ifBlank { photoUrls.firstOrNull().orEmpty() }

    val allPhotos: List<String>
        get() = if (photoUrls.isNotEmpty()) photoUrls else listOfNotNull(coverUrl.takeIf { it.isNotBlank() })

    val isVip: Boolean
        get() = tier == ProfileTier.VIP_16_9

    fun toGalleryImage(
        revealExpiresAtEpochMs: Long = 0L,
        isExpired: Boolean = false,
        photoRevealStates: Map<Int, PhotoRevealState> = emptyMap(),
    ): GalleryImage = GalleryImage(
        id = id,
        title = name,
        description = bio,
        category = category,
        imageUrl = primaryPhotoUrl,
        thumbUrl = primaryPhotoUrl,
        photoUrls = allPhotos,
        aspectRatio = if (isVip) 16.0 / 9.0 else 9.0 / 16.0,
        price = price,
        age = age,
        role = role,
        tier = tier,
        city = city,
        status = status,
        featured = isVip,
        uploadedBy = createdByUid,
        uploaderName = name,
        ownerUid = ownerUid,
        staffOnline = staffOnline,
        tags = filterTags,
        filterTags = filterTags,
        advanceBookingAmount = advanceBookingAmount,
        bookingPlans = bookingPlans,
        videoCallEnabled = videoCallEnabled,
        voiceCallPricePerMin = voiceCallPricePerMin,
        videoCallPricePerMin = videoCallPricePerMin,
        chatPricePerMin = chatPricePerMin,
        isHidden = hiddenTimed,
        revealExpiresAtEpochMs = revealExpiresAtEpochMs,
        isExpired = isExpired,
        photoRevealStates = photoRevealStates,
        refreshEpochMs = refreshEpochMs,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

@Keep
@IgnoreExtraProperties
data class CompanionPrivateContact(
    val phone: String = "",
    val address: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val galleryUrls: List<String> = emptyList(),
    val phone: String = "",
    val age: Int = 0,
    val address: String = "",
    val city: String = "",
    val bio: String = "",
    val interests: String = "",
    val genderPreference: String = "",
    val completionPercent: Int = 0,
    val online: Boolean = false,
    val lastActiveEpochMs: Long = 0L,
    val hasInquiryNotification: Boolean = false,
    val leadTag: String = LeadTag.NONE,
    val moderationStatus: String = ModerationStatus.ACTIVE,
    val staffStatus: String = StaffStatus.NONE,
    val staffRole: String = "",
    val staffPrice: String = "",
    val staffTier: String = ProfileTier.GRID_9_16,
    val staffProfileId: String = "",
    val directChatEnabled: Boolean = true,
    val staffOnline: Boolean = false,
    val videoCallEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val isApprovedStaff: Boolean
        get() = staffStatus == StaffStatus.APPROVED && moderationStatus == ModerationStatus.ACTIVE

    val canDirectChatAsStaff: Boolean
        get() = isApprovedStaff && directChatEnabled

    val isBlockedOrSuspended: Boolean
        get() = moderationStatus == ModerationStatus.BLOCKED || moderationStatus == ModerationStatus.SUSPENDED

    val isRecentlyOnline: Boolean
        get() = online || (lastActiveEpochMs > 0L && (System.currentTimeMillis() - lastActiveEpochMs) <= 10 * 60_000L)

    val hasInquiryReadyDetails: Boolean
        get() = name.trim().length >= 2 &&
            phone.count { it.isDigit() } >= 8 &&
            age in 18..99 &&
            address.trim().length >= 4
}

@Keep
@IgnoreExtraProperties
data class AdminEmailEntry(
    @DocumentId val emailKey: String = "",
    val email: String = "",
    val addedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdminAccessCode(
    @DocumentId val code: String = "",
    val label: String = "",
    val createdByEmail: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AdminMemberProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val codeUsed: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class MessageTickState {
    SENT,
    DELIVERED,
    READ,
}

@Keep
@IgnoreExtraProperties
data class PresenceStatus(
    @DocumentId val id: String = "",
    val online: Boolean = false,
    val lastSeenEpochMs: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isEffectivelyOnline(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!online) return false
        if (lastSeenEpochMs <= 0L) return true
        return (nowMs - lastSeenEpochMs) <= 15 * 60_000L
    }

    fun formatPresenceLabel(nowMs: Long = System.currentTimeMillis()): String {
        if (isEffectivelyOnline(nowMs)) return "Online"
        if (lastSeenEpochMs <= 0L) return "Offline"
        val diffSec = ((nowMs - lastSeenEpochMs) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "last seen just now"
            diffSec < 3600L -> "last seen ${diffSec / 60L}m ago"
            diffSec < 86_400L -> "last seen ${diffSec / 3600L}h ago"
            else -> "last seen ${diffSec / 86_400L}d ago"
        }
    }
}

@Keep
@IgnoreExtraProperties
data class LaunchBannerConfig(
    @DocumentId val id: String = "",
    val active: Boolean = false,
    val imageUrl: String = "",
    val title: String = "",
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val isReadyToDisplay: Boolean
        get() = active && imageUrl.startsWith("https://")
}

@Keep
@IgnoreExtraProperties
data class HomeFiltersConfig(
    @DocumentId val id: String = "",
    val filters: List<String> = emptyList(),
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val clientUid: String = "",
    val clientName: String = "",
    val clientPhotoUrl: String = "",
    val profileId: String = "",
    val profileName: String = "",
    val profilePhotoUrl: String = "",
    val profilePrice: String = "",
    val staffOwnerUid: String = "",
    val lastMessage: String = "",
    val lastSenderRole: String = "",
    val unreadForAdmin: Int = 0,
    val unreadForStaff: Int = 0,
    val unreadForClient: Int = 0,
    val clientLastReadEpochMs: Long = 0L,
    val agentLastReadEpochMs: Long = 0L,
    val clientLastDeliveredEpochMs: Long = 0L,
    val agentLastDeliveredEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun tickStateForOutgoingMessage(
        message: ChatMessage,
        viewerRole: String,
        peerOnline: Boolean,
    ): MessageTickState {
        val msgMs = message.createdAt?.toDate()?.time ?: 0L
        val peerReadMs = if (viewerRole == "client") agentLastReadEpochMs else clientLastReadEpochMs
        val peerDeliveredMs = if (viewerRole == "client") agentLastDeliveredEpochMs else clientLastDeliveredEpochMs
        return when {
            msgMs > 0L && peerReadMs >= msgMs -> MessageTickState.READ
            (msgMs > 0L && peerDeliveredMs >= msgMs) || (msgMs > 0L && peerOnline) -> MessageTickState.DELIVERED
            else -> MessageTickState.SENT
        }
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderUid: String = "",
    val senderRole: String = "client", // "client", "staff", "admin"
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class GalleryImage(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Date Night",
    val imageUrl: String = "",
    val thumbUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val aspectRatio: Double = 9.0 / 16.0,
    val price: String = "",
    val age: Int = 21,
    val role: String = "GF",
    val tier: String = ProfileTier.GRID_9_16,
    val city: String = "",
    val status: String = "published",
    val featured: Boolean = false,
    val uploadedBy: String = "",
    val uploaderName: String = "JustUs",
    val ownerUid: String = "",
    val staffOnline: Boolean = false,
    val tags: List<String> = emptyList(),
    val filterTags: List<String> = emptyList(),
    val advanceBookingAmount: Int = 299,
    val bookingPlans: List<String> = emptyList(),
    val videoCallEnabled: Boolean = true,
    val voiceCallPricePerMin: Int = 0,
    val videoCallPricePerMin: Int = 0,
    val chatPricePerMin: Int = 0,
    val isHidden: Boolean = false,
    val revealExpiresAtEpochMs: Long = 0L,
    val isExpired: Boolean = false,
    val photoRevealStates: Map<Int, PhotoRevealState> = emptyMap(),
    val refreshEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val displayThumbnailUrl: String
        get() = thumbUrl.ifBlank { imageUrl.ifBlank { photoUrls.firstOrNull().orEmpty() } }

    val allPhotoUrls: List<String>
        get() = if (photoUrls.isNotEmpty()) photoUrls else listOfNotNull(imageUrl.takeIf { it.isNotBlank() })

    val photoCount: Int
        get() = allPhotoUrls.size.coerceAtLeast(1)

    val safeAspectRatio: Float
        get() = aspectRatio.toFloat().coerceIn(0.45f, 2.2f)

    val displayPrice: String
        get() = price.trim().ifBlank { "Free" }

    val isVip: Boolean
        get() = tier == ProfileTier.VIP_16_9

    val allFilterTags: List<String>
        get() = (tags + filterTags).map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }

    fun resolvedBookingPlans(): List<BookingRatePlan> =
        BookingRatePlan.parseList(bookingPlans, displayPrice)

    fun resolvedAdvanceAmountInr(defaultFallback: Int = 299): Int =
        if (advanceBookingAmount > 0) advanceBookingAmount else defaultFallback.coerceAtLeast(1)

    fun photoState(photoIndex: Int): PhotoRevealState {
        photoRevealStates[photoIndex]?.let { return it }
        if (photoRevealStates.isEmpty() && photoIndex == 0 && (isExpired || revealExpiresAtEpochMs > 0L)) {
            return PhotoRevealState(expiresAtEpochMs = revealExpiresAtEpochMs, isExpired = isExpired)
        }
        return PhotoRevealState()
    }

    fun isPhotoExpired(photoIndex: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isHidden) return isExpired
        val st = photoState(photoIndex)
        return st.isExpired || (st.expiresAtEpochMs > 0L && nowMs >= st.expiresAtEpochMs)
    }

    fun isPhotoRevealStarted(photoIndex: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isHidden) return false
        val st = photoState(photoIndex)
        return !isPhotoExpired(photoIndex, nowMs) && st.expiresAtEpochMs > 0L
    }

    fun remainingSecondsForPhoto(photoIndex: Int, nowMs: Long = System.currentTimeMillis()): Int {
        if (isPhotoExpired(photoIndex, nowMs)) return 0
        val st = photoState(photoIndex)
        if (!isPhotoRevealStarted(photoIndex, nowMs)) return 40
        val diffMs = (st.expiresAtEpochMs - nowMs).coerceAtLeast(0L)
        return ((diffMs + 999L) / 1000L).toInt()
    }

    fun unopenedPhotoIndices(nowMs: Long = System.currentTimeMillis()): List<Int> {
        if (!isHidden) return emptyList()
        return (0 until photoCount).filter { idx ->
            !isPhotoExpired(idx, nowMs) && photoState(idx).expiresAtEpochMs == 0L
        }
    }

    fun unopenedPhotoCount(nowMs: Long = System.currentTimeMillis()): Int = unopenedPhotoIndices(nowMs).size

    fun activeRevealedPhotoCount(nowMs: Long = System.currentTimeMillis()): Int {
        if (!isHidden) return 0
        return (0 until photoCount).count { idx -> isPhotoRevealStarted(idx, nowMs) }
    }

    fun nextOpenablePhotoIndex(nowMs: Long = System.currentTimeMillis()): Int {
        if (!isHidden) return 0
        val activeIdx = (0 until photoCount).firstOrNull { isPhotoRevealStarted(it, nowMs) }
        if (activeIdx != null) return activeIdx
        return unopenedPhotoIndices(nowMs).firstOrNull() ?: 0
    }

    fun allPhotosExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isHidden) return isExpired
        return (0 until photoCount).all { idx -> isPhotoExpired(idx, nowMs) }
    }

    val isRevealStarted: Boolean
        get() = isPhotoRevealStarted(0)

    val isDeletedBlurred: Boolean
        get() = allPhotosExpired()

    fun remainingSeconds(nowMs: Long = System.currentTimeMillis()): Int = remainingSecondsForPhoto(0, nowMs)
}

@JsonClass(generateAdapter = true)
data class SavedBookmark(
    val imageId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val thumbUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val aspectRatio: Double = 9.0 / 16.0,
    val price: String = "",
    val age: Int = 21,
    val role: String = "GF",
    val tier: String = ProfileTier.GRID_9_16,
    val city: String = "",
    val uploaderName: String = "JustUs",
    val ownerUid: String = "",
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
        photoUrls = photoUrls,
        aspectRatio = aspectRatio,
        price = price,
        age = age,
        role = role,
        tier = tier,
        city = city,
        uploaderName = uploaderName,
        ownerUid = ownerUid,
        tags = tags,
    )
}

data class CategoryCollection(
    val name: String,
    val count: Int,
    val coverUrl: String,
)

enum class AdminUserFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    NOTIFIED("Notified"),
    OLD_USERS("Old Users"),
    HOT_LEAD("Hot Lead"),
    WARM_LEAD("Warm Lead"),
    COLD_LEAD("Cold Lead"),
    BLOCKED("Blocked"),
    SUSPENDED("Suspended"),
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
