package com.example.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.R

/**
 * Resolves bundled 3D Pixar-style character avatar tokens (e.g. "drawable://3d_aanya")
 * into Android drawable resource IDs so Coil renders crisp local 3D renders across
 * cards, profile details, chat headers, and system push notifications without stock photos.
 */
object Local3DAssetResolver {
    const val AVATAR_3D_AANYA = "drawable://3d_aanya"
    const val AVATAR_3D_KIARA = "drawable://3d_kiara"
    const val AVATAR_3D_KABIR = "drawable://3d_kabir"
    const val AVATAR_3D_RIYA = "drawable://3d_riya"
    const val AVATAR_3D_MEERA = "drawable://3d_meera"

    val ALL_3D_AVATAR_RES_IDS: List<Int> = listOf(
        R.drawable.img_3d_avatar_aanya_1795064941665,
        R.drawable.img_3d_avatar_kiara_1795064956175,
        R.drawable.img_3d_avatar_kabir_1795064969604,
        R.drawable.img_3d_avatar_riya_1795064983471,
        R.drawable.img_3d_avatar_meera_1795064997727,
    )

    @DrawableRes
    fun resolveDrawableResOrNull(rawUrl: String?): Int? {
        val clean = rawUrl?.trim().orEmpty()
        return when (clean) {
            AVATAR_3D_AANYA -> R.drawable.img_3d_avatar_aanya_1795064941665
            AVATAR_3D_KIARA -> R.drawable.img_3d_avatar_kiara_1795064956175
            AVATAR_3D_KABIR -> R.drawable.img_3d_avatar_kabir_1795064969604
            AVATAR_3D_RIYA -> R.drawable.img_3d_avatar_riya_1795064983471
            AVATAR_3D_MEERA -> R.drawable.img_3d_avatar_meera_1795064997727
            else -> null
        }
    }

    fun resolveCoilModel(context: Context, rawUrl: String?): Any {
        val resId = resolveDrawableResOrNull(rawUrl)
        if (resId != null) return resId
        return rawUrl.orEmpty()
    }
}
