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
) {
    fun toGalleryImage(): GalleryImage = GalleryImage(
        id = id,
        title = title,
        description = "Uploaded via ImgBB CDN test",
        category = category,
        imageUrl = imageUrl,
        thumbUrl = thumbUrl,
        aspectRatio = aspectRatio,
        status = "published",
        featured = true,
        uploadedBy = "imgbb_test",
        uploaderName = "ImgBB Test",
        tags = listOf("imgbb", category.lowercase()),
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
        decodeList(prefs[key])
            .sortedByDescending { it.createdAtEpochMs }
            .map { it.toGalleryImage() }
    }

    suspend fun addUpload(upload: StoredTestUpload) {
        context.testUploadsDataStore.edit { prefs ->
            val current = decodeList(prefs[key]).filterNot { it.id == upload.id }
            prefs[key] = adapter.toJson(listOf(upload) + current)
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
