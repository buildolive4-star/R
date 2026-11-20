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
import com.example.util.Local3DAssetResolver
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
    val city: String = "",
    val featured: Boolean = false,
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
            city = city,
            status = "published",
            featured = if (isHidden || allExpiredNow) false else (featured || tier == ProfileTier.VIP_16_9),
            uploadedBy = "justus_admin",
            uploaderName = title,
            tags = tags,
            filterTags = filterTags,
            advanceBookingAmount = advanceBookingAmount,
            bookingPlans = bookingPlans,
            videoCallEnabled = videoCallEnabled,
            voiceCallPricePerMin = voiceCallPricePerMin,
            videoCallPricePerMin = videoCallPricePerMin,
            chatPricePerMin = chatPricePerMin,
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
    private val key = stringPreferencesKey("device_profiles_3d_v3_json")

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
        filterTags: List<String>? = null,
        advanceBookingAmount: Int? = null,
        bookingPlans: List<String>? = null,
        videoCallEnabled: Boolean? = null,
        voiceCallPricePerMin: Int? = null,
        videoCallPricePerMin: Int? = null,
        chatPricePerMin: Int? = null,
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
                        filterTags = filterTags ?: item.filterTags,
                        advanceBookingAmount = advanceBookingAmount ?: item.advanceBookingAmount,
                        bookingPlans = bookingPlans ?: item.bookingPlans,
                        videoCallEnabled = videoCallEnabled ?: item.videoCallEnabled,
                        voiceCallPricePerMin = voiceCallPricePerMin?.coerceIn(0, 100_000) ?: item.voiceCallPricePerMin,
                        videoCallPricePerMin = videoCallPricePerMin?.coerceIn(0, 100_000) ?: item.videoCallPricePerMin,
                        chatPricePerMin = chatPricePerMin?.coerceIn(0, 100_000) ?: item.chatPricePerMin,
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
        if (raw == null) return defaultCuratedCompanions()
        if (raw.isBlank()) return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(defaultCuratedCompanions())
    }

    companion object {
        /**
         * Curated 3D character profiles for the 2x2 Home Grid (minimum 4+ profiles, zero prototype stock photos).
         * Notice: 0 default VIP 16:9 items so the VIP Spotlight carousel ONLY renders when uploaded from Admin Panel.
         */
        fun defaultCuratedCompanions(): List<StoredTestUpload> {
            val baseTime = 1731888000000L
            return listOf(
                StoredTestUpload(
                    id = "dm_3d_aanya",
                    title = "Aanya Sharma",
                    category = "Coffee & Chat",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_AANYA,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_AANYA,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_AANYA),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 6_000L,
                    description = "Architect & specialty coffee enthusiast in Mumbai. Loves rooftop jazz, sunset gallery walks, and deep conversations.",
                    price = "₹299",
                    age = 23,
                    role = "GF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Mumbai",
                    featured = false,
                    tags = listOf("Mumbai", "Bandra", "Budget", "Verified", "98% Match"),
                    filterTags = listOf("Coffee", "Dinner", "Mumbai", "Under ₹299", "GF", "Verified"),
                    advanceBookingAmount = 250,
                    bookingPlans = listOf("Coffee|₹299", "Dinner|₹1,499", "Night|₹2,999"),
                    isHidden = false,
                ),
                StoredTestUpload(
                    id = "dm_3d_kiara",
                    title = "Kiara Malhotra",
                    category = "Fine Dining",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_KIARA,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_KIARA,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_KIARA),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 5_000L,
                    description = "Fashion stylist & luxury evening companion in Delhi NCR. Always up for fine dining, artisanal desserts, and acoustic nights.",
                    price = "₹499",
                    age = 24,
                    role = "GF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Delhi NCR",
                    featured = false,
                    tags = listOf("Delhi NCR", "Gurgaon", "Premium", "Verified", "97% Match"),
                    filterTags = listOf("Dinner", "Night", "Delhi NCR", "Premium", "GF", "Verified"),
                    advanceBookingAmount = 500,
                    bookingPlans = listOf("Dinner|₹2,499", "Night|₹4,499", "VIP Event|₹5,999"),
                    isHidden = false,
                ),
                StoredTestUpload(
                    id = "dm_3d_kabir",
                    title = "Kabir Singhania",
                    category = "Live Music",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_KABIR,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_KABIR,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_KABIR),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 4_000L,
                    description = "Indie guitarist & fitness partner in Bengaluru. Great listener for late-evening walks, concerts, and weekend drives.",
                    price = "₹349",
                    age = 25,
                    role = "BF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Bengaluru",
                    featured = false,
                    tags = listOf("Bengaluru", "Indiranagar", "Budget", "Verified", "96% Match"),
                    filterTags = listOf("Coffee", "Movie", "Bengaluru", "Under ₹499", "BF", "Verified"),
                    advanceBookingAmount = 300,
                    bookingPlans = listOf("Coffee|₹349", "Movie|₹899", "Dinner|₹1,799"),
                    isHidden = false,
                ),
                StoredTestUpload(
                    id = "dm_3d_riya",
                    title = "Riya Verma",
                    category = "Travel Buddy",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_RIYA,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_RIYA,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_RIYA),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 3_000L,
                    description = "Psychology graduate & book cafe lover in Pune. Warm conversations, weekend road trips, and cozy cinema companion.",
                    price = "₹199",
                    age = 22,
                    role = "GF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Pune",
                    featured = false,
                    tags = listOf("Pune", "Koregaon Park", "Budget", "Verified", "99% Match"),
                    filterTags = listOf("Coffee", "Travel", "Pune", "Under ₹299", "GF", "Verified"),
                    advanceBookingAmount = 200,
                    bookingPlans = listOf("Coffee|₹199", "Dinner|₹1,199", "Day Out|₹2,299"),
                    isHidden = false,
                ),
                StoredTestUpload(
                    id = "dm_3d_meera",
                    title = "Meera Kapoor",
                    category = "Art & Culture",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_MEERA,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_MEERA,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_MEERA),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 2_000L,
                    description = "Contemporary curator & watercolor artist in Jaipur. Let's explore heritage cafes, pottery studios, and live music.",
                    price = "₹449",
                    age = 23,
                    role = "GF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Jaipur",
                    featured = false,
                    tags = listOf("Jaipur", "Premium", "Verified", "95% Match"),
                    filterTags = listOf("Dinner", "Travel", "Jaipur", "Premium", "GF", "Verified"),
                    advanceBookingAmount = 400,
                    bookingPlans = listOf("Coffee|₹449", "Dinner|₹1,999", "Night|₹3,499"),
                    isHidden = false,
                ),
                StoredTestUpload(
                    id = "dm_3d_aarav",
                    title = "Aarav Khanna",
                    category = "Coffee & Chat",
                    imageUrl = Local3DAssetResolver.AVATAR_3D_KABIR,
                    thumbUrl = Local3DAssetResolver.AVATAR_3D_KABIR,
                    photoUrls = listOf(Local3DAssetResolver.AVATAR_3D_KABIR),
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime + 1_000L,
                    description = "Culinary founder & evening drive companion in Mumbai & Delhi. Great company for upscale dining and lively talks.",
                    price = "₹299",
                    age = 26,
                    role = "BF",
                    tier = ProfileTier.GRID_9_16,
                    city = "Mumbai",
                    featured = false,
                    tags = listOf("Mumbai", "Delhi", "Budget", "Verified", "94% Match"),
                    filterTags = listOf("Coffee", "Dinner", "Mumbai", "Under ₹299", "BF", "Verified"),
                    advanceBookingAmount = 299,
                    bookingPlans = listOf("Coffee|₹299", "Dinner|₹1,599", "Night|₹2,899"),
                    isHidden = false,
                ),
            )
        }
    }
}
