package com.example.ui.gallery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryCollection
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileTier
import com.example.data.model.SavedBookmark
import com.example.data.model.UiState
import com.example.data.preferences.HiddenRevealRecord
import com.example.data.preferences.HiddenRevealStateStore
import com.example.data.preferences.LocalBookmarksRepository
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.GalleryRepository
import com.example.ui.theme.ThemeMode
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

class GalleryViewModel(
    private val repository: GalleryRepository,
    private val bookmarksRepository: LocalBookmarksRepository,
    private val testUploadsRepository: LocalTestUploadsRepository,
    private val prefsRepository: ThemePreferencesRepository,
    private val notifiedImagesStore: NotifiedImagesStore,
    private val hiddenRevealStateStore: HiddenRevealStateStore? = null,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _newArrivalEvents = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 8)
    val newArrivalEvents: SharedFlow<GalleryImage> = _newArrivalEvents.asSharedFlow()

    private val optimisticRevealExpiries = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
    private val knownFirestoreIds = mutableSetOf<String>()
    private var initialFirestoreSnapshotHandled = false
    private val activeExpirationJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            val expiredIds = testUploadsRepository.markExpiredUploads()
            expiredIds.forEach { bookmarksRepository.removeBookmark(it) }
        }
        viewModelScope.launch {
            testUploadsRepository.testUploads.collect { uploads ->
                uploads.filter { it.isHidden && !it.isExpired && it.revealExpiresAtEpochMs > 0L }
                    .forEach { scheduleExpirationAt(it.id, it.revealExpiresAtEpochMs, it.refreshEpochMs, isRemote = false) }
            }
        }
    }

    val themeMode: StateFlow<ThemeMode> = prefsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ThemeMode.DARK,
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
            val fetchedIds = remoteList.mapNotNull { it.id.takeIf(String::isNotBlank) }
            if (!initialFirestoreSnapshotHandled) {
                val persistedKnown = notifiedImagesStore.getKnownIds()
                knownFirestoreIds.addAll(persistedKnown)
                if (persistedKnown.isNotEmpty()) {
                    val missedWhileClosed = remoteList.filter { it.id.isNotBlank() && knownFirestoreIds.add(it.id) }
                    missedWhileClosed.take(3).forEach { newImage ->
                        _newArrivalEvents.tryEmit(newImage)
                    }
                } else {
                    knownFirestoreIds.addAll(fetchedIds)
                }
                notifiedImagesStore.markIdsKnown(fetchedIds)
                initialFirestoreSnapshotHandled = true
            } else {
                val newcomers = remoteList.filter { it.id.isNotBlank() && knownFirestoreIds.add(it.id) }
                if (newcomers.isNotEmpty()) {
                    notifiedImagesStore.markIdsKnown(newcomers.map { it.id })
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
            val opt = optimisticMap[img.id]
            if (img.isHidden && !img.isExpired && img.revealExpiresAtEpochMs == 0L &&
                opt != null && opt.second == img.refreshEpochMs
            ) {
                val expiredNow = now >= opt.first
                img.copy(
                    revealExpiresAtEpochMs = opt.first,
                    isExpired = expiredNow,
                )
            } else {
                img
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
                        val key = HiddenRevealStateStore.buildKey(remoteImg.id, remoteImg.refreshEpochMs)
                        val record = remoteRevealMap[key]
                        if (record != null) {
                            val expiredNow = record.isExpired || (record.expiresAtEpochMs > 0L && now >= record.expiresAtEpochMs)
                            if (record.expiresAtEpochMs > 0L && !expiredNow) {
                                scheduleExpirationAt(
                                    imageId = remoteImg.id,
                                    expiresAtEpochMs = record.expiresAtEpochMs,
                                    refreshEpochMs = remoteImg.refreshEpochMs,
                                    isRemote = true,
                                )
                            }
                            remoteImg.copy(
                                revealExpiresAtEpochMs = record.expiresAtEpochMs,
                                isExpired = expiredNow,
                            )
                        } else {
                            remoteImg
                        }
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

    val vipProfiles: StateFlow<List<GalleryImage>> = galleryState
        .map { state ->
            val items = (state as? UiState.Success)?.data ?: return@map emptyList()
            items.filter { it.tier == ProfileTier.VIP_16_9 }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val filteredGridProfiles: StateFlow<UiState<List<GalleryImage>>> = combine(
        galleryState,
        _selectedCategory,
        _searchQuery,
    ) { state, category, query ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmedQuery = query.trim().lowercase()
                val baseList = if (category == CATEGORY_ALL && trimmedQuery.isEmpty()) {
                    state.data.filter { it.tier != ProfileTier.VIP_16_9 }
                } else {
                    state.data
                }
                val filtered = baseList.filter { item ->
                    val matchesCategory = when {
                        category == CATEGORY_ALL -> true
                        category.equals("GF", true) || category.equals("BF", true) ->
                            item.role.equals(category, ignoreCase = true)
                        else -> item.category.equals(category, ignoreCase = true)
                    }
                    val matchesQuery = trimmedQuery.isEmpty() ||
                        item.title.lowercase().contains(trimmedQuery) ||
                        item.description.lowercase().contains(trimmedQuery) ||
                        item.category.lowercase().contains(trimmedQuery) ||
                        item.role.lowercase().contains(trimmedQuery) ||
                        item.price.lowercase().contains(trimmedQuery)
                    matchesCategory && matchesQuery
                }
                UiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetFilters() {
        _selectedCategory.value = CATEGORY_ALL
        _searchQuery.value = ""
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.setThemeMode(mode)
        }
    }

    fun setBackgroundSyncEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            notifiedImagesStore.setBackgroundSyncEnabled(enabled)
            if (enabled) {
                BackgroundSyncScheduler.schedulePeriodicSync(context)
                _events.tryEmit("Background sync enabled")
            } else {
                BackgroundSyncScheduler.cancelPeriodicSync(context)
                _events.tryEmit("Background sync paused")
            }
        }
    }

    fun triggerBackgroundCheckNow(context: Context) {
        BackgroundSyncScheduler.triggerImmediateCheck(context)
        _events.tryEmit("Checking for new live profiles…")
    }

    fun notifyExpiredBlurredTap() {
        _events.tryEmit("This photo has faded into a memory.")
    }

    fun emitInAppAlertPreview(image: GalleryImage) {
        _newArrivalEvents.tryEmit(image)
    }

    fun revealHiddenImage(image: GalleryImage) {
        val imageId = image.id
        val immediateExpiry = System.currentTimeMillis() + HIDDEN_REVEAL_TTL_MS
        optimisticRevealExpiries.update { it + (imageId to (immediateExpiry to image.refreshEpochMs)) }

        viewModelScope.launch {
            val localExpiresAt = testUploadsRepository.startHiddenRevealCountdown(
                imageId = imageId,
                ttlMs = HIDDEN_REVEAL_TTL_MS,
            )
            if (localExpiresAt > 0L) {
                scheduleExpirationAt(imageId, localExpiresAt, image.refreshEpochMs, isRemote = false)
            } else if (hiddenRevealStateStore != null) {
                val remoteExpiresAt = hiddenRevealStateStore.startCountdown(
                    imageId = imageId,
                    refreshEpochMs = image.refreshEpochMs,
                    ttlMs = HIDDEN_REVEAL_TTL_MS,
                )
                scheduleExpirationAt(imageId, remoteExpiresAt, image.refreshEpochMs, isRemote = true)
            } else {
                scheduleExpirationAt(imageId, immediateExpiry, image.refreshEpochMs, isRemote = false)
            }
        }
    }

    private fun scheduleExpirationAt(
        imageId: String,
        expiresAtEpochMs: Long,
        refreshEpochMs: Long,
        isRemote: Boolean,
    ) {
        val jobKey = "${imageId}_$refreshEpochMs"
        if (activeExpirationJobs[jobKey]?.isActive == true) return
        activeExpirationJobs[jobKey] = viewModelScope.launch {
            val remainingMs = (expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs > 0L) {
                delay(remainingMs)
            }
            if (isRemote) {
                hiddenRevealStateStore?.markExpired(imageId, refreshEpochMs)
            } else {
                testUploadsRepository.markExpired(imageId)
            }
            optimisticRevealExpiries.update { it - imageId }
            bookmarksRepository.removeBookmark(imageId)
            activeExpirationJobs.remove(jobKey)
        }
    }

    fun toggleBookmark(image: GalleryImage) {
        viewModelScope.launch {
            if (image.isHidden || image.isDeletedBlurred) {
                _events.tryEmit("Private moments cannot be bookmarked")
                return@launch
            }
            val nowSaved = bookmarksRepository.toggleBookmark(image)
            _events.tryEmit(if (nowSaved) "Saved to favorites" else "Removed from favorites")
        }
    }

    fun removeBookmark(imageId: String) {
        viewModelScope.launch {
            bookmarksRepository.removeBookmark(imageId)
            _events.tryEmit("Removed from favorites")
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
        const val HIDDEN_REVEAL_TTL_MS = 40_000L
        private const val TAG = "GalleryVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
