package com.example.notifications

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

object OneSignalPushManager {
    private const val TAG = "JustUsOneSignal"
    private const val PLACEHOLDER_APP_ID = "MY_ONESIGNAL_APP_ID"
    private const val PLACEHOLDER_REST_KEY = "MY_ONESIGNAL_REST_API_KEY"
    private const val ENDPOINT = "https://api.onesignal.com/notifications?c=push"

    private val UUID_REGEX = Regex("^[0-9a-fA-F-]{32,36}$")
    private val pushScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private data class PendingIdentity(
        val uid: String?,
        val isAdmin: Boolean,
        val isApprovedStaff: Boolean,
    )

    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    private var initializedAppId: String? = null

    @Volatile
    private var lastSyncedUid: String? = null

    @Volatile
    private var lastSyncedRoleKey: String? = null

    @Volatile
    private var pendingIdentity: PendingIdentity? = null

    @Volatile
    private var appContextRef: Context? = null

    private val probeInFlight = AtomicBoolean(false)

    @Volatile
    private var fcmProbeSkippedOrFailed: Boolean = false

    private val _deepLinkChat = MutableStateFlow<Pair<String, String>?>(null)
    val deepLinkChat: StateFlow<Pair<String, String>?> = _deepLinkChat.asStateFlow()

    private val _deepLinkImageId = MutableStateFlow<String?>(null)
    val deepLinkImageId: StateFlow<String?> = _deepLinkImageId.asStateFlow()

    val appId: String
        get() = BuildConfig.ONESIGNAL_APP_ID.trim()

    val restApiKey: String
        get() = BuildConfig.ONESIGNAL_REST_API_KEY.trim()

    fun isAppIdConfigured(): Boolean {
        val id = appId
        return id.isNotBlank() && id != PLACEHOLDER_APP_ID && UUID_REGEX.matches(id)
    }

    fun isRestKeyConfigured(): Boolean {
        val key = restApiKey
        return key.isNotBlank() && key != PLACEHOLDER_REST_KEY && key.length >= 16
    }

    fun isFullyConfigured(): Boolean = isAppIdConfigured() && isRestKeyConfigured()

    val isInitialized: Boolean
        get() = initializedAppId != null

    fun consumeDeepLinkChat() {
        _deepLinkChat.value = null
    }

    fun consumeDeepLinkImage() {
        _deepLinkImageId.value = null
    }

    fun init(applicationContext: Context) {
        val appCtx = applicationContext.applicationContext ?: applicationContext
        appContextRef = appCtx

        if (!isAppIdConfigured()) {
            Log.i(TAG, "OneSignal App ID not configured yet; using local + WorkManager alerts.")
            return
        }
        val currentId = appId
        if (initializedAppId == currentId) return

        // Robolectric / environments without FirebaseApp initialized cannot use FCM
        val hasFirebase = runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)
        if (!hasFirebase) {
            Log.i(TAG, "FirebaseApp unavailable in this runtime; skipping OneSignal FCM init.")
            return
        }

        val gmsAvailable = runCatching {
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appCtx) == ConnectionResult.SUCCESS
        }.getOrDefault(false)
        if (!gmsAvailable) {
            Log.i(TAG, "Google Play Services unavailable; using live Firestore + WorkManager alerts.")
            return
        }

        val hasPlayStore = isPlayStoreInstalled(appCtx)
        val emulator = isLikelyEmulator()

        // On physical devices with Google Play Services + Play Store, initialize OneSignal immediately
        // so cold-start notification clicks are never missed.
        if (!emulator && hasPlayStore) {
            initializeSdkNow(appCtx, currentId)
            return
        }

        // On emulators or non-Play-Store devices (e.g. cloud/AOSP emulators where FCM check-in is absent),
        // pre-flight probe FCM token retrieval safely first so OneSignal does not spam E/OneSignal errors.
        if (fcmProbeSkippedOrFailed || !probeInFlight.compareAndSet(false, true)) {
            return
        }
        pushScope.launch {
            try {
                val fcmReady = probeFcmTokenAvailable()
                if (fcmReady) {
                    withContext(Dispatchers.Main) {
                        initializeSdkNow(appCtx, currentId)
                    }
                } else {
                    fcmProbeSkippedOrFailed = true
                    Log.i(TAG, "FCM registration unavailable in this emulator/device; using live Firestore + WorkManager alerts.")
                }
            } finally {
                probeInFlight.set(false)
            }
        }
    }

    private fun initializeSdkNow(appCtx: Context, currentId: String) {
        if (initializedAppId == currentId) return
        runCatching {
            OneSignal.Debug.logLevel = LogLevel.WARN
            OneSignal.initWithContext(appCtx, currentId)
            initializedAppId = currentId

            OneSignal.Notifications.addClickListener(object : INotificationClickListener {
                override fun onClick(event: INotificationClickEvent) {
                    val data = event.notification.additionalData ?: return
                    val type = data.optString("type")
                    when (type) {
                        "chat", "call" -> {
                            val chatId = data.optString("chat_id")
                            val role = data.optString("chat_role", "client")
                            if (chatId.isNotBlank()) {
                                _deepLinkChat.value = chatId to role
                            }
                        }
                        "profile" -> {
                            val imageId = data.optString("image_id")
                            if (imageId.isNotBlank()) {
                                _deepLinkImageId.value = imageId
                            }
                        }
                    }
                }
            })

            OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
                override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                    // When the app is actively open, MainActivity's live Firestore listeners
                    // handle heads-up alerts + in-app banners cleanly without duplicate sound.
                    if (isAppInForeground) {
                        event.preventDefault()
                    }
                }
            })

            pendingIdentity?.let { queued ->
                syncUserIdentity(queued.uid, queued.isAdmin, queued.isApprovedStaff)
            }
        }.onFailure { err ->
            Log.w(TAG, "OneSignal initialization skipped/failed safely", err)
        }
    }

    private suspend fun probeFcmTokenAvailable(): Boolean {
        return withTimeoutOrNull(6_000L) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    FirebaseMessaging.getInstance().getToken().addOnCompleteListener { task ->
                        if (cont.isActive) {
                            val ok = task.isSuccessful && !task.result.isNullOrBlank()
                            cont.resume(ok)
                        }
                    }
                }.onFailure {
                    if (cont.isActive) cont.resume(false)
                }
            }
        } ?: false
    }

    private fun isPlayStoreInstalled(context: Context): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo("com.android.vending", PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.android.vending", 0)
            }
            true
        }.getOrDefault(false)
    }

    private fun isLikelyEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.FINGERPRINT.contains("robolectric") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator") ||
            Build.PRODUCT.contains("vbox86")
    }

    fun syncUserIdentity(
        uid: String?,
        isAdmin: Boolean,
        isApprovedStaff: Boolean,
    ) {
        pendingIdentity = PendingIdentity(uid, isAdmin, isApprovedStaff)
        if (initializedAppId == null) {
            appContextRef?.let { ctx ->
                if (!fcmProbeSkippedOrFailed) {
                    init(ctx)
                }
            }
            return
        }
        val cleanUid = uid?.trim().orEmpty()
        if (cleanUid.isEmpty()) {
            clearIdentity()
            return
        }
        val role = when {
            isAdmin -> "admin"
            isApprovedStaff -> "staff"
            else -> "client"
        }
        val roleKey = "${cleanUid}_${role}_$isAdmin"
        if (lastSyncedUid == cleanUid && lastSyncedRoleKey == roleKey) return

        runCatching {
            if (lastSyncedUid != cleanUid) {
                OneSignal.login(cleanUid)
                lastSyncedUid = cleanUid
            }
            OneSignal.User.addTags(
                mapOf(
                    "uid" to cleanUid,
                    "role" to role,
                    "is_admin" to if (isAdmin) "true" else "false",
                )
            )
            lastSyncedRoleKey = roleKey
        }.onFailure { err ->
            Log.w(TAG, "Unable to sync OneSignal user identity", err)
        }
    }

    fun clearIdentity() {
        if (initializedAppId == null) return
        if (lastSyncedUid == null) return
        runCatching {
            OneSignal.User.addTags(
                mapOf(
                    "is_admin" to "false",
                    "role" to "signed_out",
                )
            )
            OneSignal.logout()
            lastSyncedUid = null
            lastSyncedRoleKey = null
        }.onFailure { err ->
            Log.w(TAG, "Unable to logout OneSignal identity", err)
        }
    }

    fun sendChatPushAsync(
        chatId: String,
        senderRole: String,
        senderName: String,
        clientUid: String,
        clientName: String,
        staffOwnerUid: String,
        profileName: String,
        messageText: String,
        avatarUrl: String = "",
    ) {
        if (!isFullyConfigured()) return
        val cleanMessage = messageText.trim().ifBlank { "New message received" }.take(240)
        pushScope.launch {
            if (senderRole == "client") {
                // 1. Push to all Admins (Super Admin + active Code Admins)
                val adminPayload = JSONObject().apply {
                    put("app_id", appId)
                    put(
                        "filters",
                        JSONArray().put(
                            JSONObject().apply {
                                put("field", "tag")
                                put("key", "is_admin")
                                put("relation", "=")
                                put("value", "true")
                            }
                        )
                    )
                    put("headings", JSONObject().put("en", "Client Inquiry · ${clientName.ifBlank { "Client" }}"))
                    put("contents", JSONObject().put("en", "${profileName.ifBlank { "JustUs" }}: $cleanMessage"))
                    put(
                        "data",
                        JSONObject()
                            .put("type", "chat")
                            .put("chat_id", chatId)
                            .put("chat_role", "admin")
                    )
                    if (avatarUrl.startsWith("https://")) {
                        put("large_icon", avatarUrl)
                    }
                    put("existing_android_channel_id", PixoraNotificationManager.CHANNEL_MESSAGES_ID)
                    put("priority", 10)
                    put("android_group", "justus_admin_chat")
                }
                postNotification(adminPayload)

                // 2. Also push directly to Staff Companion owner if assigned
                if (staffOwnerUid.isNotBlank() && staffOwnerUid != clientUid) {
                    val staffPayload = JSONObject().apply {
                        put("app_id", appId)
                        put("target_channel", "push")
                        put(
                            "include_aliases",
                            JSONObject().put("external_id", JSONArray().put(staffOwnerUid))
                        )
                        put("headings", JSONObject().put("en", "Client Message · ${clientName.ifBlank { "Client" }}"))
                        put("contents", JSONObject().put("en", cleanMessage))
                        put(
                            "data",
                            JSONObject()
                                .put("type", "chat")
                                .put("chat_id", chatId)
                                .put("chat_role", "staff")
                        )
                        if (avatarUrl.startsWith("https://")) {
                            put("large_icon", avatarUrl)
                        }
                        put("existing_android_channel_id", PixoraNotificationManager.CHANNEL_MESSAGES_ID)
                        put("priority", 10)
                        put("android_group", "justus_staff_chat")
                    }
                    postNotification(staffPayload)
                }
            } else if (clientUid.isNotBlank()) {
                // Admin or Staff replied -> Push directly to Client external_id
                val replyTitle = profileName.ifBlank { senderName.ifBlank { "JustUs" } }
                val clientPayload = JSONObject().apply {
                    put("app_id", appId)
                    put("target_channel", "push")
                    put(
                        "include_aliases",
                        JSONObject().put("external_id", JSONArray().put(clientUid))
                    )
                    put("headings", JSONObject().put("en", "New Reply · $replyTitle"))
                    put("contents", JSONObject().put("en", cleanMessage))
                    put(
                        "data",
                        JSONObject()
                            .put("type", "chat")
                            .put("chat_id", chatId)
                            .put("chat_role", "client")
                    )
                    if (avatarUrl.startsWith("https://")) {
                        put("large_icon", avatarUrl)
                    }
                    put("existing_android_channel_id", PixoraNotificationManager.CHANNEL_MESSAGES_ID)
                    put("priority", 10)
                    put("android_group", "justus_client_chat")
                }
                postNotification(clientPayload)
            }
        }
    }

    fun sendCallPushAsync(
        callId: String,
        callerName: String,
        callerRole: String,
        calleeUid: String,
        chatId: String,
        profileName: String,
        isVideoCall: Boolean = false,
    ) {
        if (!isFullyConfigured() || calleeUid.isBlank()) return
        pushScope.launch {
            val callTitle = if (isVideoCall) "Incoming HD Video Call" else "Incoming Voice Call"
            val payload = JSONObject().apply {
                put("app_id", appId)
                if (calleeUid == "__ADMIN__") {
                    put(
                        "filters",
                        JSONArray().put(
                            JSONObject().apply {
                                put("field", "tag")
                                put("key", "is_admin")
                                put("relation", "=")
                                put("value", "true")
                            }
                        )
                    )
                } else {
                    put("target_channel", "push")
                    put(
                        "include_aliases",
                        JSONObject().put("external_id", JSONArray().put(calleeUid))
                    )
                }
                put("headings", JSONObject().put("en", "$callTitle · ${callerName.ifBlank { "DateMate" }}"))
                put(
                    "contents",
                    JSONObject().put(
                        "en",
                        if (profileName.isNotBlank()) "Private ${if (isVideoCall) "Video" else "Voice"} Call · $profileName · Tap to answer" else "Tap to answer"
                    )
                )
                put(
                    "data",
                    JSONObject()
                        .put("type", "call")
                        .put("call_id", callId)
                        .put("call_type", if (isVideoCall) "video" else "audio")
                        .put("chat_id", chatId)
                        .put("chat_role", if (callerRole == "admin") "client" else "admin")
                )
                put("existing_android_channel_id", PixoraNotificationManager.CHANNEL_CALLS_ID)
                put("priority", 10)
                put("ttl", 60)
            }
            postNotification(payload)
        }
    }

    fun sendNewProfilePushAsync(
        profileId: String,
        profileName: String,
        price: String,
        category: String,
        coverUrl: String,
        isHidden: Boolean,
    ) {
        if (!isFullyConfigured() || profileId.isBlank()) return
        pushScope.launch {
            val payload = JSONObject().apply {
                put("app_id", appId)
                put("included_segments", JSONArray().put("Subscribed Users"))
                put(
                    "headings",
                    JSONObject().put(
                        "en",
                        if (isHidden) "Private Secret Moment Unlocked" else "New Profile Live · $category"
                    )
                )
                put(
                    "contents",
                    JSONObject().put(
                        "en",
                        if (isHidden) "$profileName · $price · Open before timer ends!"
                        else "$profileName · $price · Tap to view profile"
                    )
                )
                put(
                    "data",
                    JSONObject()
                        .put("type", "profile")
                        .put("image_id", profileId)
                )
                if (!isHidden && coverUrl.startsWith("https://")) {
                    put("big_picture", coverUrl)
                    put("large_icon", coverUrl)
                }
                if (isHidden) {
                    put("android_visibility", 0)
                }
                put("existing_android_channel_id", PixoraNotificationManager.CHANNEL_ID)
                put("priority", 10)
            }
            postNotification(payload)
        }
    }

    private fun postNotification(payload: JSONObject) {
        val key = restApiKey
        if (key.isBlank() || key == PLACEHOLDER_REST_KEY) return
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val jsonBody = payload.toString()

        // Newer OneSignal v2 keys use "Key ...", legacy App API keys use "Basic ..." (try Key first, fallback Basic on 401/403)
        val primaryScheme = if (key.startsWith("os_v2_")) "Key" else "Key"
        val primaryCode = executePost(jsonBody, mediaType, "$primaryScheme $key")
        if ((primaryCode == 401 || primaryCode == 403) && !key.startsWith("os_v2_")) {
            executePost(jsonBody, mediaType, "Basic $key")
        }
    }

    private fun executePost(
        jsonBody: String,
        mediaType: okhttp3.MediaType,
        authHeader: String,
    ): Int {
        return runCatching {
            val req = Request.Builder()
                .url(ENDPOINT)
                .post(jsonBody.toRequestBody(mediaType))
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val errText = resp.body?.string().orEmpty().take(300)
                    Log.w(TAG, "OneSignal push failed (${resp.code}): $errText")
                } else {
                    Log.d(TAG, "OneSignal push queued (${resp.code})")
                }
                resp.code
            }
        }.getOrElse { err ->
            Log.w(TAG, "OneSignal network error: ${err.localizedMessage}")
            -1
        }
    }
}
