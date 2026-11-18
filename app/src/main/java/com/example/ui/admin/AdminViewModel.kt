package com.example.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
import com.example.data.model.AdminUserFilter
import com.example.data.model.ChatThread
import com.example.data.model.CompanionPrivateContact
import com.example.data.model.CompanionProfile
import com.example.data.model.GalleryImage
import com.example.data.model.LaunchBannerConfig
import com.example.data.model.ProfileTier
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.StoredTestUpload
import com.example.data.remote.ImgBbApiService
import com.example.data.remote.ImgBbUploadHelper
import com.example.data.repository.ChatRepository
import com.example.data.repository.GalleryRepository
import com.example.data.repository.UserCrmRepository
import com.example.notifications.OneSignalPushManager
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AdminPublishState {
    data object Idle : AdminPublishState
    data object Uploading : AdminPublishState
    data class Success(val message: String, val imageUrl: String) : AdminPublishState
    data class Error(val message: String) : AdminPublishState
}

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val uploadsRepository: LocalTestUploadsRepository,
    private val galleryRepository: GalleryRepository,
    private val userCrmRepository: UserCrmRepository,
    private val chatRepository: ChatRepository,
    private val imgBbApiService: ImgBbApiService = ImgBbApiService.create(),
) : ViewModel() {

    private val _authUser = MutableStateFlow(Firebase.auth.currentUser)
    val authUser: StateFlow<FirebaseUser?> = _authUser.asStateFlow()

    private val _isVerifiedAdmin = MutableStateFlow(false)
    val isVerifiedAdmin: StateFlow<Boolean> = _isVerifiedAdmin.asStateFlow()

    private val _isSuperAdmin = MutableStateFlow(false)
    val isSuperAdmin: StateFlow<Boolean> = _isSuperAdmin.asStateFlow()

    private val _publishState = MutableStateFlow<AdminPublishState>(AdminPublishState.Idle)
    val publishState: StateFlow<AdminPublishState> = _publishState.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _uploadedPreviewAlert = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 8)
    val uploadedPreviewAlert: SharedFlow<GalleryImage> = _uploadedPreviewAlert.asSharedFlow()

    private val _selectedUserFilter = MutableStateFlow(AdminUserFilter.ALL)
    val selectedUserFilter: StateFlow<AdminUserFilter> = _selectedUserFilter.asStateFlow()

    private val _visibleUsersLimit = MutableStateFlow(PAGE_SIZE_USERS)
    val visibleUsersLimit: StateFlow<Int> = _visibleUsersLimit.asStateFlow()

    private val _selectedProfileContact = MutableStateFlow<Pair<String, CompanionPrivateContact>?>(null)
    val selectedProfileContact: StateFlow<Pair<String, CompanionPrivateContact>?> = _selectedProfileContact.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val myAdminMember: StateFlow<AdminMemberProfile?> = _authUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null) else userCrmRepository.observeMyAdminMember(user.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val adminAccessCodes: StateFlow<List<AdminAccessCode>> = _isSuperAdmin
        .flatMapLatest { isSuper ->
            if (isSuper) userCrmRepository.observeAdminAccessCodes() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminMembers: StateFlow<List<AdminMemberProfile>> = _isSuperAdmin
        .flatMapLatest { isSuper ->
            if (isSuper) userCrmRepository.observeAdminMembers() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val launchBanner: StateFlow<LaunchBannerConfig?> = userCrmRepository.observeLaunchBanner()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _authUser.value = auth.currentUser
        refreshAdminAccess()
    }

    init {
        Firebase.auth.addAuthStateListener(authListener)
        refreshAdminAccess()
        // Live revocation guard: if Main Admin deletes the partner's transfer code, immediately lock Admin Panel
        viewModelScope.launch {
            myAdminMember
                .flatMapLatest { member ->
                    val code = member?.codeUsed.orEmpty()
                    if (member == null || code == "MASTER" || code.isBlank()) {
                        flowOf(true)
                    } else {
                        userCrmRepository.observeCodeActive(code)
                    }
                }
                .collect { active ->
                    if (!active && !_isSuperAdmin.value) {
                        refreshAdminAccess()
                    }
                }
        }
    }

    override fun onCleared() {
        Firebase.auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun refreshAdminAccess() {
        val user = Firebase.auth.currentUser
        _authUser.value = user
        _isSuperAdmin.value = userCrmRepository.isSuperAdminEmail(user?.email)
        if (user == null) {
            _isVerifiedAdmin.value = false
            return
        }
        viewModelScope.launch {
            _isVerifiedAdmin.value = userCrmRepository.checkIsCurrentUserAdmin()
        }
    }

    fun signInAdminWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            _isBusy.value = true
            val res = GoogleAuthHelper.signInWithGoogle(activityContext)
            res.onSuccess { user ->
                _authUser.value = user
                _authErrorMessage.value = null
                val isAdmin = userCrmRepository.checkIsCurrentUserAdmin()
                _isVerifiedAdmin.value = isAdmin
                _isSuperAdmin.value = userCrmRepository.isSuperAdminEmail(user.email)
                OneSignalPushManager.syncUserIdentity(
                    uid = user.uid,
                    isAdmin = isAdmin,
                    isApprovedStaff = false,
                )
                _isBusy.value = false
                if (isAdmin) {
                    _events.tryEmit("Signed in as Admin: ${user.email}")
                } else {
                    _events.tryEmit("Google verified! Enter Admin Code given by Main Admin.")
                }
            }.onFailure { err ->
                _isBusy.value = false
                val message = err.localizedMessage ?: "Google sign-in failed. Please try again."
                _authErrorMessage.value = message
                _events.tryEmit(message)
            }
        }
    }

    fun signOutAdmin(context: Context) {
        viewModelScope.launch {
            GoogleAuthHelper.signOut(context)
            _authUser.value = null
            _isVerifiedAdmin.value = false
            _isSuperAdmin.value = false
            _events.tryEmit("Signed out from Admin Cloud")
        }
    }

    val adminEmails: StateFlow<List<AdminEmailEntry>> = _isVerifiedAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) userCrmRepository.observeAdminEmails() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cloudCompanionProfiles: StateFlow<List<CompanionProfile>> = _isVerifiedAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) galleryRepository.observeAllProfilesForAdmin() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val localDeviceUploads: StateFlow<List<GalleryImage>> = uploadsRepository.testUploads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val combinedManagedImages: StateFlow<List<GalleryImage>> = combine(
        cloudCompanionProfiles,
        localDeviceUploads,
    ) { cloud, local ->
        (cloud.map { it.toGalleryImage() } + local).distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val allCrmUsers: StateFlow<List<UserAccount>> = _isVerifiedAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) userCrmRepository.observeUsersForAdmin() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredUsers: StateFlow<List<UserAccount>> = combine(
        allCrmUsers,
        _selectedUserFilter,
    ) { users, filter ->
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24L * 3600_000L)
        when (filter) {
            AdminUserFilter.ALL -> users
            AdminUserFilter.ONLINE -> users.filter { it.isRecentlyOnline }
            AdminUserFilter.NOTIFIED -> users.filter { it.hasInquiryNotification }
            AdminUserFilter.OLD_USERS -> users.filter {
                val createdMs = it.createdAt?.toDate()?.time ?: it.lastActiveEpochMs
                createdMs in 1..sevenDaysAgo
            }
            AdminUserFilter.HOT_LEAD -> users.filter { it.leadTag == "hot" }
            AdminUserFilter.WARM_LEAD -> users.filter { it.leadTag == "warm" }
            AdminUserFilter.COLD_LEAD -> users.filter { it.leadTag == "cold" }
            AdminUserFilter.BLOCKED -> users.filter { it.moderationStatus == "blocked" }
            AdminUserFilter.SUSPENDED -> users.filter { it.moderationStatus == "suspended" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pagedUsers: StateFlow<List<UserAccount>> = combine(
        filteredUsers,
        _visibleUsersLimit,
    ) { users, limit ->
        users.take(limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val hasMoreUsersToLoad: StateFlow<Boolean> = combine(
        filteredUsers,
        _visibleUsersLimit,
    ) { users, limit ->
        users.size > limit
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val pendingStaffUsers: StateFlow<List<UserAccount>> = allCrmUsers
        .map { users -> users.filter { it.staffStatus == StaffStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val approvedStaffUsers: StateFlow<List<UserAccount>> = allCrmUsers
        .map { users -> users.filter { it.staffStatus == StaffStatus.APPROVED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allChats: StateFlow<List<ChatThread>> = _isVerifiedAdmin
        .flatMapLatest { isAdmin ->
            if (isAdmin) chatRepository.observeAllChatsForAdmin() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun selectUserFilter(filter: AdminUserFilter) {
        _selectedUserFilter.value = filter
        _visibleUsersLimit.value = PAGE_SIZE_USERS
    }

    fun loadNextUsersPage() {
        _visibleUsersLimit.value += PAGE_SIZE_USERS
    }

    fun addAdminEmail(email: String) {
        viewModelScope.launch {
            val res = userCrmRepository.addAdminEmail(email)
            res.onSuccess {
                _events.tryEmit("Added co-admin: ${email.trim().lowercase()}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to add admin")
            }
        }
    }

    fun removeAdminEmail(email: String) {
        viewModelScope.launch {
            val res = userCrmRepository.removeAdminEmail(email)
            res.onSuccess {
                _events.tryEmit("Removed co-admin: $email")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to remove admin")
            }
        }
    }

    fun createAdminAccessCode(rawCode: String, label: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.createAdminAccessCode(rawCode, label)
            _isBusy.value = false
            res.onSuccess { created ->
                _events.tryEmit("Transfer Code saved: $created")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to create code")
            }
        }
    }

    fun deleteAdminAccessCode(code: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.deleteAdminAccessCode(code)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Code $code deleted! Access revoked immediately.")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to delete code")
            }
        }
    }

    fun removeAdminMember(memberUid: String) {
        viewModelScope.launch {
            val res = userCrmRepository.removeAdminMember(memberUid)
            res.onSuccess {
                _events.tryEmit("Admin operator removed")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to remove operator")
            }
        }
    }

    fun saveMainAdminProfile(name: String, phone: String, email: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.saveMainAdminProfile(name, phone, email)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Main Admin details saved")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save Main Admin details")
            }
        }
    }

    fun redeemAdminAccessCode(name: String, phone: String, email: String, code: String) {
        viewModelScope.launch {
            _authErrorMessage.value = null
            _isBusy.value = true
            val res = userCrmRepository.redeemAdminAccessCode(name, phone, email, code)
            if (res.isSuccess) {
                val isAdmin = userCrmRepository.checkIsCurrentUserAdmin()
                _isVerifiedAdmin.value = isAdmin
                OneSignalPushManager.syncUserIdentity(
                    uid = _authUser.value?.uid,
                    isAdmin = isAdmin,
                    isApprovedStaff = false,
                )
                _isBusy.value = false
                if (isAdmin) {
                    _events.tryEmit("Admin access unlocked!")
                } else {
                    _authErrorMessage.value = "Code verification failed. Ask Main Admin for a valid code."
                }
            } else {
                _isBusy.value = false
                val msg = res.exceptionOrNull()?.localizedMessage
                    ?: "Invalid or deleted Admin Code."
                _authErrorMessage.value = msg
                _events.tryEmit(msg)
            }
        }
    }

    fun publishCompanionProfile(
        context: Context,
        selectedUris: List<Uri>,
        manualUrl: String,
        name: String,
        phone: String,
        ageText: String,
        price: String,
        address: String,
        role: String,
        tier: String,
        category: String,
        bio: String,
        hiddenTimed: Boolean,
    ) {
        val cleanName = name.trim()
        val cleanAge = ageText.trim().toIntOrNull() ?: 21
        val cleanPrice = price.trim().ifBlank { "₹499" }
        val rawUrl = manualUrl.trim()
        if (cleanName.isEmpty()) {
            _publishState.value = AdminPublishState.Error("Companion name is required")
            return
        }
        if (selectedUris.isEmpty() && rawUrl.isEmpty()) {
            _publishState.value = AdminPublishState.Error("Select at least one photo or paste an HTTPS URL")
            return
        }

        viewModelScope.launch {
            _publishState.value = AdminPublishState.Uploading
            val resolvedPhotoUrls = mutableListOf<String>()

            for (uri in selectedUris.take(8)) {
                val res = if (ImgBbApiService.isApiKeyConfigured()) {
                    ImgBbUploadHelper.uploadUriToImgBb(context.applicationContext, imgBbApiService, uri, cleanName, category)
                } else {
                    ImgBbUploadHelper.saveUriLocallyForDeviceFeed(context.applicationContext, uri, cleanName, category)
                }
                val uploaded = res.getOrElse { err ->
                    _publishState.value = AdminPublishState.Error(err.localizedMessage ?: "Photo upload failed")
                    return@launch
                }
                resolvedPhotoUrls.add(uploaded.imageUrl)
            }

            if (rawUrl.isNotEmpty()) {
                val normalized = if (rawUrl.startsWith("http://")) "https://" + rawUrl.removePrefix("http://") else rawUrl
                if (normalized.startsWith("https://")) {
                    resolvedPhotoUrls.add(normalized)
                } else if (resolvedPhotoUrls.isEmpty()) {
                    _publishState.value = AdminPublishState.Error("Enter a valid https:// photo URL")
                    return@launch
                }
            }

            val ratio = if (tier == ProfileTier.VIP_16_9) 16.0 / 9.0 else 9.0 / 16.0
            val cover = resolvedPhotoUrls.first()

            if (_isVerifiedAdmin.value && resolvedPhotoUrls.all { it.startsWith("https://") }) {
                val cloudRes = galleryRepository.createCompanionProfile(
                    name = cleanName,
                    age = cleanAge,
                    price = cleanPrice,
                    role = role,
                    category = category.ifBlank { if (tier == ProfileTier.VIP_16_9) "VIP" else "Companion" },
                    tier = tier,
                    bio = bio,
                    photoUrls = resolvedPhotoUrls,
                    hiddenTimed = hiddenTimed,
                    phone = phone,
                    address = address,
                )
                cloudRes.onSuccess { newId ->
                    val previewImg = GalleryImage(
                        id = newId,
                        title = cleanName,
                        description = bio,
                        category = category,
                        imageUrl = cover,
                        thumbUrl = cover,
                        photoUrls = resolvedPhotoUrls,
                        aspectRatio = ratio,
                        price = cleanPrice,
                        age = cleanAge,
                        role = role,
                        tier = tier,
                        isHidden = hiddenTimed,
                    )
                    _uploadedPreviewAlert.tryEmit(previewImg)
                    OneSignalPushManager.sendNewProfilePushAsync(
                        profileId = newId,
                        profileName = cleanName,
                        price = cleanPrice,
                        category = category.ifBlank { if (tier == ProfileTier.VIP_16_9) "VIP" else "Companion" },
                        coverUrl = cover,
                        isHidden = hiddenTimed,
                    )
                    _publishState.value = AdminPublishState.Success(
                        message = "$cleanName ($cleanPrice) published to JustUs!",
                        imageUrl = cover,
                    )
                    return@launch
                }.onFailure { err ->
                    _publishState.value = AdminPublishState.Error(err.localizedMessage ?: "Cloud publish failed")
                    return@launch
                }
            } else {
                // Device-local fallback when Admin isn't signed in with Google yet or IMGBB key isn't set
                val localId = "prof_${UUID.randomUUID().toString().take(8)}"
                val upload = StoredTestUpload(
                    id = localId,
                    title = cleanName,
                    category = category.ifBlank { if (tier == ProfileTier.VIP_16_9) "VIP" else "Companion" },
                    imageUrl = cover,
                    thumbUrl = cover,
                    photoUrls = resolvedPhotoUrls,
                    aspectRatio = ratio,
                    createdAtEpochMs = System.currentTimeMillis(),
                    description = bio,
                    price = cleanPrice,
                    age = cleanAge,
                    role = role,
                    tier = tier,
                    featured = tier == ProfileTier.VIP_16_9,
                    isHidden = hiddenTimed,
                    refreshEpochMs = System.currentTimeMillis(),
                )
                uploadsRepository.addUpload(upload)
                _uploadedPreviewAlert.tryEmit(upload.toGalleryImage())
                _publishState.value = AdminPublishState.Success(
                    message = "$cleanName ($cleanPrice) published on device!",
                    imageUrl = cover,
                )
            }
        }
    }

    fun resetPublishState() {
        _publishState.value = AdminPublishState.Idle
    }

    fun loadPrivateContactForProfile(profileId: String) {
        _selectedProfileContact.value = null
        if (!_isVerifiedAdmin.value) return
        viewModelScope.launch {
            galleryRepository.getPrivateContactForAdmin(profileId)
                .onSuccess { contact ->
                    _selectedProfileContact.value = profileId to contact
                }
        }
    }

    fun refreshBlurredUpload(profileId: String) {
        viewModelScope.launch {
            val refreshed = uploadsRepository.refreshUpload(profileId)
            val remoteMatch = cloudCompanionProfiles.value.firstOrNull { it.id == profileId }
            if (_isVerifiedAdmin.value) {
                galleryRepository.refreshRemoteImage(profileId)
            }
            val pushTarget = refreshed ?: remoteMatch?.toGalleryImage()
            if (pushTarget != null) {
                OneSignalPushManager.sendNewProfilePushAsync(
                    profileId = pushTarget.id,
                    profileName = pushTarget.title,
                    price = pushTarget.displayPrice,
                    category = pushTarget.category,
                    coverUrl = pushTarget.displayThumbnailUrl,
                    isHidden = pushTarget.isHidden,
                )
            }
            if (refreshed != null) {
                _uploadedPreviewAlert.tryEmit(refreshed)
            }
            _events.tryEmit("Refreshed! Profile is unlockable again.")
        }
    }

    fun refreshAllBlurredUploads() {
        viewModelScope.launch {
            val refreshedLocal = uploadsRepository.refreshAllExpiredUploads()
            val cloudTimed = cloudCompanionProfiles.value.filter { it.hiddenTimed }
            if (_isVerifiedAdmin.value) {
                cloudTimed.forEach { galleryRepository.refreshRemoteImage(it.id) }
            }
            val pushFirst = refreshedLocal.firstOrNull() ?: cloudTimed.firstOrNull()?.toGalleryImage()
            if (pushFirst != null) {
                OneSignalPushManager.sendNewProfilePushAsync(
                    profileId = pushFirst.id,
                    profileName = pushFirst.title,
                    price = pushFirst.displayPrice,
                    category = pushFirst.category,
                    coverUrl = pushFirst.displayThumbnailUrl,
                    isHidden = pushFirst.isHidden,
                )
            }
            refreshedLocal.firstOrNull()?.let { _uploadedPreviewAlert.tryEmit(it) }
            _events.tryEmit("Refreshed secret drops for users!")
        }
    }

    fun setUserLeadTag(userId: String, leadTag: String) {
        viewModelScope.launch {
            userCrmRepository.updateUserLeadTag(userId, leadTag)
                .onSuccess { _events.tryEmit("Lead updated: ${leadTag.uppercase()}") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update lead") }
        }
    }

    fun setUserModerationStatus(userId: String, status: String) {
        viewModelScope.launch {
            userCrmRepository.updateUserModerationStatus(userId, status)
                .onSuccess { _events.tryEmit("Account status: ${status.uppercase()}") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update moderation") }
        }
    }

    fun approveStaffUser(user: UserAccount, tier: String) {
        viewModelScope.launch {
            userCrmRepository.approveStaffApplication(user, tier, galleryRepository)
                .onSuccess {
                    _events.tryEmit("${user.name} verified & approved as Staff!")
                }
                .onFailure {
                    _events.tryEmit(it.localizedMessage ?: "Could not verify staff")
                }
        }
    }

    fun rejectStaffUser(userId: String) {
        viewModelScope.launch {
            userCrmRepository.rejectStaffApplication(userId)
                .onSuccess { _events.tryEmit("Staff application rejected") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to update application") }
        }
    }

    fun updateCompanionProfileDetails(
        profileId: String,
        name: String,
        ageText: String,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        phone: String?,
        address: String?,
    ) {
        val cleanName = name.trim().ifBlank { "Companion" }
        val cleanAge = (ageText.trim().toIntOrNull() ?: 21).coerceIn(18, 80)
        val cleanPrice = price.trim().ifBlank { "₹499" }
        viewModelScope.launch {
            _isBusy.value = true
            uploadsRepository.updateUpload(
                imageId = profileId,
                title = cleanName,
                description = bio,
                category = category,
                price = cleanPrice,
                tags = emptyList(),
                featured = tier == ProfileTier.VIP_16_9,
                isHidden = hiddenTimed,
                age = cleanAge,
                role = role,
                tier = tier,
            )
            val isRemote = cloudCompanionProfiles.value.any { it.id == profileId }
            if (isRemote && _isVerifiedAdmin.value) {
                val res = galleryRepository.updateCompanionProfile(
                    profileId = profileId,
                    name = cleanName,
                    age = cleanAge,
                    price = cleanPrice,
                    role = role,
                    category = category,
                    tier = tier,
                    bio = bio,
                    hiddenTimed = hiddenTimed,
                    phone = phone,
                    address = address,
                )
                _isBusy.value = false
                res.onSuccess {
                    if (phone != null || address != null) {
                        loadPrivateContactForProfile(profileId)
                    }
                    _events.tryEmit("Updated $cleanName · $cleanPrice")
                }.onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Cloud update failed")
                }
            } else {
                _isBusy.value = false
                _events.tryEmit("Updated $cleanName · $cleanPrice")
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            uploadsRepository.removeUpload(profileId)
            if (_isVerifiedAdmin.value) {
                galleryRepository.deleteCompanionProfile(profileId)
            }
            _events.tryEmit("Profile removed")
        }
    }

    fun sendAdminReply(
        chat: ChatThread,
        text: String,
        onSent: () -> Unit,
    ) {
        viewModelScope.launch {
            val adminName = _authUser.value?.displayName?.takeIf { it.isNotBlank() } ?: "JustUs Admin"
            chatRepository.sendMessage(
                chat = chat,
                senderRole = "admin",
                senderName = adminName,
                rawText = text,
            ).onSuccess {
                onSent()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to send reply")
            }
        }
    }

    fun powerResetAllChats() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.powerResetAllChats()
            _isBusy.value = false
            res.onSuccess { (deletedChats, deletedMessages) ->
                _events.tryEmit(
                    "Power Reset complete: cleared $deletedChats chat(s) & $deletedMessages message(s)."
                )
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Power Reset failed")
            }
        }
    }

    fun uploadLaunchBannerImage(
        context: Context,
        selectedUri: Uri?,
        manualUrl: String,
        title: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            var resolvedUrl = manualUrl.trim().let {
                if (it.startsWith("http://")) "https://" + it.removePrefix("http://") else it
            }
            if (selectedUri != null) {
                if (!ImgBbApiService.isApiKeyConfigured()) {
                    _isBusy.value = false
                    _events.tryEmit("ImgBB API Key required in Secrets to upload gallery images, or paste an https:// URL.")
                    return@launch
                }
                val uploadRes = ImgBbUploadHelper.uploadUriToImgBb(
                    context = context.applicationContext,
                    apiService = imgBbApiService,
                    imageUri = selectedUri,
                    title = title.ifBlank { "JustUs Launch Banner" },
                    category = "Announcement",
                )
                resolvedUrl = uploadRes.getOrElse { err ->
                    _isBusy.value = false
                    _events.tryEmit(err.localizedMessage ?: "Launch image upload failed")
                    return@launch
                }.imageUrl
            }

            if (!resolvedUrl.startsWith("https://")) {
                _isBusy.value = false
                _events.tryEmit("Please pick an image or enter a valid https:// URL.")
                return@launch
            }

            val saveRes = userCrmRepository.saveLaunchBanner(resolvedUrl, title)
            _isBusy.value = false
            saveRes.onSuccess {
                _events.tryEmit("Launch 3s Info Image published for all users!")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to save launch image")
            }
        }
    }

    fun removeLaunchBanner(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = userCrmRepository.removeLaunchBanner()
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Launch Info Image removed.")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to remove launch image")
            }
        }
    }

    fun deleteChatThreadAsAdmin(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChatThread(chatId)
                .onSuccess { _events.tryEmit("Chat deleted") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to delete chat") }
        }
    }

    companion object {
        const val PAGE_SIZE_USERS = 30
    }
}
