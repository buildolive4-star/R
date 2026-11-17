package com.example.ui.gallery

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryCollection
import com.example.data.model.GalleryImage
import com.example.data.model.SavedBookmark
import com.example.data.model.UiState
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val repository: GalleryRepository,
    private val bookmarksRepository: LocalBookmarksRepository,
    private val testUploadsRepository: LocalTestUploadsRepository,
    private val prefsRepository: ThemePreferencesRepository,
    private val notifiedImagesStore: NotifiedImagesStore,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(CATEGORY_ALL)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _newArrivalEvents = MutableSharedFlow<GalleryImage>(extraBufferCapacity = 8)
    val newArrivalEvents: SharedFlow<GalleryImage> = _newArrivalEvents.asSharedFlow()

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
                    .forEach { scheduleExpirationAt(it.id, it.revealExpiresAtEpochMs) }
            }
        }
    }

    val isStaggeredLayout: StateFlow<Boolean> = prefsRepository.isStaggeredLayout
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true,
        )

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

    val testUploadsCount: StateFlow<Int> = testUploadsRepository.testUploads
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
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
            Log.w(TAG, "Error observing gallery images", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync companion feed"))
        }

    val galleryState: StateFlow<UiState<List<GalleryImage>>> = combine(
        remoteGalleryFlow,
        testUploadsRepository.testUploads,
    ) { remoteState, localTests ->
        when (remoteState) {
            is UiState.Loading -> {
                if (localTests.isNotEmpty()) UiState.Success(localTests) else UiState.Loading
            }
            is UiState.Error -> {
                if (localTests.isNotEmpty()) UiState.Success(localTests) else remoteState
            }
            is UiState.Success -> {
                val merged = (localTests + remoteState.data)
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
            listOf(CATEGORY_ALL) + dynamic
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = listOf(CATEGORY_ALL),
        )

    val collections: StateFlow<List<CategoryCollection>> = galleryState
        .map { state ->
            val images = (state as? UiState.Success)?.data ?: return@map emptyList()
            images.groupBy { it.category.trim().ifBlank { "Date Night" } }
                .entries
                .sortedByDescending { it.value.size }
                .map { (name, items) ->
                    CategoryCollection(
                        name = name,
                        count = items.size,
                        coverUrl = items.firstOrNull { !it.isHidden && !it.isDeletedBlurred }
                            ?.displayThumbnailUrl.orEmpty()
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val featuredImages: StateFlow<List<GalleryImage>> = galleryState
        .map { state ->
            val images = (state as? UiState.Success)?.data ?: return@map emptyList()
            val visibleImages = images.filterNot { it.isHidden || it.isDeletedBlurred }
            val explicitFeatured = visibleImages.filter { it.featured }
            (explicitFeatured.ifEmpty { visibleImages }).take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    val filteredImages: StateFlow<UiState<List<GalleryImage>>> = combine(
        galleryState,
        _selectedCategory,
        _searchQuery,
    ) { state, category, query ->
        when (state) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> state
            is UiState.Success -> {
                val trimmedQuery = query.trim().lowercase()
                val filtered = state.data.filter { img ->
                    val matchesCategory = category == CATEGORY_ALL ||
                        img.category.equals(category, ignoreCase = true)
                    val matchesQuery = trimmedQuery.isEmpty() ||
                        img.title.lowercase().contains(trimmedQuery) ||
                        img.description.lowercase().contains(trimmedQuery) ||
                        img.category.lowercase().contains(trimmedQuery) ||
                        img.tags.any { it.lowercase().contains(trimmedQuery) }
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

    fun toggleLayout() {
        viewModelScope.launch {
            prefsRepository.setStaggeredLayout(!isStaggeredLayout.value)
        }
    }

    fun setStaggeredLayout(staggered: Boolean) {
        viewModelScope.launch {
            prefsRepository.setStaggeredLayout(staggered)
        }
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
                _events.tryEmit("Background sync enabled (every 15 min)")
            } else {
                BackgroundSyncScheduler.cancelPeriodicSync(context)
                _events.tryEmit("Background sync paused")
            }
        }
    }

    fun triggerBackgroundCheckNow(context: Context) {
        BackgroundSyncScheduler.triggerImmediateCheck(context)
        _events.tryEmit("Checking for new companion drops…")
    }

    fun notifyExpiredBlurredTap() {
        _events.tryEmit("This moment expired after 20s — only a blurred memory remains.")
    }

    fun revealHiddenImage(imageId: String) {
        viewModelScope.launch {
            val expiresAt = testUploadsRepository.startHiddenRevealCountdown(
                imageId = imageId,
                ttlMs = HIDDEN_REVEAL_TTL_MS,
            )
            if (expiresAt > 0L) {
                scheduleExpirationAt(imageId, expiresAt)
            }
        }
    }

    private fun scheduleExpirationAt(imageId: String, expiresAtEpochMs: Long) {
        if (activeExpirationJobs[imageId]?.isActive == true) return
        activeExpirationJobs[imageId] = viewModelScope.launch {
            val remainingMs = (expiresAtEpochMs - System.currentTimeMillis()).coerceAtLeast(0L)
            if (remainingMs > 0L) {
                delay(remainingMs)
            }
            testUploadsRepository.markExpired(imageId)
            bookmarksRepository.removeBookmark(imageId)
            activeExpirationJobs.remove(imageId)
            _events.tryEmit("Moment deleted · Blurred memory kept")
        }
    }

    fun toggleBookmark(image: GalleryImage) {
        viewModelScope.launch {
            if (image.isHidden || image.isDeletedBlurred) {
                _events.tryEmit("Private 20s & expired moments cannot be saved")
                return@launch
            }
            val nowSaved = bookmarksRepository.toggleBookmark(image)
            _events.tryEmit(
                if (nowSaved) "Saved to favorites" else "Removed from favorites"
            )
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
            _events.tryEmit("Cleared favorite moments")
        }
    }

    fun resolvePagerImages(imageId: String): List<GalleryImage> {
        val galleryList = (galleryState.value as? UiState.Success)?.data.orEmpty()
        val eligibleGallery = galleryList.filter { !it.isDeletedBlurred || it.id == imageId }
        if (eligibleGallery.any { it.id == imageId }) return eligibleGallery
        val bookmarkList = bookmarks.value.map { it.toGalleryImage() }
        if (bookmarkList.any { it.id == imageId }) return bookmarkList
        return emptyList()
    }

    companion object {
        const val CATEGORY_ALL = "All"
        const val HIDDEN_REVEAL_TTL_MS = 20_000L
        private const val TAG = "GalleryVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
