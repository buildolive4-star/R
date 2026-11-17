package com.example.util

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object ImageActions {
    private val httpClient by lazy { OkHttpClient() }

    fun shareImage(context: Context, title: String, imageUrl: String) {
        val shareText = buildString {
            if (title.isNotBlank()) append(title).append("\n")
            if (!imageUrl.startsWith("android.resource://")) {
                append(imageUrl)
            }
        }.trim()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Pixora Moment" })
            putExtra(Intent.EXTRA_TEXT, shareText.ifBlank { "Shared from Pixora" })
        }
        context.startActivity(Intent.createChooser(intent, "Share photo"))
    }

    suspend fun saveImageToGallery(context: Context, image: GalleryImage): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val safeName = image.title
                    .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                    .take(40)
                    .ifBlank { "pixora_photo" }
                val fileName = "${safeName}_${System.currentTimeMillis()}.jpg"
                val isLocalResource = image.imageUrl.startsWith("android.resource://")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || isLocalResource) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                "${Environment.DIRECTORY_PICTURES}/Pixora"
                            )
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    ) ?: error("Unable to create MediaStore record")

                    try {
                        if (isLocalResource) {
                            resolver.openInputStream(Uri.parse(image.imageUrl))?.use { input ->
                                resolver.openOutputStream(uri)?.use { out ->
                                    input.copyTo(out)
                                } ?: error("Unable to open MediaStore output stream")
                            } ?: error("Unable to read bundled photo")
                        } else {
                            val request = Request.Builder().url(image.imageUrl).build()
                            httpClient.newCall(request).execute().use { response ->
                                if (!response.isSuccessful) {
                                    error("HTTP ${response.code} downloading image")
                                }
                                val body = response.body ?: error("Empty response body")
                                resolver.openOutputStream(uri)?.use { out ->
                                    body.byteStream().copyTo(out)
                                } ?: error("Unable to open MediaStore output stream")
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            values.clear()
                            values.put(MediaStore.Images.Media.IS_PENDING, 0)
                            resolver.update(uri, values, null, null)
                        }
                    } catch (t: Throwable) {
                        resolver.delete(uri, null, null)
                        throw t
                    }
                } else {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val req = DownloadManager.Request(Uri.parse(image.imageUrl))
                        .setTitle(image.title.ifBlank { "Pixora Photo" })
                        .setDescription("Saving image from Pixora")
                        .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_PICTURES,
                            "Pixora/$fileName"
                        )
                    dm.enqueue(req)
                }
                Unit
            }
        }
}
