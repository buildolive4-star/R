package com.example.notifications

import android.content.Context
import com.example.R
import com.example.data.model.ChatThread
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserCrmRepository
import com.example.ui.components.InAppBannerEvent
import com.google.firebase.Firebase
import com.google.firebase. FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Application-scoped real-time push notification engine that stays active even when
 * the user minimizes the app or goes to the Android Home Screen ("App ke bahar").
 *
 * Ensures that every new client message, companion reply, or admin reply immediately
 * triggers a real Android OS System Push Notification (Status Bar + Heads-Up Popup + Lockscreen).
 */
object DateMateBackgroundPushEngine {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isStarted = false

    @Volatile
    private var activeViewingChatId: String = ""

    @Volatile
    private var activeViewingChatRole: String = ""

    private var activeAuthWatchJob: Job? = null

    private val _inAppBannerEvents = MutableSharedFlow<InAppBannerEvent>(extraBufferCapacity = 8)
    val inAppBannerEvents: SharedFlow<InAppBannerEvent> = _inAppBannerEvents.asSharedFlow()

    fun setCurrentlyViewingChat(chatId: String, role: String) {
        activeViewingChatId = chatId
        activeViewingChatRole = role
    }

    fun start(applicationContext: Context) {
        if (isStarted) return
        val appCtx = applicationContext.applicationContext ?: applicationContext
        val hasFirebase = runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)
        if (!hasFirebase) return

        isStarted = true
        val db = runCatching {
            FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
        }.getOrNull() ?: return

        val chatRepo = ChatRepository(db)
        val crmRepo = UserCrmRepository(db)
        val notifiedStore = NotifiedImagesStore(appCtx)

        val authListener = FirebaseAuth.AuthStateListener { auth ->
            val uid = auth.currentUser?.uid.orEmpty()
            restartUserPushStreams(
                appCtx = appCtx,
                uid = uid,
                chatRepo = chatRepo,
                crmRepo = crmRepo,
                notifiedStore = notifiedStore,
            )
        }
        Firebase.auth.addAuthStateListener(authListener)

        // Also trigger an immediate check for current user
        val initialUid = Firebase.auth.currentUser?.uid.orEmpty()
        restartUserPushStreams(
            appCtx = appCtx,
            uid = initialUid,
            chatRepo = chatRepo,
            crmRepo = crmRepo,
            notifiedStore = notifiedStore,
        )
    }

    fun refreshNow(applicationContext: Context) {
        val appCtx = applicationContext.applicationContext ?: applicationContext
        val hasFirebase = runCatching { FirebaseApp.getApps(appCtx).isNotEmpty() }.getOrDefault(false)
        if (!hasFirebase) return
        val db = runCatching {
            FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
        }.getOrNull() ?: return
        val uid = Firebase.auth.currentUser?.uid.orEmpty()
        restartUserPushStreams(
            appCtx = appCtx,
            uid = uid,
            chatRepo = ChatRepository(db),
            crmRepo = UserCrmRepository(db),
            notifiedStore = NotifiedImagesStore(appCtx),
        )
    }

    private fun restartUserPushStreams(
        appCtx: Context,
        uid: String,
        chatRepo: ChatRepository,
        crmRepo: UserCrmRepository,
        notifiedStore: NotifiedImagesStore,
    ) {
        activeAuthWatchJob?.cancel()
        if (uid.isBlank()) return

        activeAuthWatchJob = engineScope.launch {
            val isVerifiedAdmin = runCatching { crmRepo.checkIsCurrentUserAdmin() }.getOrDefault(false)
            val clientFlow = chatRepo.observeClientChats(uid)
            val staffFlow = chatRepo.observeStaffInboxChats(uid)
            val adminFlow = if (isVerifiedAdmin) chatRepo.observeAllChatsForAdmin() else flowOf(emptyList())

            combine(clientFlow, staffFlow, adminFlow) { clientChats, staffChats, adminChats ->
                Triple(clientChats, staffChats, adminChats)
            }.collect { (clientChats, staffChats, adminChats) ->
                processIncomingChatSnapshots(
                    appCtx = appCtx,
                    notifiedStore = notifiedStore,
                    clientChats = clientChats,
                    staffChats = staffChats,
                    adminChats = adminChats,
                    isVerifiedAdmin = isVerifiedAdmin,
                )
            }
        }
    }

    private suspend fun processIncomingChatSnapshots(
        appCtx: Context,
        notifiedStore: NotifiedImagesStore,
        clientChats: List<ChatThread>,
        staffChats: List<ChatThread>,
        adminChats: List<ChatThread>,
        isVerifiedAdmin: Boolean,
    ) {
        data class PendingChatAlert(
            val chatId: String,
            val role: String,
            val senderName: String,
            val message: String,
            val avatar: String,
            val updatedMs: Long,
            val unreadCount: Int,
            val lastSenderRole: String,
        )

        val candidates = mutableListOf<PendingChatAlert>()

        clientChats.filter { it.unreadForClient > 0 && it.lastSenderRole != "client" }.forEach { thread ->
            candidates.add(
                PendingChatAlert(
                    chatId = thread.id,
                    role = "client",
                    senderName = thread.profileName.ifBlank { "Companion" },
                    message = thread.lastMessage,
                    avatar = thread.profilePhotoUrl,
                    updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                    unreadCount = thread.unreadForClient,
                    lastSenderRole = thread.lastSenderRole,
                )
            )
        }

        staffChats.filter { it.unreadForStaff > 0 && it.lastSenderRole == "client" }.forEach { thread ->
            candidates.add(
                PendingChatAlert(
                    chatId = thread.id,
                    role = "staff",
                    senderName = thread.clientName.ifBlank { "Client" },
                    message = thread.lastMessage,
                    avatar = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                    updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                    unreadCount = thread.unreadForStaff,
                    lastSenderRole = thread.lastSenderRole,
                )
            )
        }

        if (isVerifiedAdmin) {
            adminChats.filter { it.unreadForAdmin > 0 && it.lastSenderRole == "client" }.forEach { thread ->
                candidates.add(
                    PendingChatAlert(
                        chatId = thread.id,
                        role = "admin",
                        senderName = "${thread.clientName.ifBlank { "Client" }} → ${thread.profileName}",
                        message = thread.lastMessage,
                        avatar = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                        updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                        unreadCount = thread.unreadForAdmin,
                        lastSenderRole = thread.lastSenderRole,
                    )
                )
            }
        }

        for (item in candidates.distinctBy { "${it.chatId}_${it.role}" }) {
            val trackKey = "${item.chatId}_${item.role}"
            val lastNotified = notifiedStore.getLastNotifiedChatEpoch(trackKey)
            val messageStamp = NotifiedImagesStore.buildChatAlertStamp(
                updatedMs = item.updatedMs,
                lastMessage = item.message,
                unreadCount = item.unreadCount,
                lastSenderRole = item.lastSenderRole,
            )

            if (messageStamp != lastNotified) {
                notifiedStore.markChatNotified(trackKey, messageStamp)
                val isCurrentlyInsideSameChat =
                    OneSignalPushManager.isAppInForeground &&
                        activeViewingChatId == item.chatId &&
                        activeViewingChatRole == item.role

                if (!isCurrentlyInsideSameChat) {
                    // 1. Always post real Android OS System Push Notification (Status Bar + Heads-Up + Lockscreen)
                    PixoraNotificationManager.playAlertChime(appCtx)
                    PixoraNotificationManager.postChatPushAsync(
                        context = appCtx,
                        chatId = item.chatId,
                        recipientRole = item.role,
                        senderName = item.senderName,
                        messageText = item.message,
                        avatarUrl = item.avatar,
                    )

                    // 2. Also emit to in-app banner stream if app is open
                    _inAppBannerEvents.tryEmit(
                        InAppBannerEvent.ChatMessageArrival(
                            chatId = item.chatId,
                            senderRoleForOpen = item.role,
                            title = item.senderName,
                            preview = item.message.ifBlank { "New message received" },
                            avatarUrl = item.avatar,
                        )
                    )
                }
            }
        }
    }
}
