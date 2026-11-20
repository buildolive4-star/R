package com.example.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.ActiveCallUi
import com.example.data.model.AppShareConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.GalleryImage
import com.example.data.model.LaunchBannerConfig
import com.example.data.model.ModerationStatus
import com.example.data.model.PresenceStatus
import com.example.data.model.PromoTickerConfig
import com.example.data.model.UserAccount
import com.example.data.remote.ImgBbApiService
import com.example.data.remote.ImgBbUploadHelper
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserCrmRepository
import com.example.notifications.OneSignalPushManager
import com.example.notifications.PixoraNotificationManager
import com.example.util.CallRingtonePlayer
import com.example.util.LocationAddressFetcher
import com.example.util.WebRtcAudioCallClient
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.webrtc.EglBase
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SessionAndChatViewModel(
    private val userCrmRepository: UserCrmRepository,
    private val chatRepository: ChatRepository,
    private val callRepository: CallRepository? = null,
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
    appContext: Context? = null,
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

    private val _isVerifiedAdminForCalls = MutableStateFlow(false)
    val isVerifiedAdminForCalls: StateFlow<Boolean> = _isVerifiedAdminForCalls.asStateFlow()

    private val _activeCallUi = MutableStateFlow<ActiveCallUi>(ActiveCallUi.Idle)
    val activeCallUi: StateFlow<ActiveCallUi> = _activeCallUi.asStateFlow()

    private val _incomingCallAlert = MutableSharedFlow<CallSession>(extraBufferCapacity = 4)
    val incomingCallAlert: SharedFlow<CallSession> = _incomingCallAlert.asSharedFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private val ringtonePlayer: CallRingtonePlayer? = appContext?.applicationContext?.let {
        CallRingtonePlayer(it)
    }

    private var webRtcClient: WebRtcAudioCallClient? = null
    private var localVideoCollectJob: Job? = null
    private var remoteVideoCollectJob: Job? = null
    private var activeCallSessionJob: Job? = null
    private var activeIceJob: Job? = null
    private var ringTimeoutJob: Job? = null
    private val addedRemoteIceKeys = mutableSetOf<String>()
    private val handledIncomingCallIds = mutableSetOf<String>()
    private var appliedRemoteAnswerForCallId: String? = null
    private var isMutedState = false
    private var isSpeakerState = false
    private var isCameraOffState = false
    private var isFrontCameraState = true

    fun getActiveCallEglContext(): EglBase.Context? = webRtcClient?.eglBaseContext

    fun clearAuthError() {
        _authErrorMessage.value = null
    }

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _firebaseUser.value = auth.currentUser
        refreshAdminCallMonitoring()
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

    val adminChats: StateFlow<List<ChatThread>> = _isVerifiedAdminForCalls
        .flatMapLatest { isAdmin ->
            if (isAdmin) chatRepository.observeAllChatsForAdmin() else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    val launchBanner: StateFlow<LaunchBannerConfig?> = userCrmRepository.observeLaunchBanner()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    val appShareConfig: StateFlow<AppShareConfig> = userCrmRepository.observeAppShareConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppShareConfig(),
        )

    val appUpdateConfig: StateFlow<AppUpdateConfig> = userCrmRepository.observeAppUpdateConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppUpdateConfig(),
        )

    val promoTickerConfig: StateFlow<PromoTickerConfig> = userCrmRepository.observePromoTickerConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PromoTickerConfig(),
        )

    init {
        Firebase.auth.addAuthStateListener(authListener)
        refreshAdminCallMonitoring()
        observeIncomingCallStreams()
        viewModelScope.launch {
            _activeCallUi.collect { ui ->
                ringTimeoutJob?.cancel()
                when (ui) {
                    is ActiveCallUi.IncomingRinging -> {
                        PixoraNotificationManager.stopBackgroundCallRinging()
                        ringtonePlayer?.startIncomingRingtone()
                        ringTimeoutJob = viewModelScope.launch {
                            delay(45_000L)
                            if (_activeCallUi.value is ActiveCallUi.IncomingRinging) {
                                teardownCallResources()
                                _activeCallUi.value = ActiveCallUi.Idle
                            }
                        }
                    }
                    is ActiveCallUi.OutgoingRinging -> {
                        ringtonePlayer?.startOutgoingRingback()
                        ringTimeoutJob = viewModelScope.launch {
                            delay(45_000L)
                            val cur = _activeCallUi.value
                            if (cur is ActiveCallUi.OutgoingRinging) {
                                callRepository?.updateCallStatus(cur.session.id, CallStatus.MISSED)
                                teardownCallResources()
                                _activeCallUi.value = ActiveCallUi.Idle
                                _events.tryEmit("No answer")
                            }
                        }
                    }
                    else -> {
                        PixoraNotificationManager.stopBackgroundCallRinging()
                        ringtonePlayer?.stopAll()
                    }
                }
            }
        }
        viewModelScope.launch {
            combine(_firebaseUser, _isVerifiedAdminForCalls, isApprovedStaffTheme) { user, isAdmin, isStaff ->
                Triple(user?.uid, isAdmin, isStaff)
            }.collect { (uid, isAdmin, isStaff) ->
                OneSignalPushManager.syncUserIdentity(
                    uid = uid,
                    isAdmin = isAdmin,
                    isApprovedStaff = isStaff,
                )
                if (!uid.isNullOrBlank()) {
                    val effectiveOnline = if (isStaff && userAccount.value?.staffOnline == false) false else true
                    userCrmRepository.setOnlinePresence(uid, online = effectiveOnline, updateAdminSentinel = isAdmin)
                }
            }
        }
    }

    override fun onCleared() {
        Firebase.auth.removeAuthStateListener(authListener)
        teardownCallResources()
        super.onCleared()
    }

    fun refreshAdminCallMonitoring() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            _isVerifiedAdminForCalls.value = false
            return
        }
        viewModelScope.launch {
            _isVerifiedAdminForCalls.value = userCrmRepository.checkIsCurrentUserAdmin()
        }
    }

    fun unlockPinAdminAndRefreshMonitoring() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            _isVerifiedAdminForCalls.value = false
            return
        }
        viewModelScope.launch {
            userCrmRepository.ensurePinAdminUnlocked()
            _isVerifiedAdminForCalls.value = userCrmRepository.checkIsCurrentUserAdmin()
        }
    }

    private fun observeIncomingCallStreams() {
        val repo = callRepository ?: return
        val userCallsFlow = _firebaseUser.flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else repo.observeIncomingCallsForUser(user.uid)
        }
        val adminCallsFlow = _isVerifiedAdminForCalls.flatMapLatest { isAdmin ->
            if (isAdmin) repo.observeIncomingCallsForAdmin() else flowOf(emptyList())
        }

        viewModelScope.launch {
            combine(userCallsFlow, adminCallsFlow) { directCalls, adminCalls ->
                (directCalls + adminCalls).distinctBy { it.id }
            }.collect { calls ->
                val myUid = _firebaseUser.value?.uid ?: return@collect
                val now = System.currentTimeMillis()
                val nextCall = calls.firstOrNull { call ->
                    val createdMs = call.createdAt?.toDate()?.time ?: now
                    val isFresh = (now - createdMs) <= 120_000L
                    call.callerUid != myUid &&
                        !handledIncomingCallIds.contains(call.id) &&
                        isFresh
                }
                if (nextCall != null && _activeCallUi.value is ActiveCallUi.Idle) {
                    handledIncomingCallIds.add(nextCall.id)
                    _activeCallUi.value = ActiveCallUi.IncomingRinging(nextCall)
                    _incomingCallAlert.tryEmit(nextCall)
                    watchCallStatusWhileIncoming(nextCall.id)
                }
            }
        }
    }

    private fun watchCallStatusWhileIncoming(callId: String) {
        val repo = callRepository ?: return
        activeCallSessionJob?.cancel()
        activeCallSessionJob = viewModelScope.launch {
            repo.observeCallSession(callId).collect { updated ->
                val current = _activeCallUi.value
                if (current is ActiveCallUi.IncomingRinging && current.session.id == callId) {
                    if (updated == null || updated.status != CallStatus.RINGING) {
                        _activeCallUi.value = ActiveCallUi.Idle
                        activeCallSessionJob?.cancel()
                    }
                }
            }
        }
    }

    private fun combineStaffFlow() = userAccount.flatMapLatest { account ->
        if (account != null && account.canDirectChatAsStaff) {
            chatRepository.observeStaffInboxChats(account.uid)
        } else {
            flowOf(emptyList())
        }
    }

    fun observeChatThread(chatId: String) = chatRepository.observeChatThread(chatId)

    fun observeMessages(chatId: String) = chatRepository.observeMessages(chatId)

    fun observePeerPresence(chat: ChatThread?, senderRole: String): Flow<PresenceStatus?> {
        if (chat == null) return flowOf(null)
        val peerId = if (senderRole == "client") {
            chat.staffOwnerUid.ifBlank { UserCrmRepository.ADMIN_PRESENCE_ID }
        } else {
            chat.clientUid
        }
        return userCrmRepository.observePresence(peerId)
    }

    fun observePresenceMap(presenceIds: List<String>): Flow<Map<String, PresenceStatus>> =
        userCrmRepository.observePresencesMap(presenceIds)

    fun markChatRead(chatId: String, readerRole: String) {
        viewModelScope.launch {
            chatRepository.markChatThreadRead(chatId, readerRole)
        }
    }

    fun markChatsDeliveredIfNeeded(chats: List<ChatThread>, receiverRole: String) {
        if (chats.isEmpty() || _firebaseUser.value == null) return
        viewModelScope.launch {
            chats.forEach { thread ->
                val updatedMs = thread.updatedAt?.toDate()?.time ?: 0L
                val needsDeliveredStamp = if (receiverRole == "client") {
                    thread.lastSenderRole != "client" && updatedMs > thread.clientLastDeliveredEpochMs
                } else {
                    thread.lastSenderRole == "client" && updatedMs > thread.agentLastDeliveredEpochMs
                }
                if (needsDeliveredStamp) {
                    chatRepository.markChatDelivered(thread.id, receiverRole)
                }
            }
        }
    }

    fun deleteChatThread(chatId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            chatRepository.deleteChatThread(chatId)
                .onSuccess {
                    _events.tryEmit("Chat deleted")
                    onSuccess()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not delete chat")
                }
        }
    }

    fun deleteChatMessages(
        chat: ChatThread,
        messageIds: List<String>,
        remainingMessages: List<ChatMessage>,
        onSuccess: () -> Unit = {},
    ) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            chatRepository.deleteMessages(chat, messageIds, remainingMessages)
                .onSuccess {
                    _events.tryEmit(
                        if (messageIds.size == 1) "Message deleted"
                        else "${messageIds.size} messages deleted"
                    )
                    onSuccess()
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Could not delete messages")
                }
        }
    }

    fun setOnlineState(online: Boolean) {
        val uid = _firebaseUser.value?.uid ?: return
        val isAdmin = _isVerifiedAdminForCalls.value
        val account = userAccount.value
        val effectiveOnline = if (account?.isApprovedStaff == true && !account.staffOnline) false else online
        viewModelScope.launch {
            userCrmRepository.setOnlinePresence(uid, effectiveOnline, updateAdminSentinel = isAdmin)
        }
    }

    fun toggleStaffOnlineStatus(online: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.setStaffOnlineAvailability(online)
            _isBusy.value = false
            res.onSuccess { nowOnline ->
                _events.tryEmit(
                    if (nowOnline) {
                        "You are now ONLINE 🟢 — Users can see you live!"
                    } else {
                        "You are now OFFLINE ⚫ — Hidden from Online Now filter"
                    }
                )
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to update Online/Offline status")
            }
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
                refreshAdminCallMonitoring()
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
            val wasAdmin = _isVerifiedAdminForCalls.value
            if (uid != null) userCrmRepository.setOnlinePresence(uid, false, updateAdminSentinel = wasAdmin)
            OneSignalPushManager.clearIdentity()
            GoogleAuthHelper.signOut(context)
            _firebaseUser.value = null
            _isVerifiedAdminForCalls.value = false
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
                refreshAdminCallMonitoring()
            }

            val existing = userAccount.value
            val derivedCity = existing?.city?.takeIf { it.isNotBlank() }
                ?: LocationAddressFetcher.extractPublicCityFromAddress(address)
            val saveRes = userCrmRepository.upsertOwnProfile(
                name = name,
                photoUrl = existing?.photoUrl ?: (user?.photoUrl?.toString() ?: ""),
                galleryUrls = existing?.galleryUrls.orEmpty(),
                phone = phone,
                age = age,
                address = address,
                city = derivedCity,
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

    // =========================================================================
    // WebRTC 1-on-1 Voice & Private HD Video Calling (Admin <-> User & Staff)
    // =========================================================================

    fun startVoiceCallFromChat(
        context: Context,
        chat: ChatThread,
        callerRole: String,
        isVideoCall: Boolean = false,
        ratePerMinInr: Int = 0,
        profileVideoCallEnabled: Boolean = true,
    ) {
        val myUid = _firebaseUser.value?.uid
        if (myUid.isNullOrBlank()) {
            _events.tryEmit("Please sign in with Google to place a call.")
            return
        }
        val repo = callRepository ?: return
        val account = userAccount.value
        val isAdminCaller = callerRole == "admin" || _isVerifiedAdminForCalls.value

        if (isVideoCall && !isAdminCaller) {
            if (account?.videoCallEnabled == false) {
                _events.tryEmit("Video Call is turned OFF for your account by Admin.")
                return
            }
            if (!profileVideoCallEnabled) {
                _events.tryEmit("Video Call is turned OFF for this profile by Admin.")
                return
            }
        }

        val isFromAdminOrStaff = callerRole == "admin" || callerRole == "staff"
        val calleeUid = if (isFromAdminOrStaff) {
            chat.clientUid
        } else {
            chat.staffOwnerUid.ifBlank { CallStatus.ADMIN_CALLEE_UID }
        }
        val calleeName = if (isFromAdminOrStaff) {
            chat.clientName.ifBlank { "Client" }
        } else {
            chat.profileName.ifBlank { "DateMate Concierge" }
        }
        val callerName = when (callerRole) {
            "admin" -> "DateMate Concierge"
            "staff" -> account?.name?.ifBlank { chat.profileName } ?: chat.profileName
            else -> account?.name?.ifBlank { _firebaseUser.value?.displayName ?: "Client" } ?: "Client"
        }
        val resolvedCallerRole = when {
            callerRole == "admin" -> "admin"
            callerRole == "staff" && account?.isApprovedStaff == true -> "staff"
            else -> "client"
        }

        launchOutgoingCall(
            context = context,
            repo = repo,
            callerName = callerName,
            callerRole = resolvedCallerRole,
            calleeUid = calleeUid,
            calleeName = calleeName,
            chatId = chat.id,
            profileId = chat.profileId,
            profileName = chat.profileName,
            isVideoCall = isVideoCall,
            ratePerMinInr = ratePerMinInr,
        )
    }

    fun startCallFromProfile(
        context: Context,
        profile: GalleryImage,
        isVideoCall: Boolean,
    ) {
        val myUid = _firebaseUser.value?.uid
        if (myUid.isNullOrBlank()) {
            _events.tryEmit("Please sign in with Google first to call ${profile.title}.")
            _pendingChatProfile.value = profile
            return
        }
        val account = userAccount.value
        val isAdminCaller = _isVerifiedAdminForCalls.value
        if (isVideoCall && !isAdminCaller) {
            if (account?.videoCallEnabled == false) {
                _events.tryEmit("Video Call is turned OFF for your account by Admin.")
                return
            }
            if (!profile.videoCallEnabled) {
                _events.tryEmit("Video Call is turned OFF for ${profile.title} by Admin.")
                return
            }
        }
        val repo = callRepository ?: return
        val callerName = account?.name?.ifBlank { _firebaseUser.value?.displayName ?: "Client" } ?: "Client"
        val calleeUid = profile.ownerUid.ifBlank { CallStatus.ADMIN_CALLEE_UID }
        val rateInr = if (isVideoCall) profile.videoCallPricePerMin else profile.voiceCallPricePerMin
        val derivedChatId = "${myUid}_${profile.id}"

        launchOutgoingCall(
            context = context,
            repo = repo,
            callerName = callerName,
            callerRole = if (isAdminCaller) "admin" else "client",
            calleeUid = calleeUid,
            calleeName = profile.title.ifBlank { "Companion" },
            chatId = derivedChatId,
            profileId = profile.id,
            profileName = profile.title,
            isVideoCall = isVideoCall,
            ratePerMinInr = rateInr,
        )
    }

    fun startDirectAdminVoiceCallToUser(
        context: Context,
        targetUser: UserAccount,
        relatedChat: ChatThread? = null,
        isVideoCall: Boolean = false,
    ) {
        val repo = callRepository ?: return
        if (_firebaseUser.value == null) {
            _events.tryEmit("Admin Google Sign-In required for calling.")
            return
        }
        val adminName = _firebaseUser.value?.displayName?.takeIf { it.isNotBlank() } ?: "DateMate Admin"
        launchOutgoingCall(
            context = context,
            repo = repo,
            callerName = adminName,
            callerRole = "admin",
            calleeUid = targetUser.uid,
            calleeName = targetUser.name.ifBlank { "Member" },
            chatId = relatedChat?.id.orEmpty(),
            profileId = relatedChat?.profileId.orEmpty(),
            profileName = relatedChat?.profileName ?: "Concierge Call",
            isVideoCall = isVideoCall,
            ratePerMinInr = 0,
        )
    }

    private fun attachVideoTrackObservers(client: WebRtcAudioCallClient) {
        localVideoCollectJob?.cancel()
        remoteVideoCollectJob?.cancel()
        localVideoCollectJob = viewModelScope.launch {
            client.localVideoTrackFlow.collect { track ->
                _localVideoTrack.value = track
            }
        }
        remoteVideoCollectJob = viewModelScope.launch {
            client.remoteVideoTrackFlow.collect { track ->
                _remoteVideoTrack.value = track
            }
        }
    }

    private fun launchOutgoingCall(
        context: Context,
        repo: CallRepository,
        callerName: String,
        callerRole: String,
        calleeUid: String,
        calleeName: String,
        chatId: String,
        profileId: String,
        profileName: String,
        isVideoCall: Boolean = false,
        ratePerMinInr: Int = 0,
    ) {
        if (_activeCallUi.value !is ActiveCallUi.Idle) {
            _events.tryEmit("Another call is already active.")
            return
        }
        teardownCallResources()
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        handledIncomingCallIds.add(callId)
        appliedRemoteAnswerForCallId = null
        isMutedState = false
        isSpeakerState = isVideoCall
        isCameraOffState = false
        isFrontCameraState = true

        viewModelScope.launch {
            val client = WebRtcAudioCallClient(
                context = context,
                isVideoCall = isVideoCall,
                onLocalIceCandidate = { candidate ->
                    viewModelScope.launch {
                        repo.addIceCandidate(
                            callId = callId,
                            isOfferSide = true,
                            sdpMid = candidate.sdpMid ?: "audio",
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            candidate = candidate.sdp,
                        )
                    }
                },
                onConnectionStateChanged = { state ->
                    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                        val current = _activeCallUi.value
                        val sess = when (current) {
                            is ActiveCallUi.Connecting -> current.session
                            is ActiveCallUi.OutgoingRinging -> current.session
                            is ActiveCallUi.Connected -> current.session
                            else -> null
                        }
                        if (sess != null && current !is ActiveCallUi.Connected) {
                            _activeCallUi.value = ActiveCallUi.Connected(
                                session = sess,
                                connectedAtEpochMs = System.currentTimeMillis(),
                                isMuted = isMutedState,
                                isSpeakerOn = isSpeakerState,
                                isCameraOff = isCameraOffState,
                                isFrontCamera = isFrontCameraState,
                            )
                        }
                    } else if (
                        state == PeerConnection.PeerConnectionState.FAILED ||
                        state == PeerConnection.PeerConnectionState.DISCONNECTED
                    ) {
                        hangUpCall(context)
                    }
                }
            )
            webRtcClient = client
            attachVideoTrackObservers(client)

            val offerResult = runCatching { client.createOfferSdp() }
            val offerSdp = offerResult.getOrElse { err ->
                teardownCallResources()
                _events.tryEmit(
                    if (isVideoCall) "Unable to start camera/mic: ${err.localizedMessage ?: "Permission error"}"
                    else "Unable to start microphone: ${err.localizedMessage ?: "Permission or audio error"}"
                )
                return@launch
            }

            val createdRes = repo.createCallSession(
                callId = callId,
                callerName = callerName,
                callerRole = callerRole,
                calleeUid = calleeUid,
                calleeName = calleeName,
                chatId = chatId,
                profileId = profileId,
                profileName = profileName,
                offerSdp = offerSdp,
                callType = if (isVideoCall) CallType.VIDEO else CallType.AUDIO,
                ratePerMinInr = ratePerMinInr,
            )
            val session = createdRes.getOrElse { err ->
                teardownCallResources()
                val msg = err.localizedMessage.orEmpty()
                if (isVideoCall && msg.contains("PERMISSION_DENIED", ignoreCase = true)) {
                    _events.tryEmit("Video Call is restricted by Admin for this user or companion.")
                } else {
                    _events.tryEmit("Call failed: ${msg.ifBlank { "Permission denied" }}")
                }
                return@launch
            }

            _activeCallUi.value = ActiveCallUi.OutgoingRinging(session)
            OneSignalPushManager.sendCallPushAsync(
                callId = callId,
                callerName = callerName,
                callerRole = callerRole,
                calleeUid = calleeUid,
                chatId = chatId,
                profileName = profileName,
                isVideoCall = isVideoCall,
            )

            activeCallSessionJob = viewModelScope.launch {
                repo.observeCallSession(callId).collect { updated ->
                    if (updated == null) return@collect
                    when (updated.status) {
                        CallStatus.ACCEPTED -> {
                            if (updated.answerSdp.isNotBlank() && appliedRemoteAnswerForCallId != callId) {
                                appliedRemoteAnswerForCallId = callId
                                _activeCallUi.value = ActiveCallUi.Connecting(updated, isCaller = true)
                                runCatching { client.applyRemoteAnswerSdp(updated.answerSdp) }
                                    .onSuccess {
                                        if (_activeCallUi.value is ActiveCallUi.Connecting) {
                                            _activeCallUi.value = ActiveCallUi.Connected(
                                                session = updated,
                                                connectedAtEpochMs = System.currentTimeMillis(),
                                                isMuted = isMutedState,
                                                isSpeakerOn = isSpeakerState,
                                                isCameraOff = isCameraOffState,
                                                isFrontCamera = isFrontCameraState,
                                            )
                                        }
                                    }
                                    .onFailure {
                                        hangUpCall(context)
                                    }
                            }
                        }
                        CallStatus.DECLINED, CallStatus.ENDED, CallStatus.MISSED -> {
                            val endMsg = if (updated.status == CallStatus.DECLINED) "Call declined" else "Call ended"
                            teardownCallResources()
                            _activeCallUi.value = ActiveCallUi.Idle
                            _events.tryEmit(endMsg)
                        }
                    }
                }
            }

            activeIceJob = viewModelScope.launch {
                repo.observeIceCandidates(callId, fromOfferSide = false).collect { list ->
                    list.forEach { rec ->
                        val iceKey = rec.id.ifBlank { "${rec.sdpMid}_${rec.sdpMLineIndex}_${rec.candidate}" }
                        if (addedRemoteIceKeys.add(iceKey) && rec.candidate.isNotBlank()) {
                            client.addRemoteIceCandidate(rec.sdpMid, rec.sdpMLineIndex, rec.candidate)
                        }
                    }
                }
            }
        }
    }

    fun acceptIncomingCall(context: Context, session: CallSession) {
        val repo = callRepository ?: return
        PixoraNotificationManager.cancelCallNotification(context, session.id)
        teardownCallResources()
        isMutedState = false
        isSpeakerState = session.isVideoCall
        isCameraOffState = false
        isFrontCameraState = true
        _activeCallUi.value = ActiveCallUi.Connecting(session, isCaller = false)

        viewModelScope.launch {
            val client = WebRtcAudioCallClient(
                context = context,
                isVideoCall = session.isVideoCall,
                onLocalIceCandidate = { candidate ->
                    viewModelScope.launch {
                        repo.addIceCandidate(
                            callId = session.id,
                            isOfferSide = false,
                            sdpMid = candidate.sdpMid ?: "audio",
                            sdpMLineIndex = candidate.sdpMLineIndex,
                            candidate = candidate.sdp,
                        )
                    }
                },
                onConnectionStateChanged = { state ->
                    if (state == PeerConnection.PeerConnectionState.CONNECTED) {
                        val current = _activeCallUi.value
                        if (current is ActiveCallUi.Connecting) {
                            _activeCallUi.value = ActiveCallUi.Connected(
                                session = current.session,
                                connectedAtEpochMs = System.currentTimeMillis(),
                                isMuted = isMutedState,
                                isSpeakerOn = isSpeakerState,
                                isCameraOff = isCameraOffState,
                                isFrontCamera = isFrontCameraState,
                            )
                        }
                    } else if (state == PeerConnection.PeerConnectionState.FAILED) {
                        hangUpCall(context)
                    }
                }
            )
            webRtcClient = client
            attachVideoTrackObservers(client)

            val answerRes = runCatching {
                client.acceptOfferAndCreateAnswerSdp(session.offerSdp)
            }
            val answerSdp = answerRes.getOrElse { err ->
                declineIncomingCall(context, session)
                _events.tryEmit("Call error: ${err.localizedMessage ?: "Unable to connect"}")
                return@launch
            }

            val acceptRes = repo.acceptCallSession(session.id, answerSdp)
            if (acceptRes.isFailure) {
                teardownCallResources()
                _activeCallUi.value = ActiveCallUi.Idle
                _events.tryEmit("Unable to accept call")
                return@launch
            }

            _activeCallUi.value = ActiveCallUi.Connected(
                session = session.copy(status = CallStatus.ACCEPTED, answerSdp = answerSdp),
                connectedAtEpochMs = System.currentTimeMillis(),
                isMuted = isMutedState,
                isSpeakerOn = isSpeakerState,
                isCameraOff = isCameraOffState,
                isFrontCamera = isFrontCameraState,
            )

            activeCallSessionJob = viewModelScope.launch {
                repo.observeCallSession(session.id).collect { updated ->
                    if (updated != null && (updated.status == CallStatus.ENDED || updated.status == CallStatus.MISSED)) {
                        teardownCallResources()
                        _activeCallUi.value = ActiveCallUi.Idle
                        _events.tryEmit("Call ended")
                    }
                }
            }

            activeIceJob = viewModelScope.launch {
                repo.observeIceCandidates(session.id, fromOfferSide = true).collect { list ->
                    list.forEach { rec ->
                        val iceKey = rec.id.ifBlank { "${rec.sdpMid}_${rec.sdpMLineIndex}_${rec.candidate}" }
                        if (addedRemoteIceKeys.add(iceKey) && rec.candidate.isNotBlank()) {
                            client.addRemoteIceCandidate(rec.sdpMid, rec.sdpMLineIndex, rec.candidate)
                        }
                    }
                }
            }
        }
    }

    fun declineIncomingCall(context: Context, session: CallSession) {
        PixoraNotificationManager.cancelCallNotification(context, session.id)
        viewModelScope.launch {
            callRepository?.updateCallStatus(session.id, CallStatus.DECLINED)
            teardownCallResources()
            _activeCallUi.value = ActiveCallUi.Idle
        }
    }

    fun hangUpCall(context: Context) {
        val currentSession = when (val ui = _activeCallUi.value) {
            is ActiveCallUi.OutgoingRinging -> ui.session
            is ActiveCallUi.IncomingRinging -> ui.session
            is ActiveCallUi.Connecting -> ui.session
            is ActiveCallUi.Connected -> ui.session
            is ActiveCallUi.Idle -> null
        }
        if (currentSession != null) {
            PixoraNotificationManager.cancelCallNotification(context, currentSession.id)
            viewModelScope.launch {
                callRepository?.updateCallStatus(currentSession.id, CallStatus.ENDED)
            }
        }
        teardownCallResources()
        _activeCallUi.value = ActiveCallUi.Idle
    }

    fun toggleCallMute() {
        isMutedState = !isMutedState
        webRtcClient?.setMuted(isMutedState)
        val current = _activeCallUi.value
        if (current is ActiveCallUi.Connected) {
            _activeCallUi.value = current.copy(isMuted = isMutedState)
        }
    }

    fun toggleCallSpeaker() {
        isSpeakerState = !isSpeakerState
        webRtcClient?.setSpeakerphone(isSpeakerState)
        val current = _activeCallUi.value
        if (current is ActiveCallUi.Connected) {
            _activeCallUi.value = current.copy(isSpeakerOn = isSpeakerState)
        }
    }

    fun toggleCallCamera() {
        isCameraOffState = !isCameraOffState
        webRtcClient?.setCameraEnabled(!isCameraOffState)
        val current = _activeCallUi.value
        if (current is ActiveCallUi.Connected) {
            _activeCallUi.value = current.copy(isCameraOff = isCameraOffState)
        }
    }

    fun switchCallCamera() {
        webRtcClient?.switchCamera { isFront ->
            isFrontCameraState = isFront
            val current = _activeCallUi.value
            if (current is ActiveCallUi.Connected) {
                _activeCallUi.value = current.copy(isFrontCamera = isFront)
            }
        }
    }

    fun sendInCallGift(giftName: String, emoji: String, costCoins: Int) {
        val session = (_activeCallUi.value as? ActiveCallUi.Connected)?.session ?: return
        _events.tryEmit("Sent $emoji $giftName ($costCoins Coins) live!")
        val relatedChat = findChatById(session.chatId)
        if (relatedChat != null) {
            val senderRole = when {
                _isVerifiedAdminForCalls.value -> "admin"
                userAccount.value?.isApprovedStaff == true -> "staff"
                else -> "client"
            }
            sendChatMessage(
                chat = relatedChat,
                senderRole = senderRole,
                text = "🎁 Sent $emoji $giftName ($costCoins DateCoins) during Live Call!",
                onBlockedOrError = {},
                onSent = {},
            )
        }
    }

    private fun teardownCallResources() {
        ringTimeoutJob?.cancel()
        ringTimeoutJob = null
        PixoraNotificationManager.stopBackgroundCallRinging()
        ringtonePlayer?.stopAll()
        localVideoCollectJob?.cancel()
        remoteVideoCollectJob?.cancel()
        localVideoCollectJob = null
        remoteVideoCollectJob = null
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        activeCallSessionJob?.cancel()
        activeIceJob?.cancel()
        activeCallSessionJob = null
        activeIceJob = null
        addedRemoteIceKeys.clear()
        appliedRemoteAnswerForCallId = null
        runCatching { webRtcClient?.release() }
        webRtcClient = null
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
        if (senderRole == "staff" && account?.canDirectChatAsStaff != true) {
            onBlockedOrError("Direct client chat is currently managed by DateMate Admin for your profile.")
            return
        }
        val senderName = when (senderRole) {
            "admin" -> "DateMate Concierge"
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
            ?: adminChats.value.firstOrNull { it.id == chatId }
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
