package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CompanionPrivateContact
import com.example.data.model.CompanionProfile
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileTier
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

class GalleryRepository(
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

    fun observePublishedImages(): Flow<List<GalleryImage>> =
        db.collection(COLLECTION_COMPANION_PROFILES)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CompanionProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    .map { it.toGalleryImage() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_COMPANION_PROFILES)
                }
                throw error
            }

    fun observeAllProfilesForAdmin(): Flow<List<CompanionProfile>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_COMPANION_PROFILES)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CompanionProfile::class.java,
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
                        handleFirestoreError(exceptionToLog, OperationType.LIST, COLLECTION_COMPANION_PROFILES)
                    }
                    throw error
                }
        )
    }

    suspend fun createCompanionProfile(
        name: String,
        age: Int,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        photoUrls: List<String>,
        hiddenTimed: Boolean,
        phone: String,
        address: String,
        ownerUid: String = "",
        staffVerified: Boolean = true,
        customProfileId: String? = null,
    ): Result<String> {
        var docPath = COLLECTION_COMPANION_PROFILES
        return try {
            val adminUid = requireSignedInUid()
            val safeName = name.trim().take(80)
            require(safeName.isNotEmpty()) { "Companion name is required" }
            val safeAge = age.coerceIn(18, 80)
            val safePrice = price.trim().ifBlank { "₹299" }.take(32)
            val safeRole = when (role.trim().uppercase()) {
                "BF" -> "BF"
                "COMPANION" -> "Companion"
                else -> "GF"
            }
            val safeTier = if (tier == ProfileTier.VIP_16_9) ProfileTier.VIP_16_9 else ProfileTier.GRID_9_16
            val safeCategory = category.trim().ifBlank {
                if (safeTier == ProfileTier.VIP_16_9) "VIP" else "Companion"
            }.take(40)
            val safeBio = bio.trim().take(600)
            val safePhotos = photoUrls
                .map { normalizeHttpsUrl(it).take(1000) }
                .filter { it.startsWith("https://") && it.length >= 8 }
                .distinct()
                .take(8)
            require(safePhotos.isNotEmpty()) { "At least one valid photo URL is required" }

            val ratio = if (safeTier == ProfileTier.VIP_16_9) 16.0 / 9.0 else 9.0 / 16.0
            val docRef = if (!customProfileId.isNullOrBlank()) {
                db.collection(COLLECTION_COMPANION_PROFILES).document(customProfileId)
            } else {
                db.collection(COLLECTION_COMPANION_PROFILES).document()
            }
            docPath = docRef.path

            val publicPayload = hashMapOf<String, Any>(
                "name" to safeName,
                "age" to safeAge,
                "price" to safePrice,
                "role" to safeRole,
                "category" to safeCategory,
                "tier" to safeTier,
                "bio" to safeBio,
                "photoUrls" to safePhotos,
                "coverUrl" to safePhotos.first(),
                "aspectRatio" to ratio,
                "hiddenTimed" to hiddenTimed,
                "refreshEpochMs" to System.currentTimeMillis(),
                "status" to "published",
                "ownerUid" to ownerUid.trim().take(128),
                "staffVerified" to staffVerified,
                "createdByUid" to adminUid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(publicPayload).await()

            val contactRef = docRef.collection("private").document("contact")
            val privatePayload = hashMapOf<String, Any>(
                "phone" to phone.trim().take(24),
                "address" to address.trim().take(240),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            contactRef.set(privatePayload).await()

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun getPrivateContactForAdmin(profileId: String): Result<CompanionPrivateContact> {
        val path = "$COLLECTION_COMPANION_PROFILES/$profileId/private/contact"
        return try {
            requireSignedInUid()
            require(profileId.isNotBlank()) { "Profile ID required" }
            val snap = db.document(path).get().await()
            val contact = snap.toObject(CompanionPrivateContact::class.java) ?: CompanionPrivateContact()
            Result.success(contact)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun updateCompanionProfile(
        profileId: String,
        name: String,
        age: Int,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        status: String = "published",
        phone: String? = null,
        address: String? = null,
    ): Result<Unit> {
        val docPath = "$COLLECTION_COMPANION_PROFILES/$profileId"
        return try {
            requireSignedInUid()
            require(profileId.isNotBlank()) { "Profile ID required" }
            val safeTier = if (tier == ProfileTier.VIP_16_9) ProfileTier.VIP_16_9 else ProfileTier.GRID_9_16
            val ratio = if (safeTier == ProfileTier.VIP_16_9) 16.0 / 9.0 else 9.0 / 16.0
            db.document(docPath).update(
                mapOf(
                    "name" to name.trim().take(80),
                    "age" to age.coerceIn(18, 80),
                    "price" to price.trim().ifBlank { "₹299" }.take(32),
                    "role" to role.trim().ifBlank { "GF" },
                    "category" to category.trim().ifBlank { "Companion" }.take(40),
                    "tier" to safeTier,
                    "aspectRatio" to ratio,
                    "bio" to bio.trim().take(600),
                    "hiddenTimed" to hiddenTimed,
                    "status" to if (status == "archived") "archived" else "published",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            if (phone != null || address != null) {
                val contactPath = "$docPath/private/contact"
                val existing = runCatching {
                    db.document(contactPath).get().await().toObject(CompanionPrivateContact::class.java)
                }.getOrNull()
                db.document(contactPath).set(
                    mapOf(
                        "phone" to (phone ?: existing?.phone.orEmpty()).trim().take(24),
                        "address" to (address ?: existing?.address.orEmpty()).trim().take(240),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun refreshRemoteImage(profileId: String): Result<Unit> {
        val docPath = "$COLLECTION_COMPANION_PROFILES/$profileId"
        return try {
            requireSignedInUid()
            require(profileId.isNotBlank()) { "Profile ID required" }
            db.document(docPath).update(
                mapOf(
                    "hiddenTimed" to true,
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

    suspend fun deleteCompanionProfile(profileId: String): Result<Unit> {
        val docPath = "$COLLECTION_COMPANION_PROFILES/$profileId"
        return try {
            requireSignedInUid()
            require(profileId.isNotBlank()) { "Profile ID required" }
            runCatching { db.document("$docPath/private/contact").delete().await() }
            db.document(docPath).delete().await()
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
        const val COLLECTION_COMPANION_PROFILES = "companion_profiles"
    }
}
