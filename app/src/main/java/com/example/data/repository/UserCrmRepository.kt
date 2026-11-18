package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminEmailEntry
import com.example.data.model.LeadTag
import com.example.data.model.ModerationStatus
import com.example.data.model.ProfileTier
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.util.ProfileCompletionCalculator
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserCrmRepository(
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

    fun observeCurrentUser(uid: String): Flow<UserAccount?> {
        if (uid.isBlank()) return flow { emit(null) }
        val ref = db.collection(COLLECTION_USERS).document(uid)
        return ref.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserAccount::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.GET, ref.path)
                }
                emit(null)
            }
    }

    suspend fun upsertOwnProfile(
        name: String,
        photoUrl: String,
        galleryUrls: List<String>,
        phone: String,
        age: Int,
        address: String,
        city: String,
        bio: String,
        interests: String,
        genderPreference: String,
    ): Result<UserAccount> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val authUser = Firebase.auth.currentUser
            val snap = docRef.get().await()
            val summary = ProfileCompletionCalculator.calculate(
                name = name,
                photoUrl = photoUrl,
                phone = phone,
                age = age,
                address = address,
                city = city,
                bio = bio,
                interests = interests,
                genderPreference = genderPreference,
            )
            val safePhotos = galleryUrls
                .map { it.trim().take(1000) }
                .filter { it.startsWith("https://") }
                .distinct()
                .take(6)

            val nowMs = System.currentTimeMillis()
            if (!snap.exists()) {
                val payload = hashMapOf<String, Any>(
                    "name" to name.trim().take(80),
                    "email" to (authUser?.email ?: "").trim().take(120),
                    "photoUrl" to photoUrl.trim().take(1000),
                    "galleryUrls" to safePhotos,
                    "phone" to phone.trim().take(24),
                    "age" to age.coerceIn(0, 99),
                    "address" to address.trim().take(240),
                    "city" to city.trim().take(60),
                    "bio" to bio.trim().take(400),
                    "interests" to interests.trim().take(160),
                    "genderPreference" to genderPreference.trim().take(32),
                    "completionPercent" to summary.percent,
                    "online" to true,
                    "lastActiveEpochMs" to nowMs,
                    "hasInquiryNotification" to false,
                    "leadTag" to LeadTag.NONE,
                    "moderationStatus" to ModerationStatus.ACTIVE,
                    "staffStatus" to StaffStatus.NONE,
                    "staffRole" to "",
                    "staffPrice" to "",
                    "staffTier" to ProfileTier.GRID_9_16,
                    "staffProfileId" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(80),
                        "email" to (authUser?.email ?: "").trim().take(120),
                        "photoUrl" to photoUrl.trim().take(1000),
                        "galleryUrls" to safePhotos,
                        "phone" to phone.trim().take(24),
                        "age" to age.coerceIn(0, 99),
                        "address" to address.trim().take(240),
                        "city" to city.trim().take(60),
                        "bio" to bio.trim().take(400),
                        "interests" to interests.trim().take(160),
                        "genderPreference" to genderPreference.trim().take(32),
                        "completionPercent" to summary.percent,
                        "online" to true,
                        "lastActiveEpochMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            val updated = docRef.get().await().toObject(
                UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: UserAccount(uid = uid, name = name, phone = phone, age = age, address = address)
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun setOnlinePresence(uid: String, online: Boolean) {
        if (uid.isBlank()) return
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        runCatching {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "online" to online,
                        "lastActiveEpochMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
        }
    }

    suspend fun submitStaffApplication(
        staffRole: String,
        staffPrice: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            val snap = docRef.get().await()
            val account = snap.toObject(UserAccount::class.java)
                ?: return Result.failure(IllegalStateException("Save your profile first."))
            if (account.completionPercent < 100) {
                return Result.failure(IllegalStateException("Complete 100% of your profile before applying."))
            }
            val safeRole = if (staffRole.equals("BF", ignoreCase = true)) "BF" else "GF"
            val safePrice = staffPrice.trim().ifBlank { "₹499" }.take(32)
            docRef.update(
                mapOf(
                    "staffStatus" to StaffStatus.PENDING,
                    "staffRole" to safeRole,
                    "staffPrice" to safePrice,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchUsersPageForAdmin(
        pageSize: Long = 30L,
        afterSnapshot: DocumentSnapshot? = null,
    ): Result<Pair<List<UserAccount>, DocumentSnapshot?>> {
        return try {
            requireSignedInUid()
            var query = db.collection(COLLECTION_USERS)
                .orderBy("lastActiveEpochMs", Query.Direction.DESCENDING)
                .limit(pageSize)
            if (afterSnapshot != null) {
                query = query.startAfter(afterSnapshot)
            }
            val snapshot = query.get().await()
            val users = snapshot.toObjects(
                UserAccount::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val lastDoc = snapshot.documents.lastOrNull()
            Result.success(users to lastDoc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_USERS)
            Result.failure(e)
        }
    }

    fun observeUsersForAdmin(): Flow<List<UserAccount>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_USERS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserAccount::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastActiveEpochMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_USERS)
                    }
                    throw error
                }
        )
    }

    suspend fun updateUserLeadTag(userId: String, leadTag: String): Result<Unit> {
        val docPath = "$COLLECTION_USERS/$userId"
        return try {
            requireSignedInUid()
            val safeTag = when (leadTag) {
                LeadTag.HOT, LeadTag.WARM, LeadTag.COLD -> leadTag
                else -> LeadTag.NONE
            }
            db.document(docPath).update(
                mapOf(
                    "leadTag" to safeTag,
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

    suspend fun updateUserModerationStatus(userId: String, status: String): Result<Unit> {
        val docPath = "$COLLECTION_USERS/$userId"
        return try {
            requireSignedInUid()
            val safeStatus = when (status) {
                ModerationStatus.BLOCKED, ModerationStatus.SUSPENDED -> status
                else -> ModerationStatus.ACTIVE
            }
            db.document(docPath).update(
                mapOf(
                    "moderationStatus" to safeStatus,
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

    suspend fun approveStaffApplication(
        user: UserAccount,
        tier: String,
        galleryRepository: GalleryRepository,
    ): Result<String> {
        val docPath = "$COLLECTION_USERS/${user.uid}"
        return try {
            requireSignedInUid()
            val safeTier = if (tier == ProfileTier.VIP_16_9) ProfileTier.VIP_16_9 else ProfileTier.GRID_9_16
            val profileId = user.staffProfileId.ifBlank { "staff_${user.uid.take(24)}" }
            val photos = buildList {
                if (user.photoUrl.startsWith("https://")) add(user.photoUrl)
                user.galleryUrls.filter { it.startsWith("https://") }.forEach { if (!contains(it)) add(it) }
            }
            require(photos.isNotEmpty()) { "Applicant must have at least one HTTPS profile photo." }

            val createResult = galleryRepository.createCompanionProfile(
                name = user.name.ifBlank { "JustUs Companion" },
                age = user.age.coerceIn(18, 80),
                price = user.staffPrice.ifBlank { "₹499" },
                role = user.staffRole.ifBlank { "GF" },
                category = if (safeTier == ProfileTier.VIP_16_9) "VIP" else "Verified Staff",
                tier = safeTier,
                bio = user.bio,
                photoUrls = photos,
                hiddenTimed = false,
                phone = user.phone,
                address = user.address,
                ownerUid = user.uid,
                staffVerified = true,
                customProfileId = profileId,
            )
            createResult.getOrThrow()

            db.document(docPath).update(
                mapOf(
                    "staffStatus" to StaffStatus.APPROVED,
                    "staffTier" to safeTier,
                    "staffProfileId" to profileId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            Result.success(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun rejectStaffApplication(userId: String): Result<Unit> {
        val docPath = "$COLLECTION_USERS/$userId"
        return try {
            requireSignedInUid()
            db.document(docPath).update(
                mapOf(
                    "staffStatus" to StaffStatus.REJECTED,
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

    // ========================================================================
    // Multi-Admin Management (Zero manual Firebase Console editing)
    // ========================================================================

    suspend fun checkIsCurrentUserAdmin(): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        val email = user.email?.trim()?.lowercase().orEmpty()
        if (!user.isEmailVerified || email.isEmpty()) return false
        if (email == SUPER_ADMIN_EMAIL) return true
        val byEmail = runCatching {
            db.collection(COLLECTION_ADMIN_EMAILS).document(email).get().await().exists()
        }.getOrDefault(false)
        if (byEmail) return true
        return runCatching {
            db.collection("admins").document(user.uid).get().await().exists()
        }.getOrDefault(false)
    }

    fun isSuperAdminEmail(email: String?): Boolean =
        email?.trim()?.lowercase() == SUPER_ADMIN_EMAIL

    fun observeAdminEmails(): Flow<List<AdminEmailEntry>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_ADMIN_EMAILS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminEmailEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_ADMIN_EMAILS)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun addAdminEmail(rawEmail: String): Result<Unit> {
        val cleanEmail = rawEmail.trim().lowercase()
        val path = "$COLLECTION_ADMIN_EMAILS/$cleanEmail"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Super Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Super Admin ($SUPER_ADMIN_EMAIL) can add co-admins"
            }
            require(cleanEmail.matches(Regex("^[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}$"))) {
                "Enter a valid Gmail / email address"
            }
            db.document(path).set(
                mapOf(
                    "email" to cleanEmail,
                    "addedBy" to currentEmail,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminEmail(email: String): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val path = "$COLLECTION_ADMIN_EMAILS/$cleanEmail"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Super Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Super Admin can remove co-admins"
            }
            require(cleanEmail != SUPER_ADMIN_EMAIL) { "Super Admin cannot be removed" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ADMIN_EMAILS = "admin_emails"
        const val SUPER_ADMIN_EMAIL = "buildolive4@gmail.com"
    }
}
