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
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.example.data.model.GalleryImage
import com.example.util.Local3DAssetResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object PixoraNotificationManager {
    const val CHANNEL_ID = "datemate_system_push_profiles_v7"
    const val CHANNEL_MESSAGES_ID = "datemate_system_push_messages_v7"
    const val CHANNEL_CALLS_ID = "datemate_system_push_calls_v6"
    const val CHANNEL_BG_SYNC_ID = "datemate_live_push_monitor_v1"

    const val EXTRA_IMAGE_ID = "extra_justus_profile_id"
    const val EXTRA_CHAT_ID = "extra_justus_chat_id"
    const val EXTRA_CHAT_ROLE = "extra_justus_chat_role"
    const val EXTRA_CALL_ID = "extra_justus_call_id"

    private const val TAG = "DateMatePushNotify"

    private val pushNotifyScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastChimePlayedAtMs: Long = 0L

    private fun notificationSoundUri(context: Context): Uri =
        runCatching {
            Uri.parse("android.resource://${context.packageName}/${R.raw.justus_chime}")
        }.getOrElse {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

    fun triggerAlertVibration(context: Context) {
        val appCtx = context.applicationContext
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appCtx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appCtx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(
                        CHAT_VIBRATION,
                        intArrayOf(0, 220, 0, 255),
                        -1
                    )
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(CHAT_VIBRATION, -1)
                }
            }
        }
    }

    fun playAlertChime(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastChimePlayedAtMs < 1100L) return
        lastChimePlayedAtMs = now
        val appCtx = context.applicationContext
        // Always fire BOTH crisp vibration + notification chime together
        triggerAlertVibration(appCtx)
        runCatching {
            val uri = notificationSoundUri(appCtx)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                setDataSource(appCtx, uri)
                setOnCompletionListener { mp -> runCatching { mp.release() } }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    true
                }
                prepare()
                start()
            }
            if (!player.isPlaying) {
                runCatching { player.release() }
            }
        }.onFailure {
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(appCtx, fallbackUri)?.play()
            }
        }
    }

    private val CHAT_VIBRATION = longArrayOf(0L, 240L, 120L, 300L)
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
            val notifySound = notificationSoundUri(context)
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
                        lightColor = Color.parseColor("#E91E63")
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
                        "DateMate Chat Push Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Instant Android system push notifications for new messages and replies"
                        enableLights(true)
                        lightColor = Color.parseColor("#E91E63")
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
                        "DateMate Voice Calls",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Incoming WebRTC audio call alerts"
                        enableLights(true)
                        lightColor = Color.parseColor("#E91E63")
                        enableVibration(true)
                        vibrationPattern = CALL_VIBRATION
                        setSound(ringtoneSound, ringtoneAttrs)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    }
                )
            }

            if (manager.getNotificationChannel(CHANNEL_BG_SYNC_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_BG_SYNC_ID,
                        "DateMate Live Push Connection",
                        NotificationManager.IMPORTANCE_MIN
                    ).apply {
                        description = "Keeps real-time message push notifications active outside the app"
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_SECRET
                    }
                )
            }
        }
    }

    fun postNewImagePushAsync(context: Context, image: GalleryImage) {
        val appCtx = context.applicationContext
        pushNotifyScope.launch {
            notifyNewImageInternal(appCtx, image)
        }
    }

    suspend fun notifyNewImage(context: Context, image: GalleryImage) {
        val appCtx = context.applicationContext
        pushNotifyScope.launch {
            notifyNewImageInternal(appCtx, image)
        }
    }

    private suspend fun notifyNewImageInternal(context: Context, image: GalleryImage) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping system alert.")
            return
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        val soundUri = notificationSoundUri(context)

        val bitmap = if (image.isHidden) {
            null
        } else {
            withTimeoutOrNull(900L) {
                fetchNotificationBitmap(context, image.displayThumbnailUrl.ifBlank { image.imageUrl })
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pixora)
            .setColor(Color.parseColor("#E91E63"))
            .setContentTitle(title)
            .setContentText(body)
            .setTicker("$title: $body")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(
                if (image.isHidden) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC
            )
            .setSound(soundUri)
            .setVibrate(CHAT_VIBRATION)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification_pixora,
                "View Profile",
                pendingIntent
            )

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
            val nm = NotificationManagerCompat.from(context)
            nm.cancel(notificationId)
            nm.notify(notificationId, builder.build())
            playAlertChime(context)
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post image notification due to missing permission", e)
        }
    }

    fun postChatPushAsync(
        context: Context,
        chatId: String,
        recipientRole: String,
        senderName: String,
        messageText: String,
        avatarUrl: String = "",
    ) {
        val appCtx = context.applicationContext
        pushNotifyScope.launch {
            notifyNewChatMessageInternal(
                context = appCtx,
                chatId = chatId,
                recipientRole = recipientRole,
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
            )
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
        val appCtx = context.applicationContext
        pushNotifyScope.launch {
            notifyNewChatMessageInternal(
                context = appCtx,
                chatId = chatId,
                recipientRole = recipientRole,
                senderName = senderName,
                messageText = messageText,
                avatarUrl = avatarUrl,
            )
        }
    }

    private suspend fun notifyNewChatMessageInternal(
        context: Context,
        chatId: String,
        recipientRole: String,
        senderName: String,
        messageText: String,
        avatarUrl: String,
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted; skipping chat notification.")
            return
        }

        val bitmap = withTimeoutOrNull(850L) {
            fetchNotificationBitmap(context, avatarUrl)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

        val roleLabel = when (recipientRole) {
            "admin" -> "DateMate Admin Inbox"
            "staff" -> "DateMate Companion Inbox"
            else -> "DateMate Direct Message"
        }
        val cleanSender = senderName.ifBlank { "DateMate Companion" }
        val title = when (recipientRole) {
            "admin" -> "New Client Message · $cleanSender"
            "staff" -> "Client Message · $cleanSender"
            else -> "New Reply · $cleanSender"
        }
        val body = messageText.ifBlank { "Sent you a new message · Tap to open chat" }
        val soundUri = notificationSoundUri(context)
        val nowMs = System.currentTimeMillis()

        val senderPersonBuilder = Person.Builder()
            .setName(cleanSender)
            .setKey("${chatId}_$recipientRole")
            .setImportant(true)
        if (bitmap != null) {
            senderPersonBuilder.setIcon(IconCompat.createWithBitmap(bitmap))
        }
        val senderPerson = senderPersonBuilder.build()
        val mePerson = Person.Builder().setName("You").build()

        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)
            .setConversationTitle(roleLabel)
            .addMessage(body, nowMs, senderPerson)

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES_ID)
            .setSmallIcon(R.drawable.ic_notification_pixora)
            .setColor(Color.parseColor("#E91E63"))
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(roleLabel)
            .setTicker("$cleanSender: $body")
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(CHAT_VIBRATION)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setWhen(nowMs)
            .setShowWhen(true)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_notification_pixora,
                "Reply in DateMate",
                pendingIntent
            )

        if (bitmap != null) {
            builder.setLargeIcon(bitmap)
        }

        try {
            val nm = NotificationManagerCompat.from(context)
            // Cancel prior notification ID first so Android OS always triggers a fresh Heads-Up Push Pop-up outside & at top of phone
            nm.cancel(notificationId)
            nm.notify(notificationId, builder.build())
            playAlertChime(context)
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            .setColor(Color.parseColor("#E91E63"))
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
                val resolvedModel = Local3DAssetResolver.resolveCoilModel(context, url)
                val request = ImageRequest.Builder(context)
                    .data(resolvedModel)
                    .size(512)
                    .allowHardware(false)
                    .build()
                val result = context.imageLoader.execute(request)
                (result as? SuccessResult)?.drawable?.toBitmap()
            }.getOrNull()
        }
}
