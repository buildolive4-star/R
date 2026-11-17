package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.preferences.StoredTestUpload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

object ImgBbUploadHelper {
    private const val MAX_DIMENSION_PX = 1600
    private const val JPEG_QUALITY = 88

    suspend fun uploadUriToImgBb(
        context: Context,
        apiService: ImgBbApiService,
        imageUri: Uri,
        title: String,
        category: String,
    ): Result<StoredTestUpload> = withContext(Dispatchers.IO) {
        runCatching {
            if (!ImgBbApiService.isApiKeyConfigured()) {
                error("IMGBB_API_KEY is not configured. Add it in the AI Studio Secrets panel and rebuild.")
            }

            val (base64, ratio) = encodeUriToBase64(context, imageUri)
            val safeTitle = title.trim().ifBlank { "Studio Upload" }.take(120)
            val response = apiService.uploadBase64Image(
                apiKey = BuildConfig.IMGBB_API_KEY,
                base64Image = base64,
                name = safeTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
            )

            val data = response.data
            if (!response.success || data == null || data.displayUrl.isBlank()) {
                error("ImgBB upload failed (status ${response.status})")
            }

            val computedRatio = if (data.width > 0 && data.height > 0) {
                (data.width.toDouble() / data.height.toDouble()).coerceIn(0.4, 2.5)
            } else {
                ratio
            }

            val displayHttps = data.displayUrl.replaceFirst("http://", "https://")
            val thumbHttps = (data.thumb?.url?.takeIf { it.isNotBlank() } ?: displayHttps)
                .replaceFirst("http://", "https://")

            StoredTestUpload(
                id = "imgbb_${data.id.ifBlank { UUID.randomUUID().toString().take(8) }}",
                title = safeTitle,
                category = category.trim().ifBlank { "Photography" }.take(40),
                imageUrl = displayHttps,
                thumbUrl = thumbHttps,
                aspectRatio = computedRatio,
                createdAtEpochMs = System.currentTimeMillis(),
            )
        }
    }

    private fun encodeUriToBase64(context: Context, uri: Uri): Pair<String, Double> {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        } ?: error("Unable to read selected image")

        val rawW = boundsOpts.outWidth.coerceAtLeast(1)
        val rawH = boundsOpts.outHeight.coerceAtLeast(1)
        var sampleSize = 1
        while (rawW / sampleSize > MAX_DIMENSION_PX || rawH / sampleSize > MAX_DIMENSION_PX) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: error("Unable to decode selected image")

        val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
        val ratio = (scaled.width.toDouble() / scaled.height.toDouble()).coerceIn(0.4, 2.5)

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return encoded to ratio
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val scale = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
