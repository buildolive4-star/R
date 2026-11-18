package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GalleryImage
import com.example.data.model.PhotoRevealState
import com.example.data.model.ProfileTier
import com.google.firebase.Timestamp
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.testUploadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "justus_device_uploads")

@JsonClass(generateAdapter = true)
data class StoredTestUpload(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val thumbUrl: String,
    val photoUrls: List<String> = emptyList(),
    val aspectRatio: Double,
    val createdAtEpochMs: Long,
    val description: String = "",
    val price: String = "₹299",
    val age: Int = 21,
    val role: String = "GF",
    val tier: String = ProfileTier.GRID_9_16,
    val featured: Boolean = false,
    val tags: List<String> = emptyList(),
    val isHidden: Boolean = false,
    val revealExpiresAtEpochMs: Long = 0L,
    val isExpired: Boolean = false,
    val photoReveals: Map<String, HiddenRevealRecord> = emptyMap(),
    val refreshEpochMs: Long = 0L,
) {
    fun toGalleryImage(nowMs: Long = System.currentTimeMillis()): GalleryImage {
        val photos = if (photoUrls.isNotEmpty()) photoUrls else listOfNotNull(imageUrl.takeIf { it.isNotBlank() })
        val photoCount = photos.size.coerceAtLeast(1)

        val perPhotoStates = mutableMapOf<Int, PhotoRevealState>()
        if (isHidden) {
            for (idx in 0 until photoCount) {
                val rec = photoReveals[idx.toString()]
                if (rec != null) {
                    val recExpired = rec.isExpired || (rec.expiresAtEpochMs > 0L && nowMs >= rec.expiresAtEpochMs)
                    perPhotoStates[idx] = PhotoRevealState(
                        expiresAtEpochMs = rec.expiresAtEpochMs,
                        isExpired = recExpired,
                    )
                } else if (photoReveals.isEmpty() && idx == 0 && (isExpired || revealExpiresAtEpochMs > 0L)) {
                    val legacyExpired = isExpired || (revealExpiresAtEpochMs > 0L && nowMs >= revealExpiresAtEpochMs)
                    perPhotoStates[0] = PhotoRevealState(
                        expiresAtEpochMs = revealExpiresAtEpochMs,
                        isExpired = legacyExpired,
                    )
                }
            }
        }

        val allExpiredNow = if (!isHidden) {
            isExpired
        } else {
            (0 until photoCount).all { idx ->
                perPhotoStates[idx]?.isExpired == true
            }
        }
        val firstState = perPhotoStates[0]
        return GalleryImage(
            id = id,
            title = title,
            description = description,
            category = category,
            imageUrl = imageUrl,
            thumbUrl = thumbUrl,
            photoUrls = photos,
            aspectRatio = aspectRatio,
            price = price,
            age = age,
            role = role,
            tier = tier,
            status = "published",
            featured = if (isHidden || allExpiredNow) false else (featured || tier == ProfileTier.VIP_16_9),
            uploadedBy = "justus_admin",
            uploaderName = title,
            tags = tags,
            isHidden = isHidden,
            revealExpiresAtEpochMs = firstState?.expiresAtEpochMs ?: revealExpiresAtEpochMs,
            isExpired = allExpiredNow,
            photoRevealStates = perPhotoStates,
            refreshEpochMs = refreshEpochMs,
            createdAt = Timestamp(createdAtEpochMs / 1000L, 0),
            updatedAt = Timestamp(createdAtEpochMs / 1000L, 0),
        )
    }
}

class LocalTestUploadsRepository(private val context: Context) {
    private val key = stringPreferencesKey("device_profiles_json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, StoredTestUpload::class.java)
    private val adapter = moshi.adapter<List<StoredTestUpload>>(listType)

    val testUploads: Flow<List<GalleryImage>> = context.testUploadsDataStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        decodeList(prefs[key])
            .sortedByDescending { it.createdAtEpochMs }
            .map { it.toGalleryImage(now) }
    }

    suspend fun addUpload(upload: StoredTestUpload) {
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key]).filterNot { it.id == upload.id }
            prefs[key] = adapter.toJson(listOf(upload) + current)
        }
    }

    suspend fun updateUpload(
        imageId: String,
        title: String,
        description: String,
        category: String,
        price: String,
        tags: List<String>,
        featured: Boolean? = null,
        isHidden: Boolean? = null,
        age: Int? = null,
        role: String? = null,
        tier: String? = null,
    ) {
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    val nextHidden = isHidden ?: item.isHidden
                    val nextTier = tier ?: item.tier
                    item.copy(
                        title = title.trim().ifBlank { item.title }.take(80),
                        description = description.trim().take(600),
                        category = category.trim().ifBlank { "Companion" }.take(40),
                        price = price.trim().ifBlank { item.price }.take(32),
                        age = age?.coerceIn(18, 80) ?: item.age,
                        role = role ?: item.role,
                        tier = nextTier,
                        aspectRatio = if (nextTier == ProfileTier.VIP_16_9) 16.0 / 9.0 else 9.0 / 16.0,
                        featured = featured ?: (nextTier == ProfileTier.VIP_16_9),
                        tags = tags.map { it.trim().take(32) }.filter { it.isNotEmpty() }.distinct().take(10),
                        isHidden = nextHidden,
                        revealExpiresAtEpochMs = if (!nextHidden) 0L else item.revealExpiresAtEpochMs,
                        isExpired = if (!nextHidden) false else item.isExpired,
                        photoReveals = if (!nextHidden) emptyMap() else item.photoReveals,
                    )
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
    }

    suspend fun startHiddenRevealCountdown(
        imageId: String,
        photoIndex: Int = 0,
        ttlMs: Long = 40_000L,
    ): Long {
        var expiresAt = 0L
        context.testUploadsDataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId && item.isHidden) {
                    val idxKey = photoIndex.coerceAtLeast(0).toString()
                    val currentMap = item.photoReveals.toMutableMap()
                    val existing = currentMap[idxKey]
                    val resolvedExpiry = when {
                        existing != null && existing.expiresAtEpochMs > 0L -> existing.expiresAtEpochMs
                        photoIndex == 0 && item.photoReveals.isEmpty() && item.revealExpiresAtEpochMs > 0L ->
                            item.revealExpiresAtEpochMs
                        else -> now + ttlMs
                    }
                    expiresAt = resolvedExpiry
                    if (existing == null) {
                        currentMap[idxKey] = HiddenRevealRecord(
                            key = idxKey,
                            expiresAtEpochMs = resolvedExpiry,
                            isExpired = false,
                        )
                    }
                    item.copy(
                        revealExpiresAtEpochMs = if (photoIndex == 0) resolvedExpiry else item.revealExpiresAtEpochMs,
                        photoReveals = currentMap,
                    )
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
        return expiresAt
    }

    suspend fun markExpired(imageId: String, photoIndex: Int = 0) {
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    val idxKey = photoIndex.coerceAtLeast(0).toString()
                    val currentMap = item.photoReveals.toMutableMap()
                    val existing = currentMap[idxKey] ?: HiddenRevealRecord(
                        key = idxKey,
                        expiresAtEpochMs = item.revealExpiresAtEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
                        isExpired = true,
                    )
                    currentMap[idxKey] = existing.copy(isExpired = true)
                    val photoCount = if (item.photoUrls.isNotEmpty()) item.photoUrls.size else 1
                    val allExpired = (0 until photoCount).all { i ->
                        currentMap[i.toString()]?.isExpired == true
                    }
                    item.copy(
                        photoReveals = currentMap,
                        isExpired = allExpired,
                        featured = if (allExpired) false else item.featured,
                    )
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
    }

    suspend fun markExpiredUploads(nowMs: Long = System.currentTimeMillis()): List<String> {
        val fullyExpiredIds = mutableListOf<String>()
        var changed = false
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key])
            val updated = current.map { item ->
                if (!item.isHidden) return@map item
                val photoCount = if (item.photoUrls.isNotEmpty()) item.photoUrls.size else 1
                val nextMap = item.photoReveals.toMutableMap()
                if (nextMap.isEmpty() && item.revealExpiresAtEpochMs > 0L) {
                    nextMap["0"] = HiddenRevealRecord("0", item.revealExpiresAtEpochMs, item.isExpired)
                }
                var itemChanged = false
                for (i in 0 until photoCount) {
                    val k = i.toString()
                    val rec = nextMap[k]
                    if (rec != null && !rec.isExpired && rec.expiresAtEpochMs > 0L && nowMs >= rec.expiresAtEpochMs) {
                        nextMap[k] = rec.copy(isExpired = true)
                        itemChanged = true
                    }
                }
                val allExpiredNow = (0 until photoCount).all { idx ->
                    nextMap[idx.toString()]?.isExpired == true
                }
                if (allExpiredNow && !item.isExpired) {
                    fullyExpiredIds.add(item.id)
                    itemChanged = true
                }
                if (itemChanged) {
                    changed = true
                    item.copy(
                        photoReveals = nextMap,
                        isExpired = allExpiredNow,
                        featured = if (allExpiredNow) false else item.featured,
                    )
                } else {
                    item
                }
            }
            if (changed) {
                prefs[key] = adapter.toJson(updated)
            }
        }
        return fullyExpiredIds
    }

    suspend fun refreshUpload(imageId: String): GalleryImage? {
        var refreshedItem: GalleryImage? = null
        val now = System.currentTimeMillis()
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    val next = item.copy(
                        isHidden = true,
                        isExpired = false,
                        revealExpiresAtEpochMs = 0L,
                        photoReveals = emptyMap(),
                        refreshEpochMs = now,
                    )
                    refreshedItem = next.toGalleryImage(now)
                    next
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
        return refreshedItem
    }

    suspend fun refreshAllExpiredUploads(): List<GalleryImage> {
        val refreshedItems = mutableListOf<GalleryImage>()
        val now = System.currentTimeMillis()
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                val needsRefresh = item.isExpired ||
                    item.photoReveals.isNotEmpty() ||
                    (item.isHidden && item.revealExpiresAtEpochMs > 0L)
                if (needsRefresh) {
                    val next = item.copy(
                        isHidden = true,
                        isExpired = false,
                        revealExpiresAtEpochMs = 0L,
                        photoReveals = emptyMap(),
                        refreshEpochMs = now,
                    )
                    refreshedItems.add(next.toGalleryImage(now))
                    next
                } else {
                    item
                }
            }
            if (refreshedItems.isNotEmpty()) {
                prefs[key] = adapter.toJson(updated)
            }
        }
        return refreshedItems
    }

    suspend fun toggleFeatured(imageId: String): Boolean {
        var newFeatured = false
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    newFeatured = !item.featured
                    item.copy(featured = newFeatured)
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
        return newFeatured
    }

    suspend fun removeUpload(imageId: String) {
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key]).filterNot { it.id == imageId }
            prefs[key] = adapter.toJson(current)
        }
    }

    suspend fun clearAll() {
        context.testUploadsDataStore.edit { prefs ->
            prefs[key] = "[]"
        }
    }

    private fun decodeList(raw: String?): List<StoredTestUpload> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }
}
