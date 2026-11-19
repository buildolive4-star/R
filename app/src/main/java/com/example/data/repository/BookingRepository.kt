package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BookingLifecycleStatus
import com.example.data.model.BookingRatePlan
import com.example.data.model.DateMateBooking
import com.example.data.model.FilterCategoryGroup
import com.example.data.model.FilterTaxonomyConfig
import com.example.data.model.GalleryImage
import com.example.data.model.PaymentServerConfig
import com.example.data.model.PayoutStatus
import com.example.data.model.UserAccount
import com.example.data.model.UserComplaint
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ServerPaymentStatus(
    val paid: Boolean,
    val status: String,
    val paymentId: String,
    val orderId: String,
    val amountInr: Int,
)

class BookingRepository(
    private val db: FirebaseFirestore,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun requireUid(): String =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google first.")

    private fun requireEmail(): String =
        Firebase.auth.currentUser?.email?.trim()?.lowercase().orEmpty()

    // -------------------------------------------------------------------------
    // Admin Dynamic Filter Categories & Options (app_config/filter_taxonomy)
    // -------------------------------------------------------------------------

    fun observeFilterTaxonomy(): Flow<List<FilterCategoryGroup>> =
        db.collection(COLLECTION_APP_CONFIG)
            .document(DOC_FILTER_TAXONOMY)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    FilterCategoryGroup.defaultTaxonomy()
                } else {
                    val cfg = snapshot.toObject(
                        FilterTaxonomyConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    val parsed = FilterCategoryGroup.parseList(cfg?.groups.orEmpty())
                    if (parsed.isEmpty()) FilterCategoryGroup.defaultTaxonomy() else parsed
                }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(FilterCategoryGroup.defaultTaxonomy())
            }

    suspend fun saveFilterTaxonomy(groups: List<FilterCategoryGroup>): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_FILTER_TAXONOMY"
        return try {
            requireUid()
            val encoded = groups
                .filter { it.category.isNotBlank() }
                .take(20)
                .map { it.encode() }
            val payload = mapOf(
                "groups" to encoded,
                "updatedByEmail" to requireEmail().take(120),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Payment Server Configuration (app_config/payment_config)
    // -------------------------------------------------------------------------

    fun observePaymentServerConfig(): Flow<PaymentServerConfig> =
        db.collection(COLLECTION_APP_CONFIG)
            .document(DOC_PAYMENT_CONFIG)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    PaymentServerConfig()
                } else {
                    val parsed = snapshot.toObject(
                        PaymentServerConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: PaymentServerConfig()
                    if (parsed.serverBaseUrl.isBlank()) {
                        parsed.copy(serverBaseUrl = PaymentServerConfig.DEFAULT_PAYMENT_SERVER_URL)
                    } else {
                        parsed
                    }
                }
            }
            .catch { error ->
                val fsErr = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fsErr == null && error is CancellationException) throw error
                emit(PaymentServerConfig())
            }

    suspend fun savePaymentServerConfig(
        serverBaseUrl: String,
        defaultAdvanceInr: Int,
    ): Result<Unit> {
        val path = "$COLLECTION_APP_CONFIG/$DOC_PAYMENT_CONFIG"
        return try {
            requireUid()
            val trimmed = serverBaseUrl.trim().trimEnd('/').take(300)
            require(trimmed.isEmpty() || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                "Server URL must start with https://"
            }
            val safeAdvance = defaultAdvanceInr.coerceIn(1, 500_000)
            val payload = mapOf(
                "serverBaseUrl" to trimmed,
                "defaultAdvanceInr" to safeAdvance,
                "updatedByEmail" to requireEmail().take(120),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Bookings CRUD & Lifecycle (bookings/{bookingId})
    // -------------------------------------------------------------------------

    fun observeClientBookings(clientUid: String): Flow<List<DateMateBooking>> = flow {
        if (clientUid.isBlank() || Firebase.auth.currentUser?.uid != clientUid) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("clientUid", clientUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DateMateBooking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && Firebase.auth.currentUser?.uid == clientUid) {
                        handleFirestoreError(toLog, OperationType.LIST, COLLECTION_BOOKINGS)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeStaffBookings(staffUid: String): Flow<List<DateMateBooking>> = flow {
        if (staffUid.isBlank() || Firebase.auth.currentUser?.uid != staffUid) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(COLLECTION_BOOKINGS)
                .whereEqualTo("staffOwnerUid", staffUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DateMateBooking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.advancePaid || it.bookingStatus != BookingLifecycleStatus.AWAITING_PAYMENT }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && Firebase.auth.currentUser?.uid == staffUid) {
                        handleFirestoreError(toLog, OperationType.LIST, COLLECTION_BOOKINGS)
                    }
                    emit(emptyList())
                }
        )
    }

    fun observeAllBookingsForAdmin(): Flow<List<DateMateBooking>> = flow {
        requireUid()
        emitAll(
            db.collection(COLLECTION_BOOKINGS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DateMateBooking::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null) {
                        handleFirestoreError(toLog, OperationType.LIST, COLLECTION_BOOKINGS)
                    }
                    emit(emptyList())
                }
        )
    }

    suspend fun createAwaitingPaymentBooking(
        profile: GalleryImage,
        selectedPlan: BookingRatePlan,
        advanceAmountInr: Int,
        clientAccount: UserAccount?,
        clientNameInput: String,
        clientPhoneInput: String,
        dateLabel: String,
        timeSlot: String,
        meetingLocation: String,
        notes: String,
    ): Result<DateMateBooking> {
        val bookingId = "bk_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "$COLLECTION_BOOKINGS/$bookingId"
        return try {
            val uid = requireUid()
            val safeClientName = clientNameInput.trim()
                .ifBlank { clientAccount?.name.orEmpty() }
                .ifBlank { Firebase.auth.currentUser?.displayName.orEmpty() }
                .ifBlank { "DateMate Guest" }
                .take(80)
            val safePhone = clientPhoneInput.trim().ifBlank { clientAccount?.phone.orEmpty() }.take(24)
            val safeDate = dateLabel.trim().ifBlank { "Today" }.take(48)
            val safeTime = timeSlot.trim().ifBlank { "7:00 PM" }.take(48)
            val safeLocation = meetingLocation.trim().take(240)
            require(safeLocation.isNotBlank()) { "Meeting location is required" }
            val safeAdvance = advanceAmountInr.coerceIn(1, 500_000)

            val payload = hashMapOf<String, Any>(
                "clientUid" to uid,
                "clientName" to safeClientName,
                "clientPhone" to safePhone,
                "clientPhotoUrl" to clientAccount?.photoUrl.orEmpty().take(1000),
                "profileId" to profile.id.take(128),
                "profileName" to profile.title.ifBlank { "Companion" }.take(80),
                "profilePhotoUrl" to profile.displayThumbnailUrl.take(1000),
                "staffOwnerUid" to profile.ownerUid.trim().take(128),
                "planLabel" to selectedPlan.label.take(40),
                "planPrice" to selectedPlan.priceText.take(32),
                "planNumericAmount" to selectedPlan.numericPrice.coerceIn(0, 500_000),
                "advanceAmountInr" to safeAdvance,
                "advancePaid" to false,
                "paymentId" to "",
                "orderId" to "",
                "dateLabel" to safeDate,
                "timeSlot" to safeTime,
                "meetingLocation" to safeLocation,
                "notes" to notes.trim().take(400),
                "bookingStatus" to BookingLifecycleStatus.AWAITING_PAYMENT,
                "staffAttended" to false,
                "staffAttendedAtEpochMs" to 0L,
                "userRating" to 0,
                "userFeedback" to "",
                "ratedAtEpochMs" to 0L,
                "virtualEarningInr" to 0,
                "payoutStatus" to PayoutStatus.PENDING,
                "payoutNote" to "",
                "payoutUpdatedByEmail" to "",
                "payoutUpdatedAtEpochMs" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )

            db.document(path).set(payload).await()

            Result.success(
                DateMateBooking(
                    id = bookingId,
                    clientUid = uid,
                    clientName = safeClientName,
                    clientPhone = safePhone,
                    clientPhotoUrl = clientAccount?.photoUrl.orEmpty(),
                    profileId = profile.id,
                    profileName = profile.title,
                    profilePhotoUrl = profile.displayThumbnailUrl,
                    staffOwnerUid = profile.ownerUid,
                    planLabel = selectedPlan.label,
                    planPrice = selectedPlan.priceText,
                    planNumericAmount = selectedPlan.numericPrice,
                    advanceAmountInr = safeAdvance,
                    advancePaid = false,
                    dateLabel = safeDate,
                    timeSlot = safeTime,
                    meetingLocation = safeLocation,
                    notes = notes.trim(),
                    bookingStatus = BookingLifecycleStatus.AWAITING_PAYMENT,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun confirmBookingPayment(
        bookingId: String,
        paymentId: String,
        orderId: String,
    ): Result<Unit> {
        val path = "$COLLECTION_BOOKINGS/$bookingId"
        return try {
            requireUid()
            require(bookingId.isNotBlank()) { "Booking ID required" }
            db.document(path).update(
                mapOf(
                    "advancePaid" to true,
                    "paymentId" to paymentId.trim().take(120),
                    "orderId" to orderId.trim().take(120),
                    "bookingStatus" to BookingLifecycleStatus.CONFIRMED,
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

    suspend fun markStaffAttended(bookingId: String): Result<Unit> {
        val path = "$COLLECTION_BOOKINGS/$bookingId"
        return try {
            requireUid()
            require(bookingId.isNotBlank()) { "Booking ID required" }
            db.document(path).update(
                mapOf(
                    "staffAttended" to true,
                    "staffAttendedAtEpochMs" to System.currentTimeMillis(),
                    "bookingStatus" to BookingLifecycleStatus.AWAITING_RATING,
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

    suspend fun submitClientRatingAndFeedback(
        bookingId: String,
        rating: Int,
        feedback: String,
        virtualEarningInr: Int,
    ): Result<Unit> {
        val path = "$COLLECTION_BOOKINGS/$bookingId"
        return try {
            requireUid()
            require(bookingId.isNotBlank()) { "Booking ID required" }
            val safeRating = rating.coerceIn(1, 5)
            val safeVirtual = virtualEarningInr.coerceIn(0, 500_000)
            db.document(path).update(
                mapOf(
                    "userRating" to safeRating,
                    "userFeedback" to feedback.trim().take(500),
                    "ratedAtEpochMs" to System.currentTimeMillis(),
                    "bookingStatus" to BookingLifecycleStatus.COMPLETED,
                    "virtualEarningInr" to safeVirtual,
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

    suspend fun updatePayoutStatusBySuperAdmin(
        bookingId: String,
        payoutStatus: String,
        note: String,
    ): Result<Unit> {
        val path = "$COLLECTION_BOOKINGS/$bookingId"
        return try {
            requireUid()
            require(bookingId.isNotBlank()) { "Booking ID required" }
            val safeStatus = when (payoutStatus) {
                PayoutStatus.PAID -> PayoutStatus.PAID
                PayoutStatus.REJECTED -> PayoutStatus.REJECTED
                else -> PayoutStatus.PENDING
            }
            db.document(path).update(
                mapOf(
                    "payoutStatus" to safeStatus,
                    "payoutNote" to note.trim().take(240),
                    "payoutUpdatedByEmail" to requireEmail().take(120),
                    "payoutUpdatedAtEpochMs" to System.currentTimeMillis(),
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

    // -------------------------------------------------------------------------
    // User Complaints Box (complaints/{complaintId})
    // -------------------------------------------------------------------------

    fun observeMyComplaints(userUid: String): Flow<List<UserComplaint>> = flow {
        if (userUid.isBlank() || Firebase.auth.currentUser?.uid != userUid) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(COLLECTION_COMPLAINTS)
                .whereEqualTo("userUid", userUid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserComplaint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        )
    }

    fun observeAllComplaintsForAdmin(): Flow<List<UserComplaint>> = flow {
        requireUid()
        emitAll(
            db.collection(COLLECTION_COMPLAINTS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        UserComplaint::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        )
    }

    suspend fun submitComplaint(
        userAccount: UserAccount?,
        subject: String,
        message: String,
    ): Result<Unit> {
        val complaintId = "cmp_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"
        val path = "$COLLECTION_COMPLAINTS/$complaintId"
        return try {
            val uid = requireUid()
            val cleanSubject = subject.trim().take(120)
            val cleanMessage = message.trim().take(1000)
            require(cleanSubject.length >= 2) { "Please enter a brief subject" }
            require(cleanMessage.length >= 4) { "Please describe your issue" }

            val payload = mapOf(
                "userUid" to uid,
                "userName" to (userAccount?.name?.ifBlank { Firebase.auth.currentUser?.displayName.orEmpty() }
                    ?.ifBlank { "DateMate User" } ?: "DateMate User").take(80),
                "userEmail" to (userAccount?.email?.ifBlank { requireEmail() } ?: requireEmail()).take(120),
                "userPhone" to userAccount?.phone.orEmpty().take(24),
                "subject" to cleanSubject,
                "message" to cleanMessage,
                "status" to "open",
                "adminReply" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            db.document(path).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveComplaint(complaintId: String, adminReply: String): Result<Unit> {
        val path = "$COLLECTION_COMPLAINTS/$complaintId"
        return try {
            requireUid()
            db.document(path).update(
                mapOf(
                    "status" to "resolved",
                    "adminReply" to adminReply.trim().take(600),
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

    // -------------------------------------------------------------------------
    // Render Razorpay Payment Server HTTP Helpers
    // -------------------------------------------------------------------------

    fun buildChromeCheckoutUrl(baseUrl: String, booking: DateMateBooking): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val dateSlotText = "${booking.dateLabel} · ${booking.timeSlot}"
        return Uri.parse("$cleanBase/datemate-pay").buildUpon()
            .appendQueryParameter("bookingId", booking.id)
            .appendQueryParameter("amount", booking.advanceAmountInr.toString())
            .appendQueryParameter("profileName", booking.profileName)
            .appendQueryParameter("planName", booking.planLabel)
            .appendQueryParameter("totalPrice", booking.planPrice)
            .appendQueryParameter("userName", booking.clientName)
            .appendQueryParameter("userPhone", booking.clientPhone)
            .appendQueryParameter("dateSlot", dateSlotText)
            .build()
            .toString()
    }

    suspend fun checkPaymentServerHealth(baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            require(cleanBase.startsWith("https://") || cleanBase.startsWith("http://")) {
                "Enter a valid https:// Render server URL first"
            }
            val request = Request.Builder()
                .url("$cleanBase/datemate/health")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Server responded HTTP ${response.code}")
                    )
                }
                val json = runCatching { JSONObject(body) }.getOrNull()
                val razorpayReady = json?.optBoolean("razorpayConfigured", false) ?: false
                val msg = if (razorpayReady) {
                    "Connected ✓ Razorpay Live Server Ready!"
                } else {
                    "Server Online ✓ (Check RAZORPAY_KEY_ID/SECRET env vars on Render)"
                }
                Result.success(msg)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPaymentStatusOnServer(
        baseUrl: String,
        bookingId: String,
    ): Result<ServerPaymentStatus> = withContext(Dispatchers.IO) {
        try {
            val cleanBase = baseUrl.trim().trimEnd('/')
            if (cleanBase.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Payment server URL not configured by Admin"))
            }
            val request = Request.Builder()
                .url("$cleanBase/datemate/payment-status/${Uri.encode(bookingId)}")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}"))
                }
                val json = JSONObject(body)
                Result.success(
                    ServerPaymentStatus(
                        paid = json.optBoolean("paid", false),
                        status = json.optString("status", "pending"),
                        paymentId = json.optString("paymentId", ""),
                        orderId = json.optString("orderId", ""),
                        amountInr = json.optInt("amountInr", 0),
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val COLLECTION_APP_CONFIG = "app_config"
        const val DOC_FILTER_TAXONOMY = "filter_taxonomy"
        const val DOC_PAYMENT_CONFIG = "payment_config"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_COMPLAINTS = "complaints"
    }
}
