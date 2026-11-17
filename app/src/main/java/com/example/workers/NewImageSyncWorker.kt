package com.example.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.GalleryImage
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.repository.GalleryRepository
import com.example.notifications.PixoraNotificationManager
import com.google.firebase.Timestamp
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

            val snapshot = db.collection(GalleryRepository.COLLECTION_GALLERY_IMAGES)
                .whereEqualTo("status", "published")
                .get()
                .await()

            val images = snapshot.toObjects(
                GalleryImage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }

            store.updateLastSyncCheck(System.currentTimeMillis())

            val knownIds = store.getKnownIds()
            val fetchedIds = images.mapNotNull { it.id.takeIf(String::isNotBlank) }

            if (knownIds.isEmpty()) {
                // First baseline sync: seed known IDs without spamming historical notifications.
                store.markIdsKnown(fetchedIds)
                return Result.success()
            }

            val newcomers = images.filter { it.id.isNotBlank() && !knownIds.contains(it.id) }
            if (newcomers.isNotEmpty()) {
                PixoraNotificationManager.ensureChannel(applicationContext)
                newcomers.take(MAX_ALERTS_PER_SYNC).forEach { newImage ->
                    PixoraNotificationManager.notifyNewImage(applicationContext, newImage)
                }
                store.markIdsKnown(fetchedIds)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, GalleryRepository.COLLECTION_GALLERY_IMAGES)
            Log.w(TAG, "Background sync check failed, will retry", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "PixoraSyncWorker"
        private const val MAX_ALERTS_PER_SYNC = 3
    }
}
