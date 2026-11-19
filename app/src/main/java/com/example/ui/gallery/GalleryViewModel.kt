package com.example.ui.gallery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppUpdateConfig
import com.example.data.model.CategoryCollection
import com.example.data.model.FilterCategoryGroup
import com.example.data.model.GalleryImage
import com.example.data.model.PhotoRevealState
import com.example.data.model.ProfileSortMode
import com.example.data.model.ProfileTier
import com.example.data.model.SavedBookmark
import com.example.data.model.UiState
import com.example.data.preferences.HiddenRevealRecord
import com.example.data.preferences.HiddenRevealStateStore
import com.example.data.preferences.LocalBookmarksRepository
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.preferences.UserEngagementPrefs
import com.example.data.repository.BookingRepository
import com.example.data.repository.GalleryRepository
import com.example.data.repository.UserCrmRepository
import com.example.ui.theme.AppLookTheme
import com.example.ui.theme.ThemeMode
import com.example.util.LocationAddressFetcher
import com.example.workers.BackgroundSyncScheduler
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class OptimisticPhotoReveal(
    val imageId: String,
    val photoIndex: Int,
    val expiresAtEpochMs: Long,
    val refreshEpochMs: Long,
)

class GalleryViewModel(
    private val repository: GalleryRepository,
    private val bookmarksRepository: LocalBookmarksRepository,
    private val testUploadsRepository: LocalTestUploadsRepository,
    private val prefsRepository: ThemePreferencesRepository,
    private val notifiedImagesStore: NotifiedImagesStore,
    private val hiddenRevealStateStore: HiddenRevealStateStore? = null,
    private val userCrmRepository: UserCrmRepository? = null,
    private val bookingRepository: BookingRepository? = null,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedVibe = MutableStateFlow(FILTER_ALL)
    val selectedVibe: StateFlow<String> = _selectedVibe.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(ProfileSortMode.RECOMMENDED)
    val sortMode: StateFlow<ProfileSortMode> = _sortMode.asStateFlow()

    private val _userCity = MutableStateFlow("")
    val userCity: StateFlow<String> = _userCity.asStateFlow()

    private val _userAddress = MutableStateFlow("")
    val userAddress: StateFlow<String> = _userAddress.asStateFlow()

    private val _userGenderPreference = MutableStateFlow("")
    val userGenderPreference: StateFlow<String> = _userGenderPreference.asStateFlow()

    private var lastAppliedPreference: String = ""

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _newArrivalEvents = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 8)
    val newArrivalEvents: SharedFlow<GalleryImage> = _newArrivalEvents.asSharedFlow()

    // Keyed by "${imageId}_${photoIndex}"
    private val optimisticRevealExpiries = MutableStateFlow<Map<String, OptimisticPhotoReveal>>(emptyMap())
    private val knownProfileTokens = mutableSetOf<String>()
    private val knownLocalTokens = mutableSetOf<String>()
    private var initialLocalSnapshotHandled = false
    private var initialFirestoreSnapshotHandled = false
    private val activeExpirationJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            val expiredIds = testUploadsRepository.markExpiredUploads()
            expiredIds.forEach { bookmarksRepository.removeBookmark(it) }
        }
        viewModelScope.launch {
            testUploadsRepository.testUploads.collect { uploads ->
                val currentTokens = uploads.map {
                    NotifiedImagesStore.buildProfileAlertToken(it.id, it.refreshEpochMs)
                }
                if (!initialLocalSnapshotHandled) {
                    knownLocalTokens.addAll(currentTokens)
                    initialLocalSnapshotHandled = true
                } else {
                    val newLocalArrivals = uploads.filter { img ->
                        img.id.isNotBlank() && knownLocalTokens.add(
                            NotifiedImagesStore.buildProfileAlertToken(img.id, img.refreshEpochMs)
                        )
                    }
                    newLocalArrivals.forEach { newImage ->
                        _newArrivalEvents.tryEmit(newImage)
                    }
                }
                uploads.filter { it.isHidden }
                    .forEach { img ->
                        for (idx in 0 until img.photoCount) {
                            if (img.isPhotoRevealStarted(idx)) {
                                val st = img.photoState(idx)
                                scheduleExpirationAt(
                                    imageId = img.id,
                                    expiresAtEpochMs = st.expiresAtEpochMs,
                                    refreshEpochMs = img.refreshEpochMs,
                                    photoIndex = idx,
                                    isRemote = false,
                                )
                            }
                        }
                    }
            }
        }
    }

    val themeMode: StateFlow<ThemeMode> = prefsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ThemeMode.DARK,
        )

    val appLookTheme: StateFlow<AppLookTheme> = prefsRepository.appLookTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppLookTheme.DEFAULT,
        )

    val engagementPrefs: StateFlow<UserEngagementPrefs> = prefsRepository.engagementPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UserEngagementPrefs(),
        )

    val cachedAppUpdateConfig: StateFlow<AppUpdateConfig> = prefsRepository.cachedAppUpdateConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppUpdateConfig(),
        )

    val filterTaxonomy: StateFlow<List<FilterCategoryGroup>> = (
        bookingRepository?.observeFilterTaxonomy() ?: flowOf(FilterCategoryGroup.defaultTaxonomy())
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FilterCategoryGroup.defaultTaxonomy(),
        )

    val backgroundSyncEnabled: StateFlow<Boolean> = notifiedImagesStore.backgroundSyncEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true,
        )

    val lastSyncCheckEpochMs: StateFlow<Long> = notifiedImagesStore.lastSyncCheckEpochMs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L,
        )

    private val remoteGalleryFlow = repository.observePublishedImages()
        .onEach { remoteList ->
            val fetchedTokens = remoteList.mapNotNull { img ->
                img.id.takeIf(String::isNotBlank)?.let {
                    NotifiedImagesStore.buildProfileAlertToken(it, img.refreshEpochMs)
                }
            }
            if (!initialFirestoreSnapshotHandled) {
                val persistedKnown = notifiedImagesStore.getKnownIds()
                knownProfileTokens.addAll(persistedKnown)
                if (persistedKnown.isNotEmpty()) {
                    val missedWhileClosed = remoteList.filter { img ->
                        img.id.isNotBlank() && knownProfileTokens.add(
                            NotifiedImagesStore.buildProfileAlertToken(img.id, img.refreshEpochMs)
                        )
                    }
                    missedWhileClosed.take(3).forEach { newImage ->
                        _newArrivalEvents.tryEmit(newImage)
                    }
                } else {
                    knownProfileTokens.addAll(fetchedTokens)
                }
                notifiedImagesStore.markIdsKnown(fetchedTokens)
                initialFirestoreSnapshotHandled = true
            } else {
                val newcomers = remoteList.filter { img ->
                    img.id.isNotBlank() && knownProfileTokens.add(
                        NotifiedImagesStore.buildProfileAlertToken(img.id, img.refreshEpochMs)
                    )
                }
                if (newcomers.isNotEmpty()) {
                    notifiedImagesStore.markIdsKnown(
                        newcomers.map { NotifiedImagesStore.buildProfileAlertToken(it.id, it.refreshEpochMs) }
                    )
                    newcomers.forEach { newImage ->
                        _newArrivalEvents.tryEmit(newImage)
                    }
                }
            }
        }
        .map<List<GalleryImage>, UiState<List<GalleryImage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing companion profiles", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync companion feed"))
        }

    private val remoteRevealRecordsFlow = hiddenRevealStateStore?.revealStates
        ?: flowOf(emptyMap<String, HiddenRevealRecord>())

    val galleryState: StateFlow<UiState<List<GalleryImage>>> = combine(
        remoteGalleryFlow,
        testUploadsRepository.testUploads,
        remoteRevealRecordsFlow,
        optimisticRevealExpiries,
    ) { remoteState, localTests, remoteRevealMap, optimisticMap ->
        val now = System.currentTimeMillis()

        val applyOptimistic: (GalleryImage) -> GalleryImage = { img ->
            if (!img.isHidden) {
                img
            } else {
                val mergedStates = img.photoRevealStates.toMutableMap()
                for (idx in 0 until img.photoCount) {
                    val opt = optimisticMap["${img.id}_$idx"]
                    if (opt != null && opt.refreshEpochMs == img.refreshEpochMs && mergedStates[idx] == null) {
                        val expiredNow = now >= opt.expiresAtEpochMs
                        mergedStates[idx] = PhotoRevealState(
                            expiresAtEpochMs = opt.expiresAtEpochMs,
                            isExpired = expiredNow,
                        )
                    }
                }
                val allExpired = (0 until img.photoCount).all { i -> mergedStates[i]?.isExpired == true }
                val first = mergedStates[0]
                img.copy(
                    photoRevealStates = mergedStates,
                    revealExpiresAtEpochMs = first?.expiresAtEpochMs ?: img.revealExpiresAtEpochMs,
                    isExpired = allExpired,
                    featured = if (allExpired) false else img.featured,
                )
            }
        }

        val mappedLocal = localTests.map(applyOptimistic)

        when (remoteState) {
            is UiState.Loading -> {
                if (mappedLocal.isNotEmpty()) UiState.Success(mappedLocal) else UiState.Loading
            }
            is UiState.Error -> {
                if (mappedLocal.isNotEmpty()) UiState.Success(mappedLocal) else remoteState
            }
            is UiState.Success -> {
                val mappedRemote = remoteState.data.map { remoteImg ->
                    val withReveal = if (remoteImg.isHidden) {
                        val perPhotoStates = mutableMapOf<Int, PhotoRevealState>()
                        for (idx in 0 until remoteImg.photoCount) {
                            val key = HiddenRevealStateStore.buildKey(remoteImg.id, remoteImg.refreshEpochMs, idx)
                            val legacyKey = if (idx == 0) {
                                HiddenRevealStateStore.legacyKey(remoteImg.id, remoteImg.refreshEpochMs)
                            } else null
                            val record = remoteRevealMap[key] ?: legacyKey?.let { remoteRevealMap[it] }
                            if (record != null) {
                                val expiredNow = record.isExpired || (record.expiresAtEpochMs > 0L && now >= record.expiresAtEpochMs)
                                perPhotoStates[idx] = PhotoRevealState(
                                    expiresAtEpochMs = record.expiresAtEpochMs,
                                    isExpired = expiredNow,
                                )
                                if (record.expiresAtEpochMs > 0L && !expiredNow) {
                                    scheduleExpirationAt(
                                        imageId = remoteImg.id,
                                        expiresAtEpochMs = record.expiresAtEpochMs,
                                        refreshEpochMs = remoteImg.refreshEpochMs,
                                        photoIndex = idx,
                                        isRemote = true,
                                    )
                                }
                            }
                        }
                        val allExpired = (0 until remoteImg.photoCount).all { i ->
                            perPhotoStates[i]?.isExpired == true
                        }
                        val first = perPhotoStates[0]
                        remoteImg.copy(
                            photoRevealStates = perPhotoStates,
                            revealExpiresAtEpochMs = first?.expiresAtEpochMs ?: 0L,
                            isExpired = allExpired,
                            featured = if (allExpired) false else remoteImg.featured,
                        )
                    } else {
                        remoteImg
                    }
                    applyOptimistic(withReveal)
                }

                val merged = (mappedRemote + mappedLocal)
                    .distinctBy { it.id }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                UiState.Success(merged)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    val bookmarks: StateFlow<List<SavedBookmark>> = bookmarksRepository.bookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val bookmarkedIds: StateFlow<Set<String>> = bookmarks
        .map { list -> list.map { it.imageId }.toSet() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    val categories: StateFlow<List<String>> = galleryState
        .map { state ->
            val dynamic = (state as? UiState.Success)?.data
                ?.mapNotNull { it.category.trim().takeIf(String::isNotBlank) }
                ?.distinct()
                ?: emptyList()
            listOf(CATEGORY_ALL, "GF", "BF") + dynamic.filterNot {
                it.equals("GF", true) || it.equals("BF", true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = listOf(CATEGORY_ALL, "GF", "BF"),
        )

    val customHomeFilters: StateFlow<List<String>> = (userCrmRepository?.observeHomeFilters() ?: flowOf(emptyList()))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val homeFilterOptions: StateFlow<List<String>> = customHomeFilters
        .map { adminFilters ->
            (DEFAULT_SMART_FILTERS + adminFilters)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DEFAULT_SMART_FILTERS,
        )

    private data class FilterInputs(
        val category: String,
        val smartFilter: String,
        val query: String,
        val userCity: String,
        val userAddress: String,
    )

    private val filterInputsFlow = combine(
        _selectedCategory,
        _selectedVibe,
        _searchQuery,
        _userCity,
        _userAddress,
    ) { category, smartFilter, query, city, address ->
        FilterInputs(category, smartFilter, query, city, address)
    }

    private fun matchesProfileFilters(item: GalleryImage, inputs: FilterInputs): Boolean {
        val category = inputs.category
        val smartFilter = inputs.smartFilter
        val trimmedQuery = inputs.query.trim().lowercase()
        val matchesCategory = when {
            category == CATEGORY_ALL -> true
            category.equals("GF", true) || category.equals("BF", true) ->
                item.role.equals(category, ignoreCase = true)
            else -> item.category.equals(category, ignoreCase = true) ||
                item.tags.any { it.equals(category, ignoreCase = true) }
        }
        val matchesSmartFilter = matchesSmartFilter(
            item = item,
            filter = smartFilter,
            userCity = inputs.userCity,
            userAddress = inputs.userAddress,
        )
        val matchesQuery = trimmedQuery.isEmpty() ||
            item.title.lowercase().contains(trimmedQuery) ||
            item.description.lowercase().contains(trimmedQuery) ||
            item.category.lowercase().contains(trimmedQuery) ||
            item.city.lowercase().contains(trimmedQuery) ||
            item.role.lowercase().contains(trimmedQuery) ||
            item.price.lowercase().contains(trimmedQuery) ||
            item.tags.any { it.lowercase().contains(trimmedQuery) }
        return matchesCategory && matchesSmartFilter && matchesQuery
    }

    private fun sortProfiles(
        list: List<GalleryImage>,
        inputs: FilterInputs,
        sortMode: ProfileSortMode,
    ): List<GalleryImage> = when (sortMode) {
        ProfileSortMode.RECOMMENDED -> list.sortedWith(
            compareByDescending<GalleryImage> { if (it.staffOnline) 1 else 0 }
                .thenByDescending { nearbyMatchScore(it, inputs.userCity, inputs.userAddress) }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )
        ProfileSortMode.PRICE_LOW -> list.sortedWith(
            compareBy<GalleryImage> { parseNumericPrice(it.price) ?: Int.MAX_VALUE }
                .thenByDescending { if (it.staffOnline) 1 else 0 }
        )
        ProfileSortMode.PRICE_HIGH -> list.sortedWith(
            compareByDescending<GalleryImage> { parseNumericPrice(it.price) ?: 0 }
                .thenByDescending { if (it.staffOnline) 1 else 0 }
        )
        ProfileSortMode.AGE_YOUNG -> list.sortedWith(
            compareBy<GalleryImage> { it.age.coerceAtLeast(18) }
                .thenByDescending { if (it.staffOnline) 1 else 0 }
        )
    }

    val vipProfiles: StateFlow<List<GalleryImage>> = combine(
        galleryState,
        filterInputsFlow,
        _sortMode,
    ) { state, inputs, sortMode ->
        val items = (state as? UiState.Success)?.data ?: return@combine emptyList()
        val allVips = items.filter { it.tier == ProfileTier.VIP_16_9 }
        val filteredVips = allVips.filter { matchesProfileFilters(it, inputs) }
        val result = if (
            filteredVips.isEmpty() &&
            inputs.query.isBlank() &&
            inputs.smartFilter == FILTER_ALL &&
            inputs.category == CATEGORY_ALL
        ) {
            allVips
        } else {
            filteredVips
        }
        sortProfiles(result, inputs, sortMode)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
    )

    val filteredGridProfiles: StateFlow<UiState<List<GalleryImage>>> = combine(
        galleryState,
        filterInputsFlow,
        _sortMode,
    ) { state, inputs, sortMode ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val nonVipOnly = state.data.filter { it.tier != ProfileTier.VIP_16_9 }
                val filtered = nonVipOnly.filter { matchesProfileFilters(it, inputs) }
                UiState.Success(sortProfiles(filtered, inputs, sortMode))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    fun updateUserMatchingContext(
        genderPreference: String,
        city: String,
        address: String,
    ) {
        val cleanPref = genderPreference.trim()
        val cleanCity = city.trim()
        val cleanAddress = address.trim()
        _userCity.value = cleanCity
        _userAddress.value = cleanAddress
        _userGenderPreference.value = cleanPref

        if (cleanPref.isNotEmpty() && cleanPref != lastAppliedPreference) {
            lastAppliedPreference = cleanPref
            val mappedCategory = when {
                cleanPref.contains("GF", ignoreCase = true) && !cleanPref.contains("Both", ignoreCase = true) -> "GF"
                cleanPref.contains("BF", ignoreCase = true) && !cleanPref.contains("Both", ignoreCase = true) -> "BF"
                else -> CATEGORY_ALL
            }
            _selectedCategory.value = mappedCategory
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectVibe(vibe: String) {
        _selectedVibe.value = vibe
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectSortMode(mode: ProfileSortMode) {
        _sortMode.value = mode
    }

    fun resetFilters() {
        _selectedCategory.value = CATEGORY_ALL
        _selectedVibe.value = FILTER_ALL
        _searchQuery.value = ""
        _sortMode.value = ProfileSortMode.RECOMMENDED
    }

    fun claimDailySpinReward(coinsWon: Int, rewardTitle: String) {
        viewModelScope.launch {
            val todayDay = System.currentTimeMillis() / 86_400_000L
            val ok = prefsRepository.claimDailySpinReward(coinsWon, rewardTitle, todayDay)
            if (ok) {
                _events.tryEmit("Daily Bonus Claimed: +$coinsWon DateCoins · $rewardTitle ✨")
            } else {
                _events.tryEmit("Today's lucky bonus is already claimed! Come back tomorrow ❤️")
            }
        }
    }

    fun spendDateCoinsForGift(cost: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = prefsRepository.spendDateCoins(cost)
            if (!ok) {
                _events.tryEmit("Not enough DateCoins! Spin Daily Bonus to earn free coins.")
            }
            onResult(ok)
        }
    }

    fun saveVibePersonality(vibe: String, autoFilterKeyword: String = "") {
        viewModelScope.launch {
            prefsRepository.setVibePersonality(vibe)
            if (autoFilterKeyword.isNotBlank()) {
                _selectedVibe.value = autoFilterKeyword
            }
            _events.tryEmit("Your Date Vibe: $vibe ✨")
        }
    }

    fun saveEmergencyContact(name: String, phone: String) {
        viewModelScope.launch {
            prefsRepository.setEmergencyContact(name, phone)
            _events.tryEmit("Trusted Safety Contact saved ✓")
        }
    }

    fun recordProfileViewed(profileId: String) {
        viewModelScope.launch {
            prefsRepository.recordRecentlyViewed(profileId)
        }
    }

    fun cacheAppUpdateConfig(config: AppUpdateConfig) {
        viewModelScope.launch {
            prefsRepository.cacheAppUpdateConfig(config)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun setAppLookTheme(look: AppLookTheme) {
        viewModelScope.launch {
            prefsRepository.setAppLookTheme(look)
            val label = when (look) {
                AppLookTheme.DEFAULT -> "Default Rose Theme"
                AppLookTheme.GOLD -> "Royal Gold Theme"
                AppLookTheme.SILVER -> "Platinum Silver Theme"
            }
            _events.tryEmit("Switched to $label ✨")
        }
    }

    fun setBackgroundSyncEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            notifiedImagesStore.setBackgroundSyncEnabled(enabled)
            if (enabled) {
                BackgroundSyncScheduler.schedulePeriodicSync(context)
                _events.tryEmit("Live push notifications enabled")
            } else {
                BackgroundSyncScheduler.cancelPeriodicSync(context)
                _events.tryEmit("Background sync paused")
            }
        }
    }

    fun triggerBackgroundCheckNow(context: Context) {
        BackgroundSyncScheduler.triggerImmediateCheck(context)
        val latestList = (galleryState.value as? UiState.Success)?.data.orEmpty()
        val spotlight = latestList.firstOrNull { !it.isDeletedBlurred }
        if (spotlight != null) {
            _newArrivalEvents.tryEmit(spotlight)
        } else {
            _events.tryEmit("Live push notifications active & synced")
        }
    }

    fun notifyExpiredBlurredTap() {
        _events.tryEmit("All photos of this profile are now blurred memories.")
    }

    fun emitInAppAlertPreview(image: GalleryImage) {
        _newArrivalEvents.tryEmit(image)
    }

    fun revealHiddenImage(image: GalleryImage, photoIndex: Int = 0) {
        val imageId = image.id
        val safeIdx = photoIndex.coerceIn(0, (image.photoCount - 1).coerceAtLeast(0))
        val immediateExpiry = System.currentTimeMillis() + HIDDEN_REVEAL_TTL_MS
        val optKey = "${imageId}_$safeIdx"
        optimisticRevealExpiries.update {
            it + (optKey to OptimisticPhotoReveal(imageId, safeIdx, immediateExpiry, image.refreshEpochMs))
        }

        viewModelScope.launch {
            val localExpiresAt = testUploadsRepository.startHiddenRevealCountdown(
                imageId = imageId,
                photoIndex = safeIdx,
                ttlMs = HIDDEN_REVEAL_TTL_MS,
            )
            if (localExpiresAt > 0L) {
                scheduleExpirationAt(imageId, localExpiresAt, image.refreshEpochMs, safeIdx, isRemote = false)
            } else if (hiddenRevealStateStore != null) {
                val remoteExpiresAt = hiddenRevealStateStore.startCountdown(
                    imageId = imageId,
                    refreshEpochMs = image.refreshEpochMs,
                    photoIndex = safeIdx,
                    ttlMs = HIDDEN_REVEAL_TTL_MS,
                )
                scheduleExpirationAt(imageId, remoteExpiresAt, image.refreshEpochMs, safeIdx, isRemote = true)
            } else {
                scheduleExpirationAt(imageId, immediateExpiry, image.refreshEpochMs, safeIdx, isRemote = false)
            }
        }
    }

    private fun scheduleExpirationAt(
        imageId: String,
        expiresAtEpochMs: Long,
        refreshEpochMs: Long,
        photoIndex: Int,
        isRemote: Boolean,
    ) {
        val jobKey = "${imageId}_${refreshEpochMs}_$photoIndex"
        if (activeExpirationJobs[jobKey]?.isActive == true) return
        activeExpirationJobs[jobKey] = viewModelScope.launch {
            val remainingMs = (expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs > 0L) {
                delay(remainingMs)
            }
            if (isRemote) {
                hiddenRevealStateStore?.markExpired(imageId, refreshEpochMs, photoIndex)
            } else {
                testUploadsRepository.markExpired(imageId, photoIndex)
            }
            optimisticRevealExpiries.update { it - "${imageId}_$photoIndex" }
            val currentItem = (galleryState.value as? UiState.Success)?.data?.firstOrNull { it.id == imageId }
            if (currentItem?.isDeletedBlurred == true) {
                bookmarksRepository.removeBookmark(imageId)
            }
            activeExpirationJobs.remove(jobKey)
        }
    }

    fun toggleBookmark(image: GalleryImage) {
        viewModelScope.launch {
            if (image.isDeletedBlurred) {
                _events.tryEmit("Expired memories cannot be added to favorites")
                return@launch
            }
            val nowSaved = bookmarksRepository.toggleBookmark(image)
            _events.tryEmit(if (nowSaved) "Added ${image.title} to Favorites ❤️" else "Removed from Favorites")
        }
    }

    fun removeBookmark(imageId: String) {
        viewModelScope.launch {
            bookmarksRepository.removeBookmark(imageId)
            _events.tryEmit("Removed from Favorites")
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            bookmarksRepository.clearAll()
            _events.tryEmit("Cleared favorite profiles")
        }
    }

    fun resolvePagerImagesFromState(
        state: UiState<List<GalleryImage>>,
        savedBookmarks: List<SavedBookmark>,
        imageId: String,
    ): List<GalleryImage> {
        val galleryList = (state as? UiState.Success)?.data.orEmpty()
        val eligibleGallery = galleryList.filter { !it.isDeletedBlurred || it.id == imageId }
        if (eligibleGallery.any { it.id == imageId }) return eligibleGallery
        val bookmarkList = savedBookmarks.map { it.toGalleryImage() }
        if (bookmarkList.any { it.id == imageId }) return bookmarkList
        return emptyList()
    }

    companion object {
        const val CATEGORY_ALL = "All"
        const val FILTER_ALL = "All"
        const val VIBE_ALL = FILTER_ALL
        const val FILTER_ONLINE = "Online Now"
        const val FILTER_NEARBY = "Nearby You"
        const val FILTER_BUDGET = "Budget (≤₹399)"
        const val FILTER_PREMIUM = "Premium"
        const val FILTER_VERIFIED = "Verified"

        val DEFAULT_SMART_FILTERS = listOf(
            FILTER_ALL,
            FILTER_ONLINE,
            FILTER_NEARBY,
            FILTER_BUDGET,
            FILTER_PREMIUM,
            FILTER_VERIFIED,
        )

        fun parseNumericPrice(price: String): Int? {
            val digits = price.filter { it.isDigit() }
            return digits.toIntOrNull()
        }

        fun nearbyMatchScore(
            item: GalleryImage,
            userCity: String,
            userAddress: String,
        ): Int {
            val userTokens = LocationAddressFetcher.extractLocationTokens(userCity, userAddress)
            if (userTokens.isEmpty()) return 0
            val profileHaystack = buildString {
                append(item.city).append(' ')
                append(item.tags.joinToString(" ")).append(' ')
                append(item.description)
            }.lowercase()
            var score = 0
            if (userCity.trim().length >= 3 && item.city.contains(userCity.trim(), ignoreCase = true)) {
                score += 10
            }
            for (token in userTokens) {
                if (profileHaystack.contains(token)) {
                    score += 3
                }
            }
            return score
        }

        fun isProfileNearby(
            item: GalleryImage,
            userCity: String,
            userAddress: String,
        ): Boolean = nearbyMatchScore(item, userCity, userAddress) > 0

        fun matchesSmartFilter(
            item: GalleryImage,
            filter: String,
            userCity: String,
            userAddress: String,
        ): Boolean {
            if (filter.equals(FILTER_ALL, ignoreCase = true)) return true
            return when {
                filter.equals(FILTER_ONLINE, ignoreCase = true) -> item.staffOnline
                filter.equals(FILTER_NEARBY, ignoreCase = true) -> {
                    val userTokens = LocationAddressFetcher.extractLocationTokens(userCity, userAddress)
                    if (userTokens.isEmpty()) true else isProfileNearby(item, userCity, userAddress)
                }
                filter.equals(FILTER_BUDGET, ignoreCase = true) -> {
                    val numeric = parseNumericPrice(item.price)
                    (numeric != null && numeric <= 399) || item.tags.any { it.equals("Budget", ignoreCase = true) }
                }
                filter.equals(FILTER_PREMIUM, ignoreCase = true) -> {
                    val numeric = parseNumericPrice(item.price)
                    item.isVip || (numeric != null && numeric >= 449) || item.tags.any { it.equals("Premium", ignoreCase = true) }
                }
                filter.equals(FILTER_VERIFIED, ignoreCase = true) -> {
                    item.tags.any { it.contains("Verified", ignoreCase = true) } ||
                        item.category.contains("Verified", ignoreCase = true) ||
                        item.ownerUid.isNotBlank() ||
                        item.uploadedBy.isNotBlank()
                }
                else -> {
                    // Support Admin-created custom & taxonomy filters (price caps like "Under ₹299", Occasions like Dinner/Night, city/role/filterTags)
                    val underMatch = Regex("under\\s*[₹rs.]*\\s*(\\d+)", RegexOption.IGNORE_CASE).find(filter)
                    if (underMatch != null) {
                        val cap = underMatch.groupValues[1].toIntOrNull()
                        val numeric = parseNumericPrice(item.price)
                        cap != null && numeric != null && numeric <= cap
                    } else {
                        item.category.contains(filter, ignoreCase = true) ||
                            item.city.contains(filter, ignoreCase = true) ||
                            item.role.equals(filter, ignoreCase = true) ||
                            item.title.contains(filter, ignoreCase = true) ||
                            item.description.contains(filter, ignoreCase = true) ||
                            item.price.contains(filter, ignoreCase = true) ||
                            item.allFilterTags.any { it.contains(filter, ignoreCase = true) } ||
                            item.bookingPlans.any { it.contains(filter, ignoreCase = true) }
                    }
                }
            }
        }

        const val HIDDEN_REVEAL_TTL_MS = 40_000L
        private const val TAG = "GalleryVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
