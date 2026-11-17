package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.GalleryImage
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GalleryRepository(
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminUid(): String =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Admin Google account must be signed in to manage cloud gallery.")

    fun observePublishedImages(): Flow<List<GalleryImage>> =
        db.collection(COLLECTION_GALLERY_IMAGES)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    GalleryImage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_GALLERY_IMAGES)
                }
                throw error
            }

    fun observeAllImagesForAdmin(): Flow<List<GalleryImage>> = flow {
        requireAdminUid()
        emitAll(
            db.collection(COLLECTION_GALLERY_IMAGES)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GalleryImage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_GALLERY_IMAGES)
                    }
                    throw error
                }
        )
    }

    suspend fun createImage(
        title: String,
        description: String,
        category: String,
        imageUrl: String,
        thumbUrl: String,
        aspectRatio: Double,
        featured: Boolean,
        status: String,
        tags: List<String>,
        price: String = "",
        isHidden: Boolean = false,
        refreshEpochMs: Long = 0L,
    ): Result<String> {
        var docPath = COLLECTION_GALLERY_IMAGES
        return try {
            val uid = requireAdminUid()
            val user = Firebase.auth.currentUser
            val uploaderName = (user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore('@')
                ?: "Aria · Companion").take(80)

            val safeTitle = title.trim().take(120)
            require(safeTitle.isNotEmpty()) { "Title is required" }
            val safeCategory = category.trim().ifBlank { "Date Night" }.take(40)
            val safeImageUrl = normalizeHttpsUrl(imageUrl).take(1000)
            require(safeImageUrl.startsWith("https://") && safeImageUrl.length >= 8) {
                "Image URL must be a valid https:// link"
            }
            val safeThumbUrl = normalizeHttpsUrl(thumbUrl).take(1000)
            val safeDescription = description.trim().take(600)
            val safePrice = price.trim().take(32)
            val safeAspectRatio = aspectRatio.coerceIn(0.3, 3.5)
            val safeStatus = if (status == "archived") "archived" else "published"
            val safeTags = tags.map { it.trim().take(32) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(10)

            val docRef = db.collection(COLLECTION_GALLERY_IMAGES).document()
            docPath = docRef.path

            val payload = hashMapOf<String, Any>(
                "title" to safeTitle,
                "description" to safeDescription,
                "category" to safeCategory,
                "imageUrl" to safeImageUrl,
                "thumbUrl" to safeThumbUrl,
                "aspectRatio" to safeAspectRatio,
                "price" to safePrice,
                "isHidden" to isHidden,
                "refreshEpochMs" to refreshEpochMs,
                "status" to safeStatus,
                "featured" to featured,
                "uploadedBy" to uid,
                "uploaderName" to uploaderName,
                "tags" to safeTags,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun refreshRemoteImage(imageId: String): Result<Unit> {
        var docPath = "$COLLECTION_GALLERY_IMAGES/$imageId"
        return try {
            requireAdminUid()
            require(imageId.isNotBlank()) { "Image ID must not be blank" }
            val docRef = db.collection(COLLECTION_GALLERY_IMAGES).document(imageId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "isHidden" to true,
                    "refreshEpochMs" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateImage(
        imageId: String,
        title: String,
        description: String,
        category: String,
        featured: Boolean,
        status: String,
        tags: List<String>,
    ): Result<Unit> {
        var docPath = "$COLLECTION_GALLERY_IMAGES/$imageId"
        return try {
            requireAdminUid()
            require(imageId.isNotBlank()) { "Image ID must not be blank" }
            val safeTitle = title.trim().take(120)
            require(safeTitle.isNotEmpty()) { "Title is required" }
            val safeCategory = category.trim().ifBlank { "Photography" }.take(40)
            val safeDescription = description.trim().take(600)
            val safeStatus = if (status == "archived") "archived" else "published"
            val safeTags = tags.map { it.trim().take(32) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(10)

            val docRef = db.collection(COLLECTION_GALLERY_IMAGES).document(imageId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to safeTitle,
                    "description" to safeDescription,
                    "category" to safeCategory,
                    "featured" to featured,
                    "status" to safeStatus,
                    "tags" to safeTags,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageId: String): Result<Unit> {
        var docPath = "$COLLECTION_GALLERY_IMAGES/$imageId"
        return try {
            requireAdminUid()
            require(imageId.isNotBlank()) { "Image ID must not be blank" }
            val docRef = db.collection(COLLECTION_GALLERY_IMAGES).document(imageId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun normalizeHttpsUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://")) {
            "https://" + trimmed.removePrefix("http://")
        } else {
            trimmed
        }
    }

    companion object {
        const val COLLECTION_GALLERY_IMAGES = "gallery_images"
    }
}
