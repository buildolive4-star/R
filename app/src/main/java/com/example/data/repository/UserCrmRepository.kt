package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
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
    // Multi-Admin & Revocable Transfer Code Management
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
        val adminSnap = runCatching {
            db.collection(COLLECTION_ADMINS).document(user.uid).get().await()
        }.getOrNull() ?: return false
        if (!adminSnap.exists()) return false
        val codeUsed = adminSnap.getString("codeUsed")?.trim()?.uppercase().orEmpty()
        if (!codeUsed.matches(ACCESS_CODE_REGEX)) return false
        val codeSnap = runCatching {
            db.collection(COLLECTION_ADMIN_CODES).document(codeUsed).get().await()
        }.getOrNull() ?: return false
        return codeSnap.exists() && (codeSnap.getBoolean("active") == true)
    }

    fun observeMyAdminMember(uid: String): Flow<AdminMemberProfile?> {
        if (uid.isBlank()) return flow { emit(null) }
        val ref = db.collection(COLLECTION_ADMINS).document(uid)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    AdminMemberProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { emit(null) }
    }

    fun observeCodeActive(code: String): Flow<Boolean> {
        val clean = normalizeAccessCode(code)
        if (!clean.matches(ACCESS_CODE_REGEX)) return flow { emit(false) }
        return db.collection(COLLECTION_ADMIN_CODES).document(clean)
            .snapshots()
            .map { snap -> snap.exists() && (snap.getBoolean("active") == true) }
            .catch { emit(false) }
    }

    fun isSuperAdminEmail(email: String?): Boolean =
        email?.trim()?.lowercase() == SUPER_ADMIN_EMAIL

    fun observeAdminAccessCodes(): Flow<List<AdminAccessCode>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_ADMIN_CODES)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminAccessCode::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_ADMIN_CODES)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeAdminMembers(): Flow<List<AdminMemberProfile>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_ADMINS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminMemberProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_ADMINS)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun createAdminAccessCode(rawCode: String, label: String): Result<String> {
        val cleanCode = normalizeAccessCode(rawCode)
        val path = "$COLLECTION_ADMIN_CODES/$cleanCode"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can create admin transfer codes."
            }
            require(cleanCode.matches(ACCESS_CODE_REGEX)) {
                "Code must be 4-24 characters (letters A-Z, digits 0-9, _ or -)."
            }
            db.document(path).set(
                mapOf(
                    "code" to cleanCode,
                    "label" to label.trim().ifBlank { "Partner Admin" }.take(60),
                    "createdByEmail" to currentEmail,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(cleanCode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAdminAccessCode(code: String): Result<Unit> {
        val cleanCode = normalizeAccessCode(code)
        val path = "$COLLECTION_ADMIN_CODES/$cleanCode"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can delete transfer codes."
            }
            require(cleanCode.matches(ACCESS_CODE_REGEX)) { "Invalid code" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun removeAdminMember(memberUid: String): Result<Unit> {
        val path = "$COLLECTION_ADMINS/$memberUid"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can remove admin sessions."
            }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveMainAdminProfile(
        name: String,
        phone: String,
        email: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val path = "$COLLECTION_ADMINS/$uid"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase().orEmpty()
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can update Main Admin identity."
            }
            require(name.trim().length >= 2) { "Enter Main Admin name" }
            require(phone.count { it.isDigit() } >= 8) { "Enter valid mobile number" }
            val safeEmail = email.trim().ifBlank { currentEmail }.take(120)
            val snap = db.document(path).get().await()
            if (!snap.exists()) {
                db.document(path).set(
                    mapOf(
                        "name" to name.trim().take(80),
                        "phone" to phone.trim().take(24),
                        "email" to safeEmail,
                        "codeUsed" to "MASTER",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                db.document(path).update(
                    mapOf(
                        "name" to name.trim().take(80),
                        "phone" to phone.trim().take(24),
                        "email" to safeEmail,
                        "codeUsed" to "MASTER",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun redeemAdminAccessCode(
        name: String,
        phone: String,
        email: String,
        rawCode: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val cleanCode = normalizeAccessCode(rawCode)
        val cleanName = name.trim().take(80)
        val cleanPhone = phone.trim().take(24)
        val fallbackEmail = Firebase.auth.currentUser?.email.orEmpty()
        val cleanEmail = email.trim().ifBlank { fallbackEmail }.take(120)

        return try {
            require(cleanName.length >= 2) { "Please enter your full name" }
            require(cleanPhone.count { it.isDigit() } >= 8) { "Please enter a valid mobile number" }
            require(cleanEmail.contains("@")) { "Please enter a valid email address" }
            require(cleanCode.matches(ACCESS_CODE_REGEX)) {
                "Enter a valid Admin Code (4-24 letters/digits)"
            }

            val codeSnap = db.collection(COLLECTION_ADMIN_CODES).document(cleanCode).get().await()
            if (!codeSnap.exists() || codeSnap.getBoolean("active") != true) {
                return Result.failure(
                    IllegalStateException("Invalid or deleted Admin Code. Ask Main Admin for an active code.")
                )
            }

            val docRef = db.collection(COLLECTION_ADMINS).document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "name" to cleanName,
                        "phone" to cleanPhone,
                        "email" to cleanEmail,
                        "codeUsed" to cleanCode,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "phone" to cleanPhone,
                        "email" to cleanEmail,
                        "codeUsed" to cleanCode,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "$COLLECTION_ADMINS/$uid")
            Result.failure(e)
        }
    }

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
        const val COLLECTION_ADMINS = "admins"
        const val COLLECTION_ADMIN_EMAILS = "admin_emails"
        const val COLLECTION_ADMIN_CODES = "admin_access_codes"
        const val SUPER_ADMIN_EMAIL = "buildolive4@gmail.com"
        private val ACCESS_CODE_REGEX = Regex("^[A-Z0-9_\\-]{4,24}$")

        fun normalizeAccessCode(raw: String): String =
            raw.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
    }
}
