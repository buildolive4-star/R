package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
    val featured: Boolean = true,
    val tags: List<String> = emptyList(),
    val isHidden: Boolean = false,
    val revealExpiresAtEpochMs: Long = 0L,
) {
    fun toGalleryImage(): GalleryImage = GalleryImage(
        id = id,
        title = title,
        description = description.ifBlank {
            if (isHidden) "Protected disappearing photo" else "Uploaded via ImgBB CDN"
        },
        category = category,
        imageUrl = imageUrl,
        thumbUrl = thumbUrl,
        aspectRatio = aspectRatio,
        status = "published",
        featured = if (isHidden) false else featured,
        uploadedBy = "pixora_admin",
        uploaderName = "Pixora Studio",
        tags = tags.ifEmpty { listOf("imgbb", category.lowercase()) },
        isHidden = isHidden,
        revealExpiresAtEpochMs = revealExpiresAtEpochMs,
        createdAt = Timestamp(createdAtEpochMs / 1000L, 0),
        updatedAt = Timestamp(createdAtEpochMs / 1000L, 0),
    )
}

class LocalTestUploadsRepository(private val context: Context) {
    private val key = stringPreferencesKey("test_uploads_json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val listType = Types.newParameterizedType(List::class.java, StoredTestUpload::class.java)
    private val adapter = moshi.adapter<List<StoredTestUpload>>(listType)

    val testUploads: Flow<List<GalleryImage>> = context.testUploadsDataStore.data.map { prefs ->
        val now = System.currentTimeMillis()
        decodeList(prefs[key])
            .filterNot { it.isHidden && it.revealExpiresAtEpochMs > 0L && now >= it.revealExpiresAtEpochMs }
            .sortedByDescending { it.createdAtEpochMs }
            .map { it.toGalleryImage() }
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
                        category = category.trim().ifBlank { "Photography" }.take(40),
                        featured = featured,
                        tags = tags.map { it.trim().take(32) }.filter { it.isNotEmpty() }.distinct().take(10),
                        isHidden = nextHidden,
                        revealExpiresAtEpochMs = if (!nextHidden) 0L else item.revealExpiresAtEpochMs,
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
                if (item.id == imageId && item.isHidden) {
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

    suspend fun purgeExpiredUploads(nowMs: Long = System.currentTimeMillis()): List<String> {
        val removedIds = mutableListOf<String>()
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key])
            val kept = current.filter { item ->
                val expired = item.isHidden && item.revealExpiresAtEpochMs > 0L && nowMs >= item.revealExpiresAtEpochMs
                if (expired) removedIds.add(item.id)
                !expired
            }
            if (removedIds.isNotEmpty()) {
                prefs[key] = adapter.toJson(kept)
            }
        }
        return removedIds
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
