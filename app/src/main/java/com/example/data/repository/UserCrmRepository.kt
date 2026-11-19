package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminChangeRequest
import com.example.data.model.AdminChangeRequestStatus
import com.example.data.model.AdminChangeRequestType
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
import com.example.data.model.AppShareConfig
import com.example.data.model.AppUpdateConfig
import com.example.data.model.HomeFiltersConfig
import com.example.data.model.LeadTag
import com.example.data.model.LaunchBannerConfig
import com.example.data.model.ModerationStatus
import com.example.data.model.PresenceStatus
import com.example.data.model.ProfileTier
import com.example.data.model.PromoTickerConfig
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.util.ProfileCompletionCalculator
import java.util.UUID
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
            val existingCity = snap.getString("city").orEmpty()
            val existingBio = snap.getString("bio").orEmpty()
            val existingInterests = snap.getString("interests").orEmpty()
            val existingGenderPref = snap.getString("genderPreference").orEmpty()
            val existingPhotoUrl = snap.getString("photoUrl").orEmpty()
            @Suppress("UNCHECKED_CAST")
            val existingGallery = (snap.get("galleryUrls") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.map { it.trim().take(1000) }
                ?.filter { it.startsWith("https://") }
                .orEmpty()

            val mergedPhotoUrl = photoUrl.trim().ifBlank { existingPhotoUrl }.take(1000)
            val mergedCity = city.trim().ifBlank { existingCity }.take(60)
            val mergedBio = bio.trim().ifBlank { existingBio }.take(400)
            val mergedInterests = interests.trim().ifBlank { existingInterests }.take(160)
            val mergedGenderPref = genderPreference.trim().ifBlank { existingGenderPref }.take(32)
            val rawGallery = galleryUrls
                .map { it.trim().take(1000) }
                .filter { it.startsWith("https://") }
                .distinct()
                .take(6)
            val safePhotos = if (rawGallery.isNotEmpty()) rawGallery else existingGallery.take(6)

            val summary = ProfileCompletionCalculator.calculate(
                name = name,
                photoUrl = mergedPhotoUrl,
                phone = phone,
                age = age,
                address = address,
                city = mergedCity,
                bio = mergedBio,
                interests = mergedInterests,
                genderPreference = mergedGenderPref,
            )

            val nowMs = System.currentTimeMillis()
            if (!snap.exists()) {
                val payload = hashMapOf<String, Any>(
                    "name" to name.trim().take(80),
                    "email" to (authUser?.email ?: "").trim().take(120),
                    "photoUrl" to mergedPhotoUrl,
                    "galleryUrls" to safePhotos,
                    "phone" to phone.trim().take(24),
                    "age" to age.coerceIn(0, 99),
                    "address" to address.trim().take(240),
                    "city" to mergedCity,
                    "bio" to mergedBio,
                    "interests" to mergedInterests,
                    "genderPreference" to mergedGenderPref,
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
                    "directChatEnabled" to false,
                    "staffOnline" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name.trim().take(80),
                        "email" to (authUser?.email ?: "").trim().take(120),
                        "photoUrl" to mergedPhotoUrl,
                        "galleryUrls" to safePhotos,
                        "phone" to phone.trim().take(24),
                        "age" to age.coerceIn(0, 99),
                        "address" to address.trim().take(240),
                        "city" to mergedCity,
                        "bio" to mergedBio,
                        "interests" to mergedInterests,
                        "genderPreference" to mergedGenderPref,
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

    suspend fun setOnlinePresence(uid: String, online: Boolean, updateAdminSentinel: Boolean = false) {
        if (uid.isBlank()) return
        val nowMs = System.currentTimeMillis()
        val presencePayload = mapOf(
            "online" to online,
            "lastSeenEpochMs" to nowMs,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        runCatching {
            db.collection(COLLECTION_PRESENCE).document(uid).set(presencePayload).await()
        }
        if (updateAdminSentinel) {
            runCatching {
                db.collection(COLLECTION_PRESENCE).document(ADMIN_PRESENCE_ID).set(presencePayload).await()
            }
        }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        runCatching {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "online" to online,
                        "lastActiveEpochMs" to nowMs,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
        }
    }

    fun observePresence(presenceId: String): Flow<PresenceStatus?> {
        val cleanId = presenceId.trim()
        if (cleanId.isBlank() || Firebase.auth.currentUser == null) return flowOf(null)
        return db.collection(COLLECTION_PRESENCE).document(cleanId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    PresenceStatus::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { emit(null) }
    }

    fun observePresencesMap(presenceIds: List<String>): Flow<Map<String, PresenceStatus>> {
        val distinctIds = presenceIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(25)
        if (distinctIds.isEmpty() || Firebase.auth.currentUser == null) {
            return flowOf(emptyMap())
        }
        val flows = distinctIds.map { id ->
            observePresence(id).map { status -> id to status }
        }
        return combine(flows) { pairs ->
            pairs.mapNotNull { (id, status) -> status?.let { id to it } }.toMap()
        }
    }

    fun observeLaunchBanner(): Flow<LaunchBannerConfig?> {
        val ref = db.collection(COLLECTION_APP_CONFIG).document(DOC_LAUNCH_BANNER)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(
                    LaunchBannerConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { emit(null) }
    }

    suspend fun saveLaunchBanner(imageUrl: String, title: String): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_LAUNCH_BANNER"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can upload the launch info image."
            }
            val cleanUrl = imageUrl.trim().take(1000)
            require(cleanUrl.startsWith("https://")) {
                "Please select an image or enter a valid https:// URL."
            }
            db.document(path).set(
                mapOf(
                    "active" to true,
                    "imageUrl" to cleanUrl,
                    "title" to title.trim().take(120),
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeLaunchBanner(): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_LAUNCH_BANNER"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can remove the launch info image."
            }
            db.document(path).set(
                mapOf(
                    "active" to false,
                    "imageUrl" to "",
                    "title" to "",
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeHomeFilters(): Flow<List<String>> {
        val ref = db.collection(COLLECTION_APP_CONFIG).document(DOC_HOME_FILTERS)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) emptyList()
                else {
                    val config = snap.toObject(
                        HomeFiltersConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    config?.filters
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?.distinctBy { it.lowercase() }
                        .orEmpty()
                }
            }
            .catch { emit(emptyList()) }
    }

    suspend fun addHomeFilter(rawLabel: String): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_HOME_FILTERS"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Admin first")
            val cleanLabel = rawLabel.trim().take(32)
            require(cleanLabel.length >= 2) { "Enter a filter name (at least 2 characters)." }
            val ref = db.document(path)
            val snap = ref.get().await()
            val existing = snap.toObject(HomeFiltersConfig::class.java)?.filters.orEmpty()
            val updated = (existing + cleanLabel)
                .map { it.trim().take(32) }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(24)
            ref.set(
                mapOf(
                    "filters" to updated,
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeHomeFilter(label: String): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_HOME_FILTERS"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Admin first")
            val ref = db.document(path)
            val snap = ref.get().await()
            val existing = snap.toObject(HomeFiltersConfig::class.java)?.filters.orEmpty()
            val updated = existing.filterNot { it.equals(label.trim(), ignoreCase = true) }
            ref.set(
                mapOf(
                    "filters" to updated,
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Main Admin Power Reset: Deletes all user chat threads & messages, resets user inquiry flags,
     * and cleans up stale call sessions so Firebase storage and read load stay minimal.
     */
    suspend fun powerResetAllChats(): Result<Pair<Int, Int>> {
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can run Power Reset."
            }

            val chatsSnap = db.collection(ChatRepository.COLLECTION_CHATS).get().await()
            var deletedChats = 0
            var deletedMessages = 0
            val pendingDeletes = mutableListOf<com.google.firebase.firestore.DocumentReference>()

            for (chatDoc in chatsSnap.documents) {
                val msgSnap = chatDoc.reference.collection(ChatRepository.COLLECTION_MESSAGES).get().await()
                msgSnap.documents.forEach { msgDoc ->
                    pendingDeletes.add(msgDoc.reference)
                    deletedMessages++
                }
                pendingDeletes.add(chatDoc.reference)
                deletedChats++
            }

            pendingDeletes.chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }

            // Reset hasInquiryNotification on users so CRM badges match clean inbox
            runCatching {
                val notifiedUsers = db.collection(COLLECTION_USERS)
                    .whereEqualTo("hasInquiryNotification", true)
                    .get()
                    .await()
                notifiedUsers.documents.chunked(350).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { doc ->
                        batch.update(
                            doc.reference,
                            mapOf(
                                "hasInquiryNotification" to false,
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        )
                    }
                    batch.commit().await()
                }
            }

            // Clean up finished/stale call sessions cleanly
            runCatching {
                val callsSnap = db.collection(CallRepository.COLLECTION_CALLS).get().await()
                callsSnap.documents.chunked(350).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }

            Result.success(deletedChats to deletedMessages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, ChatRepository.COLLECTION_CHATS)
            Result.failure(e)
        }
    }

    suspend fun submitStaffApplication(
        staffRole: String,
        staffPrice: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection(COLLECTION_USERS).document(uid)
        return try {
            // Brief delay in case saveFullProfile was triggered concurrently right before applyForStaff
            kotlinx.coroutines.delay(450L)
            val snap = docRef.get().await()
            val account = snap.toObject(UserAccount::class.java)
                ?: return Result.failure(IllegalStateException("Please save your verification info first."))
            if (account.name.trim().length < 2 || account.phone.count { it.isDigit() } < 8 || account.age < 18) {
                return Result.failure(IllegalStateException("Please enter your full name, valid mobile number, and age (18+)."))
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
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            doc.toObject(
                                UserAccount::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.let { if (it.uid.isBlank()) it.copy(uid = doc.id) else it }
                        }.getOrNull()
                    }.sortedByDescending { it.lastActiveEpochMs }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_USERS)
                    }
                    emit(emptyList())
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
        directChatEnabled: Boolean = true,
    ): Result<String> {
        val docPath = "$COLLECTION_USERS/${user.uid}"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can approve Staff applications."
            }
            val safeTier = if (tier == ProfileTier.VIP_16_9) ProfileTier.VIP_16_9 else ProfileTier.GRID_9_16
            val profileId = user.staffProfileId.ifBlank { "staff_${user.uid.take(24)}" }
            val photos = buildList {
                if (user.photoUrl.startsWith("https://")) add(user.photoUrl)
                user.galleryUrls.filter { it.startsWith("https://") }.forEach { if (!contains(it)) add(it) }
                if (isEmpty()) {
                    add("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=85")
                }
            }

            val createResult = galleryRepository.createCompanionProfile(
                name = user.name.ifBlank { "DateMate Companion" },
                age = user.age.coerceIn(18, 80),
                price = user.staffPrice.ifBlank { "₹499" },
                role = user.staffRole.ifBlank { "GF" },
                category = if (safeTier == ProfileTier.VIP_16_9) "VIP" else "Verified Staff",
                tier = safeTier,
                bio = user.bio.ifBlank { "Verified DateMate Companion ready for dates." },
                photoUrls = photos,
                hiddenTimed = false,
                phone = user.phone,
                address = user.address,
                ownerUid = user.uid,
                staffVerified = true,
                staffOnline = true,
                customProfileId = profileId,
            )
            createResult.getOrThrow()

            db.document(docPath).update(
                mapOf(
                    "staffStatus" to StaffStatus.APPROVED,
                    "staffTier" to safeTier,
                    "staffProfileId" to profileId,
                    "directChatEnabled" to directChatEnabled,
                    "staffOnline" to true,
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

    suspend fun setStaffDirectChatAccess(userId: String, enabled: Boolean): Result<Unit> {
        val docPath = "$COLLECTION_USERS/$userId"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can toggle Staff direct client chat access."
            }
            require(userId.isNotBlank()) { "Invalid staff user ID" }
            db.document(docPath).update(
                mapOf(
                    "directChatEnabled" to enabled,
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

    suspend fun setStaffOnlineAvailability(online: Boolean): Result<Boolean> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val userDocPath = "$COLLECTION_USERS/$uid"
        var activePath = userDocPath
        return try {
            val userSnap = db.document(userDocPath).get().await()
            val account = userSnap.toObject(UserAccount::class.java)
                ?: return Result.failure(IllegalStateException("Staff account not found."))
            require(account.isApprovedStaff) {
                "Only verified DateMate Staff can toggle Online / Offline status."
            }

            db.document(userDocPath).update(
                mapOf(
                    "staffOnline" to online,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            val profileId = account.staffProfileId.trim()
            if (profileId.isNotBlank()) {
                val profilePath = "${GalleryRepository.COLLECTION_COMPANION_PROFILES}/$profileId"
                activePath = profilePath
                val profileRef = db.document(profilePath)
                val profileSnap = profileRef.get().await()
                if (profileSnap.exists()) {
                    profileRef.update(
                        mapOf(
                            "staffOnline" to online,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    ).await()
                }
            }

            val presencePath = "$COLLECTION_PRESENCE/$uid"
            activePath = presencePath
            runCatching {
                db.document(presencePath).set(
                    mapOf(
                        "online" to online,
                        "lastSeenEpochMs" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }

            Result.success(online)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, activePath)
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
        if (codeUsed == "PIN123456") return true
        if (!codeUsed.matches(ACCESS_CODE_REGEX)) return false
        val codeSnap = runCatching {
            db.collection(COLLECTION_ADMIN_CODES).document(codeUsed).get().await()
        }.getOrNull() ?: return false
        return codeSnap.exists() && (codeSnap.getBoolean("active") == true)
    }

    suspend fun ensurePinAdminUnlocked(): Boolean {
        if (checkIsCurrentUserAdmin()) return true
        val user = Firebase.auth.currentUser ?: return false
        val email = user.email?.trim()?.lowercase().orEmpty()
        if (!user.isEmailVerified || email.isEmpty()) return false
        val docRef = db.collection(COLLECTION_ADMINS).document(user.uid)
        return runCatching {
            val snap = docRef.get().await()
            val payload = hashMapOf<String, Any>(
                "name" to (user.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "DateMate Admin").take(80),
                "phone" to "9999999999",
                "email" to email.take(120),
                "codeUsed" to "PIN123456",
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (snap.exists() && snap.getTimestamp("createdAt") != null) {
                payload["createdAt"] = snap.getTimestamp("createdAt")!!
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
            true
        }.getOrDefault(false)
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
        if (clean == "PIN123456") return flow { emit(true) }
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

    // ========================================================================
    // Main Admin Custom App Share Link & Message
    // ========================================================================

    fun observeAppShareConfig(): Flow<AppShareConfig> {
        val ref = db.collection(COLLECTION_APP_CONFIG).document(DOC_SHARE_CONFIG)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) AppShareConfig()
                else snap.toObject(
                    AppShareConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AppShareConfig()
            }
            .catch { emit(AppShareConfig()) }
    }

    suspend fun saveAppShareConfig(shareUrl: String, shareMessage: String): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_SHARE_CONFIG"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can update the App Share Link."
            }
            val rawUrl = shareUrl.trim().take(500)
            val normalizedUrl = when {
                rawUrl.isBlank() -> ""
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                else -> "https://$rawUrl"
            }
            val cleanMessage = shareMessage.trim()
                .ifBlank { AppShareConfig.DEFAULT_SHARE_MESSAGE }
                .take(500)
            db.document(path).set(
                mapOf(
                    "shareUrl" to normalizedUrl,
                    "shareMessage" to cleanMessage,
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ========================================================================
    // Live App Update Manager (Option 1) & Home Live Promo Ticker
    // ========================================================================

    fun observeAppUpdateConfig(): Flow<AppUpdateConfig> {
        val ref = db.collection(COLLECTION_APP_CONFIG).document(DOC_APP_UPDATE)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) AppUpdateConfig()
                else snap.toObject(
                    AppUpdateConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: AppUpdateConfig()
            }
            .catch { emit(AppUpdateConfig()) }
    }

    suspend fun saveAppUpdateConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        apkDownloadUrl: String,
        releaseNotes: String,
        forceUpdate: Boolean,
    ): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_APP_UPDATE"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can publish App Updates."
            }
            val safeCode = latestVersionCode.coerceIn(1, 100_000)
            val safeName = latestVersionName.trim().ifBlank { "$safeCode.0" }.take(32)
            val rawUrl = apkDownloadUrl.trim().take(600)
            val normalizedUrl = when {
                rawUrl.isBlank() -> ""
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                else -> "https://$rawUrl"
            }
            if (normalizedUrl.isNotBlank()) {
                require(normalizedUrl.startsWith("https://") || normalizedUrl.startsWith("http://")) {
                    "Enter a valid https:// APK download link."
                }
            }
            val cleanNotes = releaseNotes.trim().take(600)
            db.document(path).set(
                mapOf(
                    "latestVersionCode" to safeCode,
                    "latestVersionName" to safeName,
                    "apkDownloadUrl" to normalizedUrl,
                    "releaseNotes" to cleanNotes,
                    "forceUpdate" to forceUpdate,
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observePromoTickerConfig(): Flow<PromoTickerConfig> {
        val ref = db.collection(COLLECTION_APP_CONFIG).document(DOC_PROMO_TICKER)
        return ref.snapshots()
            .map { snap ->
                if (!snap.exists()) PromoTickerConfig()
                else snap.toObject(
                    PromoTickerConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: PromoTickerConfig()
            }
            .catch { emit(PromoTickerConfig()) }
    }

    suspend fun savePromoTickerConfig(
        active: Boolean,
        badgeLabel: String,
        text: String,
    ): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_PROMO_TICKER"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin ($SUPER_ADMIN_EMAIL) can update the Live Offer Ticker."
            }
            val cleanBadge = badgeLabel.trim().ifBlank { "LIVE OFFER" }.take(32)
            val cleanText = text.trim().take(240)
            if (active) {
                require(cleanText.length >= 4) { "Enter offer/announcement text (min 4 characters)." }
            }
            db.document(path).set(
                mapOf(
                    "active" to active,
                    "badgeLabel" to cleanBadge,
                    "text" to cleanText,
                    "updatedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ========================================================================
    // Co-Admin -> Main Admin Change Request Workflow
    // ========================================================================

    fun observeAllChangeRequestsForMainAdmin(): Flow<List<AdminChangeRequest>> = flow {
        requireSignedInUid()
        emitAll(
            db.collection(COLLECTION_ADMIN_CHANGE_REQUESTS)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            doc.toObject(
                                AdminChangeRequest::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                        }.getOrNull()
                    }.sortedWith(
                        compareBy<AdminChangeRequest> { it.status != AdminChangeRequestStatus.PENDING }
                            .thenByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_ADMIN_CHANGE_REQUESTS)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeMyChangeRequests(uid: String): Flow<List<AdminChangeRequest>> = flow {
        if (uid.isBlank() || Firebase.auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(COLLECTION_ADMIN_CHANGE_REQUESTS)
                .whereEqualTo("requesterUid", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            doc.toObject(
                                AdminChangeRequest::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
                        }.getOrNull()
                    }.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (firestoreError != null) {
                        handleFirestoreError(firestoreError, OperationType.LIST, COLLECTION_ADMIN_CHANGE_REQUESTS)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun submitAdminChangeRequest(
        requestType: String,
        targetId: String,
        targetTitle: String,
        details: String,
    ): Result<Unit> {
        val uid = runCatching { requireSignedInUid() }.getOrElse { return Result.failure(it) }
        val requestId = "req_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val path = "$COLLECTION_ADMIN_CHANGE_REQUESTS/$requestId"
        return try {
            val authUser = Firebase.auth.currentUser
            val cleanTitle = targetTitle.trim().take(100)
            val cleanDetails = details.trim().take(1000)
            require(cleanTitle.isNotBlank()) { "Please enter a title / profile name for your request." }
            require(cleanDetails.length >= 4) { "Please describe the change needed (minimum 4 characters)." }
            val safeType = if (AdminChangeRequestType.ALL_OPTIONS.any { it.first == requestType }) {
                requestType
            } else {
                AdminChangeRequestType.OTHER
            }
            val requesterName = authUser?.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Co-Admin"
            val requesterEmail = authUser?.email?.trim()?.lowercase().orEmpty().take(120)

            db.document(path).set(
                mapOf(
                    "requesterUid" to uid,
                    "requesterName" to requesterName.take(80),
                    "requesterEmail" to requesterEmail,
                    "requestType" to safeType,
                    "targetId" to targetId.trim().take(128),
                    "targetTitle" to cleanTitle,
                    "details" to cleanDetails,
                    "status" to AdminChangeRequestStatus.PENDING,
                    "mainAdminNote" to "",
                    "reviewedByEmail" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
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

    suspend fun reviewAdminChangeRequest(
        requestId: String,
        approve: Boolean,
        mainAdminNote: String,
    ): Result<Unit> {
        val path = "$COLLECTION_ADMIN_CHANGE_REQUESTS/$requestId"
        return try {
            val currentEmail = Firebase.auth.currentUser?.email?.trim()?.lowercase()
                ?: error("Sign in as Main Admin first")
            require(currentEmail == SUPER_ADMIN_EMAIL) {
                "Only Main Admin can review Co-Admin change requests."
            }
            require(requestId.isNotBlank()) { "Invalid request ID" }
            val nextStatus = if (approve) AdminChangeRequestStatus.APPROVED else AdminChangeRequestStatus.REJECTED
            db.document(path).update(
                mapOf(
                    "status" to nextStatus,
                    "mainAdminNote" to mainAdminNote.trim().take(500),
                    "reviewedByEmail" to currentEmail,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ADMINS = "admins"
        const val COLLECTION_ADMIN_EMAILS = "admin_emails"
        const val COLLECTION_ADMIN_CODES = "admin_access_codes"
        const val COLLECTION_ADMIN_CHANGE_REQUESTS = "admin_change_requests"
        const val COLLECTION_PRESENCE = "presence"
        const val COLLECTION_APP_CONFIG = "app_config"
        const val DOC_LAUNCH_BANNER = "launch_banner"
        const val DOC_HOME_FILTERS = "home_filters"
        const val DOC_SHARE_CONFIG = "share_config"
        const val DOC_APP_UPDATE = "app_update"
        const val DOC_PROMO_TICKER = "promo_ticker"
        const val ADMIN_PRESENCE_ID = "admin_concierge"
        const val SUPER_ADMIN_EMAIL = "buildolive4@gmail.com"
        private val ACCESS_CODE_REGEX = Regex("^[A-Z0-9_\\-]{4,24}$")

        fun normalizeAccessCode(raw: String): String =
            raw.trim().uppercase().replace(Regex("[^A-Z0-9_\\-]"), "")
    }
}
