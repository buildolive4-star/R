package com.example.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.GalleryImage
import com.example.data.model.ModerationStatus
import com.example.data.model.UserAccount
import com.example.data.remote.ImgBbApiService
import com.example.data.remote.ImgBbUploadHelper
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserCrmRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SessionAndChatViewModel(
    private val userCrmRepository: UserCrmRepository,
    private val chatRepository: ChatRepository,
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
) : ViewModel() {

    private val _firebaseUser = MutableStateFlow(Firebase.auth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pendingChatProfile = MutableStateFlow<GalleryImage?>(null)
    val pendingChatProfile: StateFlow<GalleryImage?> = _pendingChatProfile.asStateFlow()

    private val _navigateToChatId = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val navigateToChatId: SharedFlow<String> = _navigateToChatId.asSharedFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _firebaseUser.value = auth.currentUser
    }

    init {
        Firebase.auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        Firebase.auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    val userAccount: StateFlow<UserAccount?> = _firebaseUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else userCrmRepository.observeCurrentUser(user.uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    val isApprovedStaffTheme: StateFlow<Boolean> = userAccount
        .map { it?.isApprovedStaff == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = false,
        )

    val clientChats: StateFlow<List<ChatThread>> = _firebaseUser
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else chatRepository.observeClientChats(user.uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val staffInboxChats: StateFlow<List<ChatThread>> = combineStaffFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    private fun combineStaffFlow() = userAccount.flatMapLatest { account ->
        if (account != null && account.isApprovedStaff) {
            chatRepository.observeStaffInboxChats(account.uid)
        } else {
            flowOf(emptyList())
        }
    }

    fun observeChatThread(chatId: String) = chatRepository.observeChatThread(chatId)

    fun observeMessages(chatId: String) = chatRepository.observeMessages(chatId)

    fun setOnlineState(online: Boolean) {
        val uid = _firebaseUser.value?.uid ?: return
        viewModelScope.launch {
            userCrmRepository.setOnlinePresence(uid, online)
        }
    }

    fun signInWithGoogle(activityContext: Context, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            _isBusy.value = true
            val res = GoogleAuthHelper.signInWithGoogle(activityContext, autoSelectSilentOnly = false)
            _isBusy.value = false
            res.onSuccess { user ->
                _firebaseUser.value = user
                _authErrorMessage.value = null
                userCrmRepository.setOnlinePresence(user.uid, true)
                onSuccess?.invoke()
            }.onFailure { err ->
                val message = err.localizedMessage ?: "Google sign-in failed. Please try again."
                _authErrorMessage.value = message
                _events.tryEmit(message)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            val uid = _firebaseUser.value?.uid
            if (uid != null) userCrmRepository.setOnlinePresence(uid, false)
            GoogleAuthHelper.signOut(context)
            _firebaseUser.value = null
            _events.tryEmit("Signed out")
        }
    }

    fun requestChatWithProfile(profile: GalleryImage) {
        val currentUser = _firebaseUser.value
        val account = userAccount.value
        if (account?.moderationStatus == ModerationStatus.BLOCKED) {
            _events.tryEmit("Your account is blocked by Admin.")
            return
        }
        if (account?.moderationStatus == ModerationStatus.SUSPENDED) {
            _events.tryEmit("Your account is suspended by Admin.")
            return
        }

        if (currentUser != null && account != null && account.hasInquiryReadyDetails) {
            openChatDirectly(account, profile)
        } else {
            _authErrorMessage.value = null
            _pendingChatProfile.value = profile
        }
    }

    fun dismissChatOnboarding() {
        _authErrorMessage.value = null
        _pendingChatProfile.value = null
    }

    fun completeOnboardingAndOpenChat(
        activityContext: Context,
        name: String,
        phone: String,
        ageText: String,
        address: String,
    ) {
        val profile = _pendingChatProfile.value ?: return
        _authErrorMessage.value = null
        val age = ageText.trim().toIntOrNull() ?: 0
        if (name.trim().length < 2) {
            val msg = "Please enter your full name (minimum 2 characters)."
            _authErrorMessage.value = msg
            _events.tryEmit(msg)
            return
        }
        if (phone.count { it.isDigit() } < 8) {
            val msg = "Please enter a valid mobile number (at least 8 digits)."
            _authErrorMessage.value = msg
            _events.tryEmit(msg)
            return
        }
        if (age !in 18..99) {
            val msg = "Please enter a valid age (18 to 99)."
            _authErrorMessage.value = msg
            _events.tryEmit(msg)
            return
        }
        if (address.trim().length < 4) {
            val msg = "Please enter your full city / address."
            _authErrorMessage.value = msg
            _events.tryEmit(msg)
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            var user = _firebaseUser.value
            if (user == null) {
                val signInResult = GoogleAuthHelper.signInWithGoogle(activityContext)
                if (signInResult.isFailure) {
                    _isBusy.value = false
                    val message = signInResult.exceptionOrNull()?.localizedMessage
                        ?: "Google sign-in is required to open private chat"
                    _authErrorMessage.value = message
                    _events.tryEmit(message)
                    return@launch
                }
                user = signInResult.getOrNull()
                _firebaseUser.value = user
                _authErrorMessage.value = null
            }

            val existing = userAccount.value
            val saveRes = userCrmRepository.upsertOwnProfile(
                name = name,
                photoUrl = existing?.photoUrl ?: (user?.photoUrl?.toString() ?: ""),
                galleryUrls = existing?.galleryUrls.orEmpty(),
                phone = phone,
                age = age,
                address = address,
                city = existing?.city.orEmpty(),
                bio = existing?.bio.orEmpty(),
                interests = existing?.interests.orEmpty(),
                genderPreference = existing?.genderPreference.orEmpty(),
            )
            saveRes.onSuccess { savedAccount ->
                val chatRes = chatRepository.openOrCreateChat(savedAccount, profile)
                _isBusy.value = false
                chatRes.onSuccess { thread ->
                    _authErrorMessage.value = null
                    _pendingChatProfile.value = null
                    _navigateToChatId.tryEmit(thread.id)
                }.onFailure { err ->
                    val msg = "Chat Error: ${err.localizedMessage ?: "Unable to start chat"}"
                    _authErrorMessage.value = msg
                    _events.tryEmit(msg)
                }
            }.onFailure { err ->
                _isBusy.value = false
                val msg = "Profile Save Error: ${err.localizedMessage ?: "Unable to save inquiry profile"}"
                _authErrorMessage.value = msg
                _events.tryEmit(msg)
            }
        }
    }

    private fun openChatDirectly(account: UserAccount, profile: GalleryImage) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = chatRepository.openOrCreateChat(account, profile)
            _isBusy.value = false
            res.onSuccess { thread ->
                _navigateToChatId.tryEmit(thread.id)
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to open chat")
            }
        }
    }

    fun saveFullProfile(
        context: Context,
        name: String,
        selectedAvatarUri: Uri?,
        selectedGalleryUris: List<Uri>,
        phone: String,
        ageText: String,
        address: String,
        city: String,
        bio: String,
        interests: String,
        genderPreference: String,
    ) {
        if (_firebaseUser.value == null) {
            _events.tryEmit("Please sign in with Google first")
            return
        }
        val age = ageText.trim().toIntOrNull() ?: 0
        viewModelScope.launch {
            _isBusy.value = true
            val existing = userAccount.value
            var avatarUrl = existing?.photoUrl.orEmpty()
            if (selectedAvatarUri != null) {
                avatarUrl = uploadUriSmart(context, selectedAvatarUri, "${name}_avatar")
            } else if (avatarUrl.isBlank()) {
                avatarUrl = _firebaseUser.value?.photoUrl?.toString().orEmpty()
            }

            val existingGallery = existing?.galleryUrls.orEmpty().toMutableList()
            for (uri in selectedGalleryUris.take(6)) {
                val uploaded = uploadUriSmart(context, uri, "${name}_photo")
                if (uploaded.isNotBlank() && !existingGallery.contains(uploaded)) {
                    existingGallery.add(0, uploaded)
                }
            }

            val res = userCrmRepository.upsertOwnProfile(
                name = name,
                photoUrl = avatarUrl,
                galleryUrls = existingGallery.take(6),
                phone = phone,
                age = age,
                address = address,
                city = city,
                bio = bio,
                interests = interests,
                genderPreference = genderPreference,
            )
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Profile saved (${it.completionPercent}% complete)")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun applyForStaff(staffRole: String, staffPrice: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.submitStaffApplication(staffRole, staffPrice)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Application submitted! Status: Under Review")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Complete 100% of profile first")
            }
        }
    }

    fun sendChatMessage(
        chat: ChatThread,
        senderRole: String,
        text: String,
        onBlockedOrError: (String) -> Unit,
        onSent: () -> Unit,
    ) {
        val account = userAccount.value
        val authUser = _firebaseUser.value
        val senderName = when (senderRole) {
            "admin" -> "JustUs Concierge"
            "staff" -> account?.name?.ifBlank { authUser?.displayName ?: "Companion" } ?: "Companion"
            else -> account?.name?.ifBlank { authUser?.displayName ?: "You" } ?: "You"
        }
        viewModelScope.launch {
            val res = chatRepository.sendMessage(
                chat = chat,
                senderRole = senderRole,
                senderName = senderName,
                rawText = text,
            )
            res.onSuccess {
                onSent()
            }.onFailure { err ->
                onBlockedOrError(err.localizedMessage ?: "Message could not be sent")
            }
        }
    }

    fun findChatById(chatId: String): ChatThread? {
        return clientChats.value.firstOrNull { it.id == chatId }
            ?: staffInboxChats.value.firstOrNull { it.id == chatId }
    }

    private suspend fun uploadUriSmart(context: Context, uri: Uri, title: String): String {
        val res = if (ImgBbApiService.isApiKeyConfigured()) {
            ImgBbUploadHelper.uploadUriToImgBb(context, imgBbApiService, uri, title, "Profile")
        } else {
            ImgBbUploadHelper.saveUriLocallyForDeviceFeed(context, uri, title, "Profile")
        }
        return res.getOrNull()?.imageUrl.orEmpty()
    }
}
