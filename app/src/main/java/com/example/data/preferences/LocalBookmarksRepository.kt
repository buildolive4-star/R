package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.GalleryImage
import com.example.data.model.SavedBookmark
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookmarksDataStore: DataStore<Preferences> by preferencesDataStore(name = "pixora_bookmarks")

class LocalBookmarksRepository(private val context: Context) {
    private val key = stringPreferencesKey("saved_bookmarks_json")

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, SavedBookmark::class.java)
    private val adapter = moshi.adapter<List<SavedBookmark>>(listType)

    val bookmarks: Flow<List<SavedBookmark>> = context.bookmarksDataStore.data.map { prefs ->
        val raw = prefs[key].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            runCatching { adapter.fromJson(raw).orEmpty() }
                .getOrDefault(emptyList())
                .sortedByDescending { it.savedAtEpochMs }
        }
    }

    suspend fun toggleBookmark(image: GalleryImage): Boolean {
        var nowSaved = false
        context.bookmarksDataStore.edit { prefs ->
            val current = decodeList(prefs[key])
            val exists = current.any { it.imageId == image.id }
            val updated = if (exists) {
                nowSaved = false
                current.filterNot { it.imageId == image.id }
            } else {
                nowSaved = true
                val entry = SavedBookmark(
                    imageId = image.id,
                    title = image.title.ifBlank { "Untitled Artwork" },
                    description = image.description,
                    category = image.category.ifBlank { "Photography" },
                    imageUrl = image.imageUrl,
                    thumbUrl = image.thumbUrl,
                    aspectRatio = image.aspectRatio.coerceIn(0.3, 3.5),
                    uploaderName = image.uploaderName,
                    tags = image.tags,
                    savedAtEpochMs = System.currentTimeMillis(),
                )
                listOf(entry) + current
            }
            prefs[key] = adapter.toJson(updated)
        }
        return nowSaved
    }

    suspend fun removeBookmark(imageId: String) {
        context.bookmarksDataStore.edit { prefs ->
            val updated = decodeList(prefs[key]).filterNot { it.imageId == imageId }
            prefs[key] = adapter.toJson(updated)
        }
    }

    suspend fun clearAll() {
        context.bookmarksDataStore.edit { prefs ->
            prefs[key] = "[]"
        }
    }

    private fun decodeList(raw: String?): List<SavedBookmark> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }
}
