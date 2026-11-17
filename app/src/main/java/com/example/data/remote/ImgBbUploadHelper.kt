package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
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
                error("IMGBB_API_KEY is not configured. Add it in Secrets and rebuild.")
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

    internal fun encodeUriToBase64(context: Context, uri: Uri): Pair<String, Double> {
        val decoded = decodeScaledBitmap(context, uri)
        val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
        val ratio = (scaled.width.toDouble() / scaled.height.toDouble()).coerceIn(0.4, 2.5)

        val out = ByteArrayOutputStream()
        val compressed = scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        if (!compressed || out.size() == 0) {
            error("Unable to compress selected image")
        }

        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return encoded to ratio
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val rawW = info.size.width.coerceAtLeast(1)
                val rawH = info.size.height.coerceAtLeast(1)
                if (rawW > MAX_DIMENSION_PX || rawH > MAX_DIMENSION_PX) {
                    val scale = minOf(
                        MAX_DIMENSION_PX.toFloat() / rawW,
                        MAX_DIMENSION_PX.toFloat() / rawH
                    )
                    decoder.setTargetSize(
                        (rawW * scale).toInt().coerceAtLeast(1),
                        (rawH * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        }

        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read selected image")
        if (bytes.isEmpty()) error("Selected image file is empty")

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
        val rawW = boundsOpts.outWidth
        val rawH = boundsOpts.outHeight
        if (rawW <= 0 || rawH <= 0) error("Unsupported or corrupted image format")

        var sampleSize = 1
        while (rawW / sampleSize > MAX_DIMENSION_PX || rawH / sampleSize > MAX_DIMENSION_PX) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: error("Unable to decode selected image")
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
