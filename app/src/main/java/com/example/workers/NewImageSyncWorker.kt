package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.ChatThread
import com.example.data.model.CompanionProfile
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.repository.ChatRepository
import com.example.data.repository.GalleryRepository
import com.example.notifications.PixoraNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class NewImageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val store = NotifiedImagesStore(applicationContext)
        if (!store.isBackgroundSyncEnabled()) {
            return Result.success()
        }

        return try {
            val databaseId = applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)

            val snapshot = db.collection(GalleryRepository.COLLECTION_COMPANION_PROFILES)
                .whereEqualTo("status", "published")
                .get()
                .await()

            val profiles = snapshot.toObjects(
                CompanionProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
                .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                .map { it.toGalleryImage() }

            store.updateLastSyncCheck(System.currentTimeMillis())

            val knownTokens = store.getKnownIds()
            val fetchedTokens = profiles.mapNotNull { img ->
                img.id.takeIf(String::isNotBlank)?.let {
                    NotifiedImagesStore.buildProfileAlertToken(it, img.refreshEpochMs)
                }
            }

            if (knownTokens.isEmpty()) {
                store.markIdsKnown(fetchedTokens)
            } else {
                val newcomers = profiles.filter { img ->
                    img.id.isNotBlank() && !knownTokens.contains(
                        NotifiedImagesStore.buildProfileAlertToken(img.id, img.refreshEpochMs)
                    )
                }
                if (newcomers.isNotEmpty()) {
                    PixoraNotificationManager.ensureChannel(applicationContext)
                    newcomers.take(MAX_ALERTS_PER_SYNC).forEach { newProfile ->
                        PixoraNotificationManager.notifyNewImage(applicationContext, newProfile)
                    }
                    store.markIdsKnown(fetchedTokens)
                }
            }

            // Also check unread chat messages for the currently signed-in user in background
            val uid = Firebase.auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                checkBackgroundChatMessages(db, store, uid)
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, GalleryRepository.COLLECTION_COMPANION_PROFILES)
            Log.w(TAG, "Background sync check failed, will retry", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkBackgroundChatMessages(
        db: FirebaseFirestore,
        store: NotifiedImagesStore,
        uid: String,
    ) {
        runCatching {
            val clientSnap = db.collection(ChatRepository.COLLECTION_CHATS)
                .whereEqualTo("clientUid", uid)
                .get()
                .await()
            val clientThreads = clientSnap.toObjects(
                ChatThread::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            for (thread in clientThreads) {
                val updatedMs = thread.updatedAt?.toDate()?.time ?: continue
                if (thread.unreadForClient > 0 && thread.lastSenderRole != "client") {
                    val lastNotified = store.getLastNotifiedChatEpoch(thread.id)
                    if (updatedMs > lastNotified) {
                        PixoraNotificationManager.notifyNewChatMessage(
                            context = applicationContext,
                            chatId = thread.id,
                            recipientRole = "client",
                            senderName = thread.profileName.ifBlank { "JustUs" },
                            messageText = thread.lastMessage,
                            avatarUrl = thread.profilePhotoUrl,
                        )
                        store.markChatNotified(thread.id, updatedMs)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "JustUsSyncWorker"
        private const val MAX_ALERTS_PER_SYNC = 3
    }
}
