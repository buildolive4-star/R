package com.example.ui.booking

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BookingLifecycleStatus
import com.example.data.model.BookingRatePlan
import com.example.data.model.DateMateBooking
import com.example.data.model.FilterCategoryGroup
import com.example.data.model.GalleryImage
import com.example.data.model.PaymentServerConfig
import com.example.data.model.UserAccount
import com.example.data.model.UserComplaint
import com.example.data.repository.BookingRepository
import com.example.notifications.PixoraNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _currentUid = MutableStateFlow(Firebase.auth.currentUser?.uid.orEmpty())

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUid.value = auth.currentUser?.uid.orEmpty()
    }

    init {
        Firebase.auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        Firebase.auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _pendingPaymentBookingId = MutableStateFlow<String?>(null)
    val pendingPaymentBookingId: StateFlow<String?> = _pendingPaymentBookingId.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val _navigateToBookings = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val navigateToBookings: SharedFlow<Unit> = _navigateToBookings.asSharedFlow()

    val filterTaxonomy: StateFlow<List<FilterCategoryGroup>> = bookingRepository.observeFilterTaxonomy()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = FilterCategoryGroup.defaultTaxonomy(),
        )

    val paymentServerConfig: StateFlow<PaymentServerConfig> = bookingRepository.observePaymentServerConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PaymentServerConfig(),
        )

    val clientBookings: StateFlow<List<DateMateBooking>> = _currentUid
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList()) else bookingRepository.observeClientBookings(uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val staffBookings: StateFlow<List<DateMateBooking>> = _currentUid
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList()) else bookingRepository.observeStaffBookings(uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val myComplaints: StateFlow<List<UserComplaint>> = _currentUid
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList()) else bookingRepository.observeMyComplaints(uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    fun startBookingAndOpenChrome(
        context: Context,
        profile: GalleryImage,
        selectedPlan: BookingRatePlan,
        clientAccount: UserAccount?,
        clientName: String,
        clientPhone: String,
        dateLabel: String,
        timeSlot: String,
        meetingLocation: String,
        notes: String,
        onBookingCreated: () -> Unit,
    ) {
        val serverUrl = paymentServerConfig.value.cleanBaseUrl
        if (serverUrl.isBlank()) {
            _events.tryEmit("Payment Server URL is not configured yet. Ask Admin to set Render URL in Admin Studio → Bookings.")
            return
        }
        if (Firebase.auth.currentUser == null) {
            _events.tryEmit("Please sign in with Google from Profile first to book a date.")
            return
        }
        val advanceAmount = profile.resolvedAdvanceAmountInr(paymentServerConfig.value.defaultAdvanceInr)
        viewModelScope.launch {
            _isBusy.value = true
            val res = bookingRepository.createAwaitingPaymentBooking(
                profile = profile,
                selectedPlan = selectedPlan,
                advanceAmountInr = advanceAmount,
                clientAccount = clientAccount,
                clientNameInput = clientName,
                clientPhoneInput = clientPhone,
                dateLabel = dateLabel,
                timeSlot = timeSlot,
                meetingLocation = meetingLocation,
                notes = notes,
            )
            _isBusy.value = false
            res.onSuccess { booking ->
                _pendingPaymentBookingId.value = booking.id
                onBookingCreated()
                val checkoutUrl = bookingRepository.buildChromeCheckoutUrl(serverUrl, booking)
                openUrlInChrome(context, checkoutUrl)
                _events.tryEmit("Complete ₹${booking.advanceAmountInr} pre-booking advance in Chrome to confirm your date!")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Unable to start booking. Please try again.")
            }
        }
    }

    fun reopenChromePaymentForBooking(context: Context, booking: DateMateBooking) {
        val serverUrl = paymentServerConfig.value.cleanBaseUrl
        if (serverUrl.isBlank()) {
            _events.tryEmit("Payment Server URL is not configured in Admin Studio.")
            return
        }
        _pendingPaymentBookingId.value = booking.id
        val checkoutUrl = bookingRepository.buildChromeCheckoutUrl(serverUrl, booking)
        openUrlInChrome(context, checkoutUrl)
    }

    fun handlePaymentDeepLinkUri(context: Context, uri: Uri) {
        if (uri.scheme != "datemate" || uri.host != "payment-result") return
        val status = uri.getQueryParameter("status").orEmpty()
        val bookingId = uri.getQueryParameter("bookingId").orEmpty()
        val paymentId = uri.getQueryParameter("paymentId").orEmpty()
        val orderId = uri.getQueryParameter("orderId").orEmpty()
        if (bookingId.isBlank()) return

        if (status.equals("success", ignoreCase = true)) {
            viewModelScope.launch {
                _isBusy.value = true
                // Verify on server when URL is present, and confirm in Firestore
                val serverUrl = paymentServerConfig.value.cleanBaseUrl
                var resolvedPaymentId = paymentId
                var resolvedOrderId = orderId
                if (serverUrl.isNotBlank()) {
                    val serverCheck = bookingRepository.verifyPaymentStatusOnServer(serverUrl, bookingId)
                    serverCheck.onSuccess { info ->
                        if (info.paymentId.isNotBlank()) resolvedPaymentId = info.paymentId
                        if (info.orderId.isNotBlank()) resolvedOrderId = info.orderId
                    }
                }
                val confirmRes = bookingRepository.confirmBookingPayment(
                    bookingId = bookingId,
                    paymentId = resolvedPaymentId.ifBlank { "rzp_confirmed" },
                    orderId = resolvedOrderId,
                )
                _isBusy.value = false
                if (_pendingPaymentBookingId.value == bookingId) {
                    _pendingPaymentBookingId.value = null
                }
                confirmRes.onSuccess {
                    val booking = clientBookings.value.firstOrNull { it.id == bookingId }
                    PixoraNotificationManager.notifyBookingConfirmed(
                        context = context,
                        bookingId = bookingId,
                        companionName = booking?.profileName?.ifBlank { "Your Companion" } ?: "Your Companion",
                        planLabel = booking?.planLabel?.ifBlank { "Date" } ?: "Date",
                        advanceInr = booking?.advanceAmountInr ?: paymentServerConfig.value.defaultAdvanceInr,
                    )
                    _events.tryEmit("Pre-booking Advance Paid ✓ Your date is confirmed!")
                    _navigateToBookings.tryEmit(Unit)
                }.onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "Payment received — tap Verify Payment in Bookings.")
                    _navigateToBookings.tryEmit(Unit)
                }
            }
        } else {
            _events.tryEmit("Payment was not completed. Complete advance payment to confirm your booking.")
            _navigateToBookings.tryEmit(Unit)
        }
    }

    fun verifyPendingPaymentIfAny(context: Context) {
        val pendingId = _pendingPaymentBookingId.value ?: return
        verifyBookingPayment(context, pendingId, silentIfUnpaid = true)
    }

    fun verifyBookingPayment(
        context: Context,
        bookingId: String,
        silentIfUnpaid: Boolean = false,
    ) {
        val serverUrl = paymentServerConfig.value.cleanBaseUrl
        if (serverUrl.isBlank()) {
            if (!silentIfUnpaid) _events.tryEmit("Payment server URL not configured by Admin.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val statusRes = bookingRepository.verifyPaymentStatusOnServer(serverUrl, bookingId)
            statusRes.onSuccess { info ->
                if (info.paid) {
                    val confirmRes = bookingRepository.confirmBookingPayment(
                        bookingId = bookingId,
                        paymentId = info.paymentId.ifBlank { "rzp_verified" },
                        orderId = info.orderId,
                    )
                    _isBusy.value = false
                    if (_pendingPaymentBookingId.value == bookingId) {
                        _pendingPaymentBookingId.value = null
                    }
                    confirmRes.onSuccess {
                        val booking = clientBookings.value.firstOrNull { it.id == bookingId }
                        PixoraNotificationManager.notifyBookingConfirmed(
                            context = context,
                            bookingId = bookingId,
                            companionName = booking?.profileName?.ifBlank { "Companion" } ?: "Companion",
                            planLabel = booking?.planLabel?.ifBlank { "Date" } ?: "Date",
                            advanceInr = booking?.advanceAmountInr ?: paymentServerConfig.value.defaultAdvanceInr,
                        )
                        _events.tryEmit("Payment Verified ✓ Booking Confirmed!")
                    }.onFailure { err ->
                        _events.tryEmit(err.localizedMessage ?: "Could not save confirmation")
                    }
                } else {
                    _isBusy.value = false
                    if (!silentIfUnpaid) {
                        _events.tryEmit("Payment still pending. Tap Pay Advance in Chrome to finish checkout.")
                    }
                }
            }.onFailure { err ->
                _isBusy.value = false
                if (!silentIfUnpaid) {
                    _events.tryEmit(err.localizedMessage ?: "Could not reach payment server right now")
                }
            }
        }
    }

    fun markStaffAttended(booking: DateMateBooking) {
        if (!booking.advancePaid || booking.staffAttended) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = bookingRepository.markStaffAttended(booking.id)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Marked Attended ✓ Client has been notified to submit Rating & Feedback!")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not update attendance")
            }
        }
    }

    fun submitRatingAndFeedback(
        booking: DateMateBooking,
        rating: Int,
        feedback: String,
    ) {
        if (booking.bookingStatus == BookingLifecycleStatus.COMPLETED) return
        viewModelScope.launch {
            _isBusy.value = true
            val virtualMoney = booking.effectiveVirtualEarningInr
            val res = bookingRepository.submitClientRatingAndFeedback(
                bookingId = booking.id,
                rating = rating,
                feedback = feedback,
                virtualEarningInr = virtualMoney,
            )
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Thank you for rating ${booking.profileName}! Date completed ⭐")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Failed to submit rating")
            }
        }
    }

    fun submitUserComplaint(
        userAccount: UserAccount?,
        subject: String,
        message: String,
        onSubmitted: () -> Unit = {},
    ) {
        if (Firebase.auth.currentUser == null) {
            _events.tryEmit("Please sign in with Google first to submit a complaint.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = bookingRepository.submitComplaint(userAccount, subject, message)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("Complaint submitted ✓ Admin team will review & reply soon.")
                onSubmitted()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "Could not submit complaint")
            }
        }
    }

    private fun openUrlInChrome(context: Context, url: String) {
        val uri = Uri.parse(url)
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.android.chrome")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chromeIntent)
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                _events.tryEmit("No web browser found to open payment page.")
            }
        }
    }
}
