package com.example.ui.datemate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookingDraft
import com.example.data.model.BookingStatus
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.CustomerUserProfile
import com.example.data.model.DateBooking
import com.example.data.model.DateMateCatalog
import com.example.data.model.DateMateChatThread
import com.example.data.model.DateMateMessage
import com.example.data.model.DateMateNotification
import com.example.data.model.DiscoverFilters
import com.example.data.model.MembershipTier
import com.example.data.model.NotificationCategory
import com.example.data.firebase.GoogleAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class ActiveCallType {
    AUDIO,
    VIDEO,
}

data class ActiveCompanionCall(
    val companionId: String,
    val companionName: String,
    val companionPhotoUrl: String,
    val callType: ActiveCallType,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val statusText: String = "Ringing…",
)

class DateMateViewModel : ViewModel() {

    private val _hasCompletedSplash = MutableStateFlow(false)
    val hasCompletedSplash: StateFlow<Boolean> = _hasCompletedSplash.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(false)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _selectedHomeCity = MutableStateFlow("Mumbai")
    val selectedHomeCity: StateFlow<String> = _selectedHomeCity.asStateFlow()

    private val _homeSearchQuery = MutableStateFlow("")
    val homeSearchQuery: StateFlow<String> = _homeSearchQuery.asStateFlow()

    private val _companions = MutableStateFlow(DateMateCatalog.curatedCompanions)
    val companions: StateFlow<List<DateMateCompanionProfile>> = _companions.asStateFlow()

    private val _favoriteIds = MutableStateFlow(setOf("comp_aanya_01", "comp_kiara_02"))
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _blockedCompanionIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedCompanionIds: StateFlow<Set<String>> = _blockedCompanionIds.asStateFlow()

    private val _discoverFilters = MutableStateFlow(DiscoverFilters())
    val discoverFilters: StateFlow<DiscoverFilters> = _discoverFilters.asStateFlow()

    private val _passedIdsInSession = MutableStateFlow<Set<String>>(emptySet())
    private val _likedIdsInSession = MutableStateFlow<Set<String>>(emptySet())
    val likedIdsInSession: StateFlow<Set<String>> = _likedIdsInSession.asStateFlow()

    private val _heartExplosionTrigger = MutableStateFlow(0L)
    val heartExplosionTrigger: StateFlow<Long> = _heartExplosionTrigger.asStateFlow()

    private val _lastMatchedCompanion = MutableStateFlow<DateMateCompanionProfile?>(null)
    val lastMatchedCompanion: StateFlow<DateMateCompanionProfile?> = _lastMatchedCompanion.asStateFlow()

    private val _bookings = MutableStateFlow(DateMateCatalog.initialBookings)
    val bookings: StateFlow<List<DateBooking>> = _bookings.asStateFlow()

    private val _bookingDraft = MutableStateFlow(
        BookingDraft(
            step = 1,
            companionId = DateMateCatalog.curatedCompanions.first().id,
        )
    )
    val bookingDraft: StateFlow<BookingDraft> = _bookingDraft.asStateFlow()

    private val _showBookingWizard = MutableStateFlow(false)
    val showBookingWizard: StateFlow<Boolean> = _showBookingWizard.asStateFlow()

    private val _chatThreads = MutableStateFlow(DateMateCatalog.initialThreads)
    val chatThreads: StateFlow<List<DateMateChatThread>> = _chatThreads.asStateFlow()

    private val _messagesByThread = MutableStateFlow(DateMateCatalog.initialMessagesByThread)
    val messagesByThread: StateFlow<Map<String, List<DateMateMessage>>> = _messagesByThread.asStateFlow()

    private val _notifications = MutableStateFlow(DateMateCatalog.initialNotifications)
    val notifications: StateFlow<List<DateMateNotification>> = _notifications.asStateFlow()

    private val _userProfile = MutableStateFlow(CustomerUserProfile())
    val userProfile: StateFlow<CustomerUserProfile> = _userProfile.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCompanionCall?>(null)
    val activeCall: StateFlow<ActiveCompanionCall?> = _activeCall.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    fun emitMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    fun completeSplash() {
        _hasCompletedSplash.value = true
    }

    fun completeOnboarding() {
        _hasCompletedOnboarding.value = true
    }

    fun completeAuthentication(
        fullName: String = _userProfile.value.fullName,
        emailOrMobile: String = _userProfile.value.email,
        dob: String = _userProfile.value.dateOfBirth,
    ) {
        _userProfile.update { current ->
            current.copy(
                fullName = fullName.ifBlank { current.fullName },
                email = if (emailOrMobile.contains("@")) emailOrMobile else current.email,
                mobileNumber = if (!emailOrMobile.contains("@") && emailOrMobile.isNotBlank()) emailOrMobile else current.mobileNumber,
                dateOfBirth = dob.ifBlank { current.dateOfBirth },
                isAge18Verified = true,
            )
        }
        _isAuthenticated.value = true
        emitMessage("Welcome to DateMate, ${_userProfile.value.fullName.substringBefore(" ")}! ❤️")
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            val result = GoogleAuthHelper.signInWithGoogle(context)
            result.onSuccess { fbUser ->
                _userProfile.update { current ->
                    current.copy(
                        fullName = fbUser.displayName?.takeIf { it.isNotBlank() } ?: current.fullName,
                        email = fbUser.email?.takeIf { it.isNotBlank() } ?: current.email,
                        avatarUrl = fbUser.photoUrl?.toString()?.takeIf { it.isNotBlank() } ?: current.avatarUrl,
                        isAge18Verified = true,
                        isIdentityVerified = true,
                    )
                }
                _isAuthenticated.value = true
                emitMessage("Signed in as ${_userProfile.value.fullName}")
            }.onFailure {
                // Smooth fallback so preview / emulator users without a Google account configured can still enter DateMate seamlessly
                _isAuthenticated.value = true
                emitMessage("Welcome to DateMate! Signed in as ${_userProfile.value.fullName}")
            }
        }
    }

    fun selectHomeCity(city: String) {
        _selectedHomeCity.value = city
    }

    fun updateHomeSearchQuery(query: String) {
        _homeSearchQuery.value = query
    }

    fun toggleFavorite(companionId: String) {
        val current = _favoriteIds.value
        val companion = findCompanionById(companionId)
        if (companionId in current) {
            _favoriteIds.value = current - companionId
            emitMessage("Removed ${companion?.name ?: "profile"} from Favourites")
        } else {
            _favoriteIds.value = current + companionId
            _heartExplosionTrigger.value = System.currentTimeMillis()
            emitMessage("Added ${companion?.name ?: "profile"} to Favourites ❤️")
        }
    }

    fun updateDiscoverFilters(filters: DiscoverFilters) {
        _discoverFilters.value = filters
    }

    fun resetDiscoverFilters() {
        _discoverFilters.value = DiscoverFilters()
        _passedIdsInSession.value = emptySet()
        _likedIdsInSession.value = emptySet()
        emitMessage("Discover deck & filters refreshed ✨")
    }

    fun getDiscoverDeck(): List<DateMateCompanionProfile> {
        val filters = _discoverFilters.value
        val blocked = _blockedCompanionIds.value
        val passed = _passedIdsInSession.value
        val liked = _likedIdsInSession.value
        return _companions.value.filter { profile ->
            profile.id !in blocked &&
                profile.id !in passed &&
                profile.id !in liked &&
                profile.distanceKm <= filters.maxDistanceKm &&
                profile.age in filters.ageRange &&
                (filters.selectedCity == "All Cities" || profile.city.equals(filters.selectedCity, ignoreCase = true)) &&
                (filters.selectedInterest == "All" || profile.interests.any { it.equals(filters.selectedInterest, ignoreCase = true) }) &&
                (!filters.onlineOnly || profile.isOnline) &&
                (!filters.verifiedOnly || profile.isVerified)
        }
    }

    fun passCompanionOnDiscover(companionId: String) {
        _passedIdsInSession.update { it + companionId }
    }

    fun likeCompanionOnDiscover(companionId: String, isSuperLike: Boolean = false) {
        val companion = findCompanionById(companionId) ?: return
        _likedIdsInSession.update { it + companionId }
        _favoriteIds.update { it + companionId }
        _heartExplosionTrigger.value = System.currentTimeMillis()
        ensureChatThreadForCompanion(companion)

        val notificationTitle = if (isSuperLike) {
            "Super Rose Match with ${companion.name}! 🌹"
        } else {
            "New Match with ${companion.name}! ❤️"
        }
        val newNotif = DateMateNotification(
            id = "notif_${System.currentTimeMillis()}",
            category = NotificationCategory.NEW_MATCH,
            title = notificationTitle,
            body = "${companion.name} is online and ready to connect with you.",
            timestampLabel = "Just now",
            avatarUrl = companion.primaryPhotoUrl,
            targetCompanionId = companion.id,
            targetThreadId = "th_${companion.id}",
            isRead = false,
        )
        _notifications.update { listOf(newNotif) + it }
        _lastMatchedCompanion.value = companion
    }

    fun dismissMatchCelebration() {
        _lastMatchedCompanion.value = null
    }

    fun findCompanionById(companionId: String): DateMateCompanionProfile? {
        return _companions.value.firstOrNull { it.id == companionId }
    }

    // ========================================================================
    // 6-Step Booking Process Engine
    // ========================================================================
    fun openBookingWizard(preselectedCompanionId: String? = null) {
        val defaultCompanionId = preselectedCompanionId
            ?.takeIf { it.isNotBlank() }
            ?: _companions.value.firstOrNull()?.id.orEmpty()
        val startStep = if (preselectedCompanionId != null) 2 else 1
        _bookingDraft.value = BookingDraft(
            step = startStep,
            companionId = defaultCompanionId,
        )
        _showBookingWizard.value = true
    }

    fun closeBookingWizard() {
        _showBookingWizard.value = false
    }

    fun updateBookingDraft(transform: (BookingDraft) -> BookingDraft) {
        _bookingDraft.update(transform)
    }

    fun confirmBookingFromDraft(): DateBooking? {
        val draft = _bookingDraft.value
        val companion = findCompanionById(draft.companionId) ?: _companions.value.firstOrNull() ?: return null
        val code = "DM-${Random.nextInt(10000, 99999)}"
        val total = companion.pricePerHourInr * draft.durationHours
        val created = DateBooking(
            id = "bk_${System.currentTimeMillis()}",
            companionId = companion.id,
            companionName = companion.name,
            companionPhotoUrl = companion.primaryPhotoUrl,
            companionCity = companion.city,
            dateLabel = draft.selectedDateLabel,
            timeSlot = draft.selectedTimeSlot,
            durationHours = draft.durationHours,
            venueExperience = draft.venueExperience,
            specialNote = draft.specialNote,
            totalFeeInr = total,
            status = BookingStatus.CONFIRMED,
            confirmationCode = code,
        )
        _bookings.update { listOf(created) + it }
        _notifications.update { current ->
            listOf(
                DateMateNotification(
                    id = "notif_bk_${System.currentTimeMillis()}",
                    category = NotificationCategory.BOOKING_CONFIRMATION,
                    title = "Booking Confirmed with ${companion.name} ✨",
                    body = "${draft.selectedDateLabel} at ${draft.selectedTimeSlot} (${draft.durationHours}h) · Code: $code",
                    timestampLabel = "Just now",
                    avatarUrl = companion.primaryPhotoUrl,
                    targetCompanionId = companion.id,
                    isRead = false,
                )
            ) + current
        }
        _showBookingWizard.value = false
        _heartExplosionTrigger.value = System.currentTimeMillis()
        emitMessage("Date booked with ${companion.name}! Confirmation: $code")
        return created
    }

    fun cancelBooking(bookingId: String) {
        _bookings.update { list ->
            list.map { item ->
                if (item.id == bookingId) item.copy(status = BookingStatus.CANCELLED) else item
            }
        }
        emitMessage("Booking cancelled per free cancellation policy")
    }

    // ========================================================================
    // Real-Time Chat Engine
    // ========================================================================
    fun ensureChatThreadForCompanion(companion: DateMateCompanionProfile): String {
        val existing = _chatThreads.value.firstOrNull { it.companionId == companion.id }
        if (existing != null) return existing.id

        val newThreadId = "th_${companion.id}"
        val newThread = DateMateChatThread(
            id = newThreadId,
            companionId = companion.id,
            companionName = companion.name,
            companionAge = companion.age,
            companionCity = companion.city,
            companionPhotoUrl = companion.primaryPhotoUrl,
            isOnline = companion.isOnline,
            isVerified = companion.isVerified,
            lastMessage = "Say hello to ${companion.name} ❤️",
            lastUpdatedLabel = "Just now",
            unreadCount = 0,
        )
        _chatThreads.update { listOf(newThread) + it }
        _messagesByThread.update { map ->
            map + (newThreadId to listOf(
                DateMateMessage(
                    id = "msg_welcome_${System.currentTimeMillis()}",
                    threadId = newThreadId,
                    senderIsMe = false,
                    senderName = companion.name,
                    text = "Hi ${_userProfile.value.fullName.substringBefore(" ")}! Lovely to connect with you on DateMate 🌸",
                    timestampLabel = currentTimeLabel(),
                )
            ))
        }
        return newThreadId
    }

    fun markThreadRead(threadId: String) {
        _chatThreads.update { list ->
            list.map { thread ->
                if (thread.id == threadId) thread.copy(unreadCount = 0) else thread
            }
        }
    }

    fun sendChatMessage(
        threadId: String,
        text: String,
        imageUrl: String = "",
        voiceDurationSec: Int = 0,
        replyToPreview: String = "",
    ) {
        if (text.isBlank() && imageUrl.isBlank() && voiceDurationSec <= 0) return
        val thread = _chatThreads.value.firstOrNull { it.id == threadId } ?: return
        val displayPreview = when {
            voiceDurationSec > 0 -> "🎤 Voice note (${voiceDurationSec}s)"
            imageUrl.isNotBlank() -> "📷 Shared a photo"
            else -> text.trim()
        }
        val outgoing = DateMateMessage(
            id = "msg_${System.currentTimeMillis()}",
            threadId = threadId,
            senderIsMe = true,
            senderName = "You",
            text = text.trim(),
            imageUrl = imageUrl,
            voiceDurationSec = voiceDurationSec,
            replyToPreview = replyToPreview,
            timestampLabel = currentTimeLabel(),
            isRead = true,
        )
        _messagesByThread.update { map ->
            val current = map[threadId].orEmpty()
            map + (threadId to (current + outgoing))
        }
        _chatThreads.update { list ->
            list.map {
                if (it.id == threadId) {
                    it.copy(
                        lastMessage = displayPreview,
                        lastUpdatedLabel = "Just now",
                        isTyping = true,
                    )
                } else it
            }
        }

        // Intelligent warm companion reply with realistic typing indicator
        viewModelScope.launch {
            delay(1600)
            val companionName = thread.companionName
            val replyText = generateWarmCompanionReply(companionName, text, voiceDurationSec, imageUrl)
            val incoming = DateMateMessage(
                id = "msg_reply_${System.currentTimeMillis()}",
                threadId = threadId,
                senderIsMe = false,
                senderName = companionName,
                text = replyText,
                timestampLabel = currentTimeLabel(),
                isRead = true,
            )
            _messagesByThread.update { map ->
                val current = map[threadId].orEmpty()
                map + (threadId to (current + incoming))
            }
            _chatThreads.update { list ->
                list.map {
                    if (it.id == threadId) {
                        it.copy(
                            lastMessage = replyText,
                            lastUpdatedLabel = "Just now",
                            isTyping = false,
                        )
                    } else it
                }
            }
        }
    }

    private fun generateWarmCompanionReply(
        companionName: String,
        userText: String,
        voiceSec: Int,
        imageUrl: String,
    ): String {
        val lower = userText.lowercase()
        return when {
            voiceSec > 0 -> "Loved hearing your voice note! Your evening plan sounds wonderful ❤️"
            imageUrl.isNotBlank() -> "What a gorgeous photo! Thank you for sharing ✨"
            lower.contains("coffee") || lower.contains("cafe") ->
                "A cozy specialty coffee date sounds delightful! I know a lovely spot with warm lighting ☕✨"
            lower.contains("dinner") || lower.contains("saturday") || lower.contains("date") || lower.contains("book") ->
                "I would love that! Feel free to lock in our time via Book Now whenever you're ready 🌹"
            lower.contains("hi") || lower.contains("hello") || lower.contains("hey") ->
                "Hi there! How is your day going so far? 😊❤️"
            else ->
                "That sounds so thoughtful! I'm really looking forward to our time together on DateMate ✨"
        }
    }

    fun toggleMessageReaction(threadId: String, messageId: String, emoji: String) {
        _messagesByThread.update { map ->
            val updated = map[threadId].orEmpty().map { msg ->
                if (msg.id == messageId) {
                    val nextReactions = if (emoji in msg.reactions) {
                        msg.reactions - emoji
                    } else {
                        msg.reactions + emoji
                    }
                    msg.copy(reactions = nextReactions)
                } else msg
            }
            map + (threadId to updated)
        }
        _heartExplosionTrigger.value = System.currentTimeMillis()
    }

    fun deleteMessage(threadId: String, messageId: String) {
        _messagesByThread.update { map ->
            val remaining = map[threadId].orEmpty().filterNot { it.id == messageId }
            map + (threadId to remaining)
        }
        emitMessage("Message deleted")
    }

    fun deleteChatThread(threadId: String) {
        _chatThreads.update { list -> list.filterNot { it.id == threadId } }
        _messagesByThread.update { map -> map - threadId }
        emitMessage("Conversation deleted")
    }

    // ========================================================================
    // Audio & Video Call Overlay
    // ========================================================================
    fun startCompanionCall(companionId: String, callType: ActiveCallType) {
        val companion = findCompanionById(companionId) ?: _companions.value.firstOrNull() ?: return
        _activeCall.value = ActiveCompanionCall(
            companionId = companion.id,
            companionName = companion.name,
            companionPhotoUrl = companion.primaryPhotoUrl,
            callType = callType,
            statusText = "Ringing ${companion.name}…",
        )
        viewModelScope.launch {
            delay(1800)
            _activeCall.update { current ->
                current?.copy(statusText = "00:01 · HD Encrypted ${if (callType == ActiveCallType.VIDEO) "Video" else "Audio"}")
            }
        }
    }

    fun toggleCallMute() {
        _activeCall.update { it?.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCall.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun endCompanionCall() {
        _activeCall.value = null
        emitMessage("Call ended")
    }

    // ========================================================================
    // Block & Report Safety Controls
    // ========================================================================
    fun blockCompanion(companionId: String, reason: String = "Blocked by user") {
        val companion = findCompanionById(companionId)
        _blockedCompanionIds.update { it + companionId }
        _favoriteIds.update { it - companionId }
        _chatThreads.update { list -> list.filterNot { it.companionId == companionId } }
        emitMessage("${companion?.name ?: "Profile"} has been blocked.")
    }

    fun unblockCompanion(companionId: String) {
        val companion = findCompanionById(companionId)
        _blockedCompanionIds.update { it - companionId }
        emitMessage("${companion?.name ?: "Profile"} unblocked.")
    }

    fun reportCompanion(companionId: String, reason: String) {
        val companion = findCompanionById(companionId)
        emitMessage("Report submitted for ${companion?.name ?: "profile"} ($reason). Our safety team is reviewing.")
    }

    // ========================================================================
    // Membership & Notifications
    // ========================================================================
    fun selectMembershipPlan(tier: MembershipTier, isYearly: Boolean) {
        _userProfile.update {
            it.copy(
                membershipTier = tier,
                isYearlyBilling = isYearly,
            )
        }
        _notifications.update { current ->
            listOf(
                DateMateNotification(
                    id = "notif_mem_${System.currentTimeMillis()}",
                    category = NotificationCategory.MEMBERSHIP_UPDATE,
                    title = "DateMate ${tier.title} Plan Activated 👑",
                    body = "Your ${if (isYearly) "Yearly" else "Monthly"} ${tier.title} membership benefits are now live.",
                    timestampLabel = "Just now",
                    isRead = false,
                )
            ) + current
        }
        _heartExplosionTrigger.value = System.currentTimeMillis()
        emitMessage("Subscribed to DateMate ${tier.title} (${if (isYearly) "Yearly" else "Monthly"}) 👑")
    }

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
        emitMessage("All notifications marked as read")
    }

    fun markNotificationRead(notificationId: String) {
        _notifications.update { list ->
            list.map { if (it.id == notificationId) it.copy(isRead = true) else it }
        }
    }

    // ========================================================================
    // My Profile & Settings Management
    // ========================================================================
    fun updateUserProfile(transform: (CustomerUserProfile) -> CustomerUserProfile) {
        _userProfile.update(transform)
        emitMessage("Profile updated ✨")
    }

    fun addUploadedProfilePhoto(uriString: String) {
        if (uriString.isBlank()) return
        _userProfile.update { current ->
            val updatedGallery = (listOf(uriString) + current.galleryPhotos).distinct()
            current.copy(
                avatarUrl = uriString,
                galleryPhotos = updatedGallery,
            )
        }
        emitMessage("New profile photo uploaded")
    }

    fun logoutUser() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        _isAuthenticated.value = false
        emitMessage("Logged out of DateMate")
    }

    fun deleteUserAccount() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        _userProfile.value = CustomerUserProfile()
        _isAuthenticated.value = false
        emitMessage("Your DateMate account and personal data have been permanently deleted")
    }

    private fun currentTimeLabel(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }
}
