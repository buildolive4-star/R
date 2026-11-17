package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ImgBbUploadResponse(
    @param:Json(name = "data") val data: ImgBbData? = null,
    @param:Json(name = "success") val success: Boolean = false,
    @param:Json(name = "status") val status: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ImgBbData(
    @param:Json(name = "id") val id: String = "",
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "url_viewer") val urlViewer: String = "",
    @param:Json(name = "url") val url: String = "",
    @param:Json(name = "display_url") val displayUrl: String = "",
    @param:Json(name = "width") val width: Int = 0,
    @param:Json(name = "height") val height: Int = 0,
    @param:Json(name = "size") val size: Long = 0L,
    @param:Json(name = "thumb") val thumb: ImgBbImageAsset? = null,
    @param:Json(name = "medium") val medium: ImgBbImageAsset? = null,
    @param:Json(name = "delete_url") val deleteUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ImgBbImageAsset(
    @param:Json(name = "url") val url: String = "",
)

/**
 * Retrofit service for the ImgBB image hosting API (`https://api.imgbb.com/1/upload`).
 * Configured via `BuildConfig.IMGBB_API_KEY` (injected from AI Studio Secrets).
 * Ready for Admin Panel upload workflows while keeping the User Panel cleanly separated.
 */
interface ImgBbApiService {
    @FormUrlEncoded
    @POST("1/upload")
    suspend fun uploadBase64Image(
        @Query("key") apiKey: String = BuildConfig.IMGBB_API_KEY,
        @Field("image") base64Image: String,
        @Field("name") name: String? = null,
    ): ImgBbUploadResponse

    companion object {
        private const val BASE_URL = "https://api.imgbb.com/"

        fun isApiKeyConfigured(): Boolean {
            val key = BuildConfig.IMGBB_API_KEY
            return key.isNotBlank() && key != "MY_IMGBB_API_KEY"
        }

        fun create(): ImgBbApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ImgBbApiService::class.java)
        }
    }
}
