package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GalleryImage
import com.google.firebase.Timestamp
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.testUploadsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_test_uploads")

@JsonClass(generateAdapter = true)
data class StoredTestUpload(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val thumbUrl: String,
    val aspectRatio: Double,
    val createdAtEpochMs: Long,
    val description: String = "",
    val price: String = "",
    val featured: Boolean = true,
    val tags: List<String> = emptyList(),
    val isHidden: Boolean = false,
    val revealExpiresAtEpochMs: Long = 0L,
    val isExpired: Boolean = false,
    val refreshEpochMs: Long = 0L,
) {
    fun toGalleryImage(nowMs: Long = System.currentTimeMillis()): GalleryImage {
        val expiredNow = isExpired || (isHidden && revealExpiresAtEpochMs > 0L && nowMs >= revealExpiresAtEpochMs)
        return GalleryImage(
            id = id,
            title = title,
            description = description.ifBlank {
                when {
                    expiredNow -> "Expired after 20s · Blurred memory"
                    isHidden -> "Private 20s disappearing drop"
                    else -> "Shared by Aria · Companion"
                }
            },
            category = category,
            imageUrl = imageUrl,
            thumbUrl = thumbUrl,
            aspectRatio = aspectRatio,
            price = price,
            status = "published",
            featured = if (isHidden || expiredNow) false else featured,
            uploadedBy = "pixora_admin",
            uploaderName = "Aria · Companion",
            tags = tags.ifEmpty { listOf("companion", category.lowercase()) },
            isHidden = isHidden,
            revealExpiresAtEpochMs = revealExpiresAtEpochMs,
            isExpired = expiredNow,
            refreshEpochMs = refreshEpochMs,
            createdAt = Timestamp(createdAtEpochMs / 1000L, 0),
            updatedAt = Timestamp(createdAtEpochMs / 1000L, 0),
        )
    }
}

class LocalTestUploadsRepository(private val context: Context) {
    private val key = stringPreferencesKey("test_uploads_json")
    private val starterSeededKey = booleanPreferencesKey("starter_seeded_v2")

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

    suspend fun ensureStarterMomentsSeeded(packageName: String) {
        context.testUploadsDataStore.edit { prefs ->
            if (prefs[starterSeededKey] == true) return@edit
            val current = decodeList(prefs[key])
            if (current.size >= 4) {
                prefs[starterSeededKey] = true
                return@edit
            }
            val baseTime = System.currentTimeMillis()
            val starters = listOf(
                StoredTestUpload(
                    id = "starter_date_night",
                    title = "Candlelit Rooftop Date",
                    category = "Date Night",
                    imageUrl = "android.resource://$packageName/drawable/img_moment_date",
                    thumbUrl = "android.resource://$packageName/drawable/img_moment_date",
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime - 1000L,
                    description = "Warm twilight dinner lights just for us tonight.",
                    price = "₹299",
                    featured = true,
                    tags = listOf("datenight", "rooftop", "evening"),
                ),
                StoredTestUpload(
                    id = "starter_morning_latte",
                    title = "Golden Hour Latte",
                    category = "Morning",
                    imageUrl = "android.resource://$packageName/drawable/img_moment_selfie",
                    thumbUrl = "android.resource://$packageName/drawable/img_moment_selfie",
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime - 2000L,
                    description = "Morning coffee mood by the sunlit window.",
                    price = "₹199",
                    featured = true,
                    tags = listOf("morning", "coffee", "candid"),
                ),
                StoredTestUpload(
                    id = "starter_twilight_balcony",
                    title = "Twilight Balcony Mood",
                    category = "Outfits",
                    imageUrl = "android.resource://$packageName/drawable/img_moment_velvet",
                    thumbUrl = "android.resource://$packageName/drawable/img_moment_velvet",
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime - 3000L,
                    description = "City skyline lights in my favorite lavender coat.",
                    price = "₹399",
                    featured = false,
                    tags = listOf("outfits", "twilight", "balcony"),
                ),
                StoredTestUpload(
                    id = "starter_secret_whisper",
                    title = "Secret 20s Snap",
                    category = "Exclusive",
                    imageUrl = "android.resource://$packageName/drawable/img_companion_avatar",
                    thumbUrl = "android.resource://$packageName/drawable/img_companion_avatar",
                    aspectRatio = 9.0 / 16.0,
                    createdAtEpochMs = baseTime - 4000L,
                    description = "Tap to unlock — blurs automatically after 20 seconds.",
                    price = "₹499",
                    featured = false,
                    tags = listOf("exclusive", "secret", "20s"),
                    isHidden = true,
                ),
            )
            val existingIds = current.map { it.id }.toSet()
            val merged = current + starters.filterNot { existingIds.contains(it.id) }
            prefs[key] = adapter.toJson(merged)
            prefs[starterSeededKey] = true
        }
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
        featured: Boolean,
        tags: List<String>,
        isHidden: Boolean? = null,
    ) {
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    val nextHidden = isHidden ?: item.isHidden
                    item.copy(
                        title = title.trim().ifBlank { item.title }.take(120),
                        description = description.trim().take(600),
                        category = category.trim().ifBlank { "Date Night" }.take(40),
                        price = price.trim().take(32),
                        featured = featured,
                        tags = tags.map { it.trim().take(32) }.filter { it.isNotEmpty() }.distinct().take(10),
                        isHidden = nextHidden,
                        revealExpiresAtEpochMs = if (!nextHidden) 0L else item.revealExpiresAtEpochMs,
                        isExpired = if (!nextHidden) false else item.isExpired,
                    )
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
    }

    suspend fun startHiddenRevealCountdown(imageId: String, ttlMs: Long = 20_000L): Long {
        var expiresAt = 0L
        context.testUploadsDataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId && item.isHidden && !item.isExpired) {
                    val resolvedExpiry = if (item.revealExpiresAtEpochMs > 0L) {
                        item.revealExpiresAtEpochMs
                    } else {
                        now + ttlMs
                    }
                    expiresAt = resolvedExpiry
                    item.copy(revealExpiresAtEpochMs = resolvedExpiry)
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
        return expiresAt
    }

    suspend fun markExpired(imageId: String) {
        context.testUploadsDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).map { item ->
                if (item.id == imageId) {
                    item.copy(isExpired = true, featured = false)
                } else {
                    item
                }
            }
            prefs[key] = adapter.toJson(updated)
        }
    }

    suspend fun markExpiredUploads(nowMs: Long = System.currentTimeMillis()): List<String> {
        val newlyExpiredIds = mutableListOf<String>()
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key])
            val updated = current.map { item ->
                val shouldExpire = !item.isExpired &&
                    item.isHidden &&
                    item.revealExpiresAtEpochMs > 0L &&
                    nowMs >= item.revealExpiresAtEpochMs
                if (shouldExpire) {
                    newlyExpiredIds.add(item.id)
                    item.copy(isExpired = true, featured = false)
                } else {
                    item
                }
            }
            if (newlyExpiredIds.isNotEmpty()) {
                prefs[key] = adapter.toJson(updated)
            }
        }
        return newlyExpiredIds
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
                val needsRefresh = item.isExpired || (item.isHidden && item.revealExpiresAtEpochMs > 0L)
                if (needsRefresh) {
                    val next = item.copy(
                        isHidden = true,
                        isExpired = false,
                        revealExpiresAtEpochMs = 0L,
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
