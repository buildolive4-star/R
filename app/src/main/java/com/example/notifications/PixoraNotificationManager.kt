package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PixoraNotificationManager {
    const val CHANNEL_ID = "justus_live_profiles_v2"
    const val CHANNEL_MESSAGES_ID = "justus_chat_messages_v1"
    const val CHANNEL_CALLS_ID = "justus_voice_calls_v1"

    const val EXTRA_IMAGE_ID = "extra_justus_profile_id"
    const val EXTRA_CHAT_ID = "extra_justus_chat_id"
    const val EXTRA_CHAT_ROLE = "extra_justus_chat_role"
    const val EXTRA_CALL_ID = "extra_justus_call_id"

    private const val TAG = "JustUsNotify"

    private val CHAT_VIBRATION = longArrayOf(0L, 140L, 80L, 180L)
    private val CALL_VIBRATION = longArrayOf(0L, 500L, 350L, 500L, 350L)

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifySound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifyAttrs = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                        enableLights(true)
                        lightColor = Color.parseColor("#FF6B9D")
                        enableVibration(true)
                        vibrationPattern = CHAT_VIBRATION
                        setSound(notifySound, notifyAttrs)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(true)
                    }
                )
            }

            if (manager.getNotificationChannel(CHANNEL_MESSAGES_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_MESSAGES_ID,
                        "JustUs Chat Messages",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Instant heads-up alerts for new companion replies and client messages"
                        enableLights(true)
                        lightColor = Color.parseColor("#25D366")
                        enableVibration(true)
                        vibrationPattern = CHAT_VIBRATION
                        setSound(notifySound, notifyAttrs)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(true)
                    }
                )
            }

            if (manager.getNotificationChannel(CHANNEL_CALLS_ID) == null) {
                val ringtoneSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                val ringtoneAttrs = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_CALLS_ID,
                        "JustUs Voice Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Incoming WebRTC audio call alerts"
                        enableLights(true)
                        lightColor = Color.parseColor("#F6C478")
                        enableVibration(true)
                        vibrationPattern = CALL_VIBRATION
                        setSound(ringtoneSound, ringtoneAttrs)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                )
            }
        }
    }

    suspend fun notifyNewImage(context: Context, image: GalleryImage) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping system alert.")
            return
        }

        val bitmap = if (image.isHidden) {
            null
        } else {
            fetchNotificationBitmap(context, image.displayThumbnailUrl.ifBlank { image.imageUrl })
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_IMAGE_ID, image.id)
        }
        val notificationId = ("img_" + image.id).hashCode().let { if (it == 0) System.currentTimeMillis().toInt() else it }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (image.isHidden) {
            context.getString(R.string.hidden_photo_banner_title)
        } else {
            context.getString(R.string.new_photo_banner_title, image.category)
        }
        val body = if (image.isHidden) {
            "${image.title} · ${image.displayPrice} · Tap to view"
        } else {
            "${image.title} (${image.age}) · ${image.displayPrice}"
        }
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pixora)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(
                if (image.isHidden) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC
            )
            .setSound(soundUri)
            .setVibrate(CHAT_VIBRATION)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
                    .setBigContentTitle(title)
                    .setSummaryText(body)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post image notification due to missing permission", e)
        }
    }

    suspend fun notifyNewChatMessage(
        context: Context,
        chatId: String,
        recipientRole: String, // "client", "staff", "admin"
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping chat notification.")
            return
        }

        val bitmap = fetchNotificationBitmap(context, avatarUrl)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_CHAT_ROLE, recipientRole)
        }
        val notificationId = ("chat_" + chatId + "_" + recipientRole).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (recipientRole) {
            "admin" -> "Client Inquiry · $senderName"
            "staff" -> "Client Message · $senderName"
            else -> "New Reply · $senderName"
        }
        val body = messageText.ifBlank { "Tap to open conversation" }
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_notification_pixora)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(CHAT_VIBRATION)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post chat notification due to missing permission", e)
        }
    }

    fun notifyIncomingCall(
        context: Context,
        callId: String,
        callerName: String,
        profileName: String,
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CALL_ID, callId)
        }
        val notificationId = ("call_" + callId).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(context, CHANNEL_CALLS_ID)
            .setSmallIcon(R.drawable.ic_notification_pixora)
            .setContentTitle("Incoming Voice Call · $callerName")
            .setContentText(if (profileName.isNotBlank()) "Regarding $profileName · Tap to Answer" else "Tap to Answer")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(ringtone)
            .setVibrate(CALL_VIBRATION)
            .setOngoing(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancelCallNotification(context: Context, callId: String) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(("call_" + callId).hashCode())
        }
    }

    private suspend fun fetchNotificationBitmap(context: Context, url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext null
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(720)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                (result as? SuccessResult)?.drawable?.toBitmap()
            }.getOrNull()
        }
}
