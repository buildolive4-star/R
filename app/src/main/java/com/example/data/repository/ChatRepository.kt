package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.GalleryImage
import com.example.data.model.ModerationStatus
import com.example.data.model.UserAccount
import com.example.notifications.OneSignalPushManager
import com.example.util.ContactLeakFilter
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireSignedInUid(): String =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google first.")

    private fun DocumentSnapshot.toChatThreadSafe(): ChatThread? =
        runCatching {
            toObject(
                ChatThread::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )?.let { if (it.id.isBlank()) it.copy(id = id) else it }
        }.getOrNull()

    fun observeClientChats(clientUid: String): Flow<List<ChatThread>> {
        if (clientUid.isBlank()) return flow { emit(emptyList()) }
        return db.collection(COLLECTION_CHATS)
            .whereEqualTo("clientUid", clientUid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toChatThreadSafe() }
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_CHATS)
                }
                emit(emptyList())
            }
    }

    fun observeStaffInboxChats(staffUid: String): Flow<List<ChatThread>> {
        if (staffUid.isBlank()) return flow { emit(emptyList()) }
        return db.collection(COLLECTION_CHATS)
            .whereEqualTo("staffOwnerUid", staffUid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toChatThreadSafe() }
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_CHATS)
                }
                emit(emptyList())
            }
    }

    fun observeAllChatsForAdmin(): Flow<List<ChatThread>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_CHATS)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.toChatThreadSafe() }
                        .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_CHATS)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeChatThread(chatId: String): Flow<ChatThread?> {
        if (chatId.isBlank()) return flow { emit(null) }
        val docRef = db.collection(COLLECTION_CHATS).document(chatId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toChatThreadSafe()
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.GET, docRef.path)
                }
                emit(null)
            }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        if (chatId.isBlank()) return flow { emit(emptyList()) }
        val messagesRef = db.collection(COLLECTION_CHATS).document(chatId).collection(COLLECTION_MESSAGES)
        return messagesRef
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        doc.toObject(
                            ChatMessage::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                    }.getOrNull()
                }.sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, messagesRef.path)
                }
                emit(emptyList())
            }
    }

    suspend fun openOrCreateChat(
        clientAccount: UserAccount,
        profile: GalleryImage,
    ): Result<ChatThread> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        if (clientAccount.moderationStatus == ModerationStatus.BLOCKED) {
            return Result.failure(IllegalStateException("Your account is blocked by Admin."))
        }
        if (clientAccount.moderationStatus == ModerationStatus.SUSPENDED) {
            return Result.failure(IllegalStateException("Your account is temporarily suspended."))
        }
        val safeProfileId = profile.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "companion" }
        val chatId = "${uid}_$safeProfileId".take(120)
        val docRef = db.collection(COLLECTION_CHATS).document(chatId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = hashMapOf<String, Any>(
                    "clientUid" to uid,
                    "clientName" to clientAccount.name.ifBlank { "Guest" }.take(80),
                    "clientPhotoUrl" to clientAccount.photoUrl.take(1000),
                    "profileId" to safeProfileId,
                    "profileName" to profile.title.ifBlank { "Companion" }.take(80),
                    "profilePhotoUrl" to profile.displayThumbnailUrl.take(1000),
                    "profilePrice" to profile.displayPrice.take(32),
                    "staffOwnerUid" to profile.ownerUid.trim().take(128),
                    "lastMessage" to "Inquiry started",
                    "lastSenderRole" to "client",
                    "unreadForAdmin" to 1,
                    "unreadForStaff" to if (profile.ownerUid.isNotBlank()) 1 else 0,
                    "unreadForClient" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
                // Flag user inquiry notification for Admin filter
                runCatching {
                    db.collection(UserCrmRepository.COLLECTION_USERS).document(uid)
                        .update(
                            mapOf(
                                "hasInquiryNotification" to true,
                                "lastActiveEpochMs" to System.currentTimeMillis(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        ).await()
                }
                OneSignalPushManager.sendChatPushAsync(
                    chatId = chatId,
                    senderRole = "client",
                    senderName = clientAccount.name.ifBlank { "Guest" },
                    clientUid = uid,
                    clientName = clientAccount.name.ifBlank { "Guest" },
                    staffOwnerUid = profile.ownerUid.trim(),
                    profileName = profile.title.ifBlank { "Companion" },
                    messageText = "Started a new inquiry for ${profile.title}",
                    avatarUrl = clientAccount.photoUrl.ifBlank { profile.displayThumbnailUrl },
                )
            }
            val saved = docRef.get().await().toChatThreadSafe() ?: ChatThread(
                id = chatId,
                clientUid = uid,
                clientName = clientAccount.name,
                profileId = safeProfileId,
                profileName = profile.title,
                profilePhotoUrl = profile.displayThumbnailUrl,
                profilePrice = profile.displayPrice,
                staffOwnerUid = profile.ownerUid,
            )
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        chat: ChatThread,
        senderRole: String, // "client", "staff", "admin"
        senderName: String,
        rawText: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val cleanText = rawText.trim().take(1000)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Message cannot be empty"))
        }

        if (senderRole == "staff") {
            val check = ContactLeakFilter.validateForStaff(cleanText)
            if (!check.isAllowed) {
                return Result.failure(IllegalArgumentException(check.reason ?: "Blocked personal contact detail"))
            }
        }

        val chatRef = db.collection(COLLECTION_CHATS).document(chat.id)
        val msgRef = chatRef.collection(COLLECTION_MESSAGES).document()
        return try {
            msgRef.set(
                mapOf(
                    "senderUid" to uid,
                    "senderRole" to senderRole,
                    "senderName" to senderName.trim().ifBlank { "JustUs" }.take(80),
                    "text" to cleanText,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            chatRef.update(
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastSenderRole" to senderRole,
                    "unreadForAdmin" to if (senderRole == "client") chat.unreadForAdmin + 1 else 0,
                    "unreadForStaff" to if (senderRole == "client" && chat.staffOwnerUid.isNotBlank()) chat.unreadForStaff + 1 else 0,
                    "unreadForClient" to if (senderRole != "client") chat.unreadForClient + 1 else 0,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            if (senderRole == "client") {
                runCatching {
                    db.collection(UserCrmRepository.COLLECTION_USERS).document(chat.clientUid)
                        .update(
                            mapOf(
                                "hasInquiryNotification" to true,
                                "online" to true,
                                "lastActiveEpochMs" to System.currentTimeMillis(),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        ).await()
                }
            }
            OneSignalPushManager.sendChatPushAsync(
                chatId = chat.id,
                senderRole = senderRole,
                senderName = senderName.trim().ifBlank { "JustUs" },
                clientUid = chat.clientUid,
                clientName = chat.clientName,
                staffOwnerUid = chat.staffOwnerUid,
                profileName = chat.profileName,
                messageText = cleanText,
                avatarUrl = if (senderRole == "client") chat.clientPhotoUrl.ifBlank { chat.profilePhotoUrl } else chat.profilePhotoUrl,
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun markChatThreadRead(chatId: String, readerRole: String) {
        if (chatId.isBlank()) return
        runCatching { requireSignedInUid() }.getOrNull() ?: return
        val nowMs = System.currentTimeMillis()
        val updates = when (readerRole) {
            "admin" -> mapOf(
                "unreadForAdmin" to 0,
                "agentLastReadEpochMs" to nowMs,
                "agentLastDeliveredEpochMs" to nowMs,
            )
            "staff" -> mapOf(
                "unreadForStaff" to 0,
                "agentLastReadEpochMs" to nowMs,
                "agentLastDeliveredEpochMs" to nowMs,
            )
            else -> mapOf(
                "unreadForClient" to 0,
                "clientLastReadEpochMs" to nowMs,
                "clientLastDeliveredEpochMs" to nowMs,
            )
        }
        runCatching {
            db.collection(COLLECTION_CHATS).document(chatId)
                .update(updates)
                .await()
        }
    }

    suspend fun markChatDelivered(chatId: String, receiverRole: String) {
        if (chatId.isBlank()) return
        runCatching { requireSignedInUid() }.getOrNull() ?: return
        val nowMs = System.currentTimeMillis()
        val field = if (receiverRole == "client") {
            "clientLastDeliveredEpochMs"
        } else {
            "agentLastDeliveredEpochMs"
        }
        runCatching {
            db.collection(COLLECTION_CHATS).document(chatId)
                .update(mapOf(field to nowMs))
                .await()
        }
    }

    suspend fun deleteChatThread(chatId: String): Result<Unit> {
        if (chatId.isBlank()) return Result.failure(IllegalArgumentException("Invalid chat ID"))
        val chatRef = db.collection(COLLECTION_CHATS).document(chatId)
        return try {
            requireSignedInUid()
            val messagesSnap = chatRef.collection(COLLECTION_MESSAGES).get().await()
            messagesSnap.documents.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            chatRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, chatRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessages(
        chat: ChatThread,
        messageIds: List<String>,
        remainingMessages: List<ChatMessage>,
    ): Result<Unit> {
        if (chat.id.isBlank() || messageIds.isEmpty()) return Result.success(Unit)
        val chatRef = db.collection(COLLECTION_CHATS).document(chat.id)
        return try {
            requireSignedInUid()
            messageIds.distinct().chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { msgId ->
                    batch.delete(chatRef.collection(COLLECTION_MESSAGES).document(msgId))
                }
                batch.commit().await()
            }
            val latestRemaining = remainingMessages
                .filter { it.id !in messageIds }
                .maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
            val nextPreview = latestRemaining?.text?.take(500) ?: "Message deleted"
            val nextRole = latestRemaining?.senderRole ?: chat.lastSenderRole
            chatRef.update(
                mapOf(
                    "lastMessage" to nextPreview,
                    "lastSenderRole" to nextRole,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "${chatRef.path}/$COLLECTION_MESSAGES")
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_CHATS = "chats"
        const val COLLECTION_MESSAGES = "messages"
    }
}
