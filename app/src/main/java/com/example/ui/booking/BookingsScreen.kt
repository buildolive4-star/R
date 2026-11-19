package com.example.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingLifecycleStatus
import com.example.data.model.DateMateBooking
import com.example.data.model.PayoutStatus
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.Animated3DPageBackdrop
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.PixoraNetworkImage

@Composable
fun BookingsScreen(
    clientBookings: List<DateMateBooking>,
    staffBookings: List<DateMateBooking>,
    userAccount: UserAccount?,
    isSignedIn: Boolean,
    isBusy: Boolean,
    onPayAdvanceInChrome: (DateMateBooking) -> Unit,
    onVerifyPayment: (DateMateBooking) -> Unit,
    onMarkStaffAttended: (DateMateBooking) -> Unit,
    onSubmitRating: (DateMateBooking, Int, String) -> Unit,
    onExploreClick: () -> Unit,
    onGoToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isApprovedStaff = userAccount?.staffStatus == StaffStatus.APPROVED
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Animated3DPageBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("bookings_screen")
        ) {
            // Top Title + RoyalMate Mascot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Animated3DMascotOrb(
                    mascot = DateMateMascot.ROYAL_MATE,
                    size = 48.dp,
                    showOrbitSparkles = true,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date Bookings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pre-bookings, Attendance, Ratings & Staff Virtual Wallet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "My Bookings (${clientBookings.size})",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("bookings_tab_client")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isApprovedStaff) "Staff & Wallet (${staffBookings.size})" else "Staff & Wallet",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.testTag("bookings_tab_staff")
                )
            }

            if (!isSignedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyGalleryState(
                        title = "Sign In to View Bookings",
                        subtitle = "Sign in with Google from your Profile tab to book companions, track confirmed dates, or manage Staff Virtual Money.",
                        actionLabel = "Go to Profile",
                        onAction = onGoToProfile,
                        mascot = DateMateMascot.WELCOME_GIRL,
                    )
                }
            } else if (selectedTab == 0) {
                // MY BOOKINGS (Client)
                if (clientBookings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyGalleryState(
                            title = "No Bookings Yet",
                            subtitle = "Open any companion profile, choose a rate plan (Dinner / Night / Coffee), and pay the pre-booking advance in Chrome to confirm your date.",
                            actionLabel = "Browse Companions",
                            onAction = onExploreClick,
                            mascot = DateMateMascot.CUPID,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(clientBookings, key = { it.id }) { booking ->
                            ClientBookingCard(
                                booking = booking,
                                isBusy = isBusy,
                                onPayInChrome = { onPayAdvanceInChrome(booking) },
                                onVerifyPayment = { onVerifyPayment(booking) },
                                onSubmitRating = { stars, feedback ->
                                    onSubmitRating(booking, stars, feedback)
                                }
                            )
                        }
                    }
                }
            } else {
                // STAFF BOOKINGS & VIRTUAL MONEY
                if (!isApprovedStaff) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyGalleryState(
                            title = "Staff Bookings & Virtual Wallet",
                            subtitle = "Become verified Companion Staff from your Profile tab to receive paid bookings, mark attendance, and earn Virtual Money approved by Main Admin.",
                            actionLabel = "Apply in Profile",
                            onAction = onGoToProfile,
                            mascot = DateMateMascot.SAFETY_MATE,
                        )
                    }
                } else {
                    val completedStaffBookings = remember(staffBookings) {
                        staffBookings.filter { it.isCompleted }
                    }
                    val pendingVirtualInr = remember(completedStaffBookings) {
                        completedStaffBookings
                            .filter { it.payoutStatus == PayoutStatus.PENDING }
                            .sumOf { it.effectiveVirtualEarningInr }
                    }
                    val paidVirtualInr = remember(completedStaffBookings) {
                        completedStaffBookings
                            .filter { it.payoutStatus == PayoutStatus.PAID }
                            .sumOf { it.effectiveVirtualEarningInr }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(key = "staff_wallet_summary") {
                            StaffVirtualMoneyCard(
                                pendingVirtualInr = pendingVirtualInr,
                                paidVirtualInr = paidVirtualInr,
                                completedCount = completedStaffBookings.size,
                                activeCount = staffBookings.count { !it.isCompleted },
                            )
                        }

                        if (staffBookings.isEmpty()) {
                            item(key = "staff_bookings_empty") {
                                EmptyGalleryState(
                                    title = "No Confirmed Client Bookings Yet",
                                    subtitle = "When a client pays the pre-booking advance for your profile, their full date timing, venue, and contact details appear right here.",
                                    mascot = DateMateMascot.LOVE_MATE,
                                )
                            }
                        } else {
                            items(staffBookings, key = { it.id }) { booking ->
                                StaffBookingCard(
                                    booking = booking,
                                    isBusy = isBusy,
                                    onMarkAttended = { onMarkStaffAttended(booking) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientBookingCard(
    booking: DateMateBooking,
    isBusy: Boolean,
    onPayInChrome: () -> Unit,
    onVerifyPayment: () -> Unit,
    onSubmitRating: (stars: Int, feedback: String) -> Unit,
) {
    var selectedStars by remember(booking.id) { mutableIntStateOf(5) }
    var feedbackText by remember(booking.id) { mutableStateOf("") }

    val (statusLabel, statusColor) = when (booking.bookingStatus) {
        BookingLifecycleStatus.AWAITING_PAYMENT -> "Advance Payment Pending" to Color(0xFFD97706)
        BookingLifecycleStatus.CONFIRMED -> "Confirmed · Advance Paid ✓" to Color(0xFF16A34A)
        BookingLifecycleStatus.AWAITING_RATING -> "Date Attended · Rate Now ⭐" to MaterialTheme.colorScheme.primary
        BookingLifecycleStatus.COMPLETED -> "Completed ✓" to Color(0xFF0F766E)
        else -> booking.bookingStatus.uppercase() to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, statusColor.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_booking_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixoraNetworkImage(
                    imageUrl = booking.profilePhotoUrl,
                    contentDescription = booking.profileName,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = booking.profileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${booking.planLabel} · ${booking.planPrice} (Advance ₹${booking.advanceAmountInr})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Booking Details Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BookingDetailRow(Icons.Rounded.CalendarMonth, "Date", booking.dateLabel)
                    BookingDetailRow(Icons.Rounded.Schedule, "Timing", booking.timeSlot)
                    BookingDetailRow(Icons.Rounded.LocationOn, "Meeting Venue", booking.meetingLocation)
                    if (booking.paymentId.isNotBlank()) {
                        BookingDetailRow(Icons.Rounded.CreditCard, "Payment Ref", booking.paymentId)
                    }
                    if (booking.notes.isNotBlank()) {
                        Text(
                            text = "Note: ${booking.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Area by State
            when {
                booking.bookingStatus == BookingLifecycleStatus.AWAITING_PAYMENT -> {
                    Text(
                        text = "Your booking will be confirmed only after completing the ₹${booking.advanceAmountInr} pre-booking payment in Chrome.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateMate3DGlowingButton(
                            text = "Pay ₹${booking.advanceAmountInr} in Chrome",
                            onClick = onPayInChrome,
                            style = GlowButtonStyle.ROSE_PINK,
                            leadingIcon = Icons.Rounded.CreditCard,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1.25f)
                        )
                        DateMate3DGlowingButton(
                            text = "Verify",
                            onClick = onVerifyPayment,
                            style = GlowButtonStyle.GLASS_SOFT,
                            leadingIcon = Icons.Rounded.Refresh,
                            enabled = !isBusy,
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }

                booking.isWaitingForRating -> {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${booking.profileName} marked your date Attended! Please rate your experience ⭐",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { selectedStars = star },
                                        modifier = Modifier.testTag("rating_star_${booking.id}_$star")
                                    ) {
                                        Icon(
                                            imageVector = if (star <= selectedStars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                            contentDescription = "$star stars",
                                            tint = if (star <= selectedStars) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = feedbackText,
                                onValueChange = { feedbackText = it.take(300) },
                                minLines = 2,
                                maxLines = 3,
                                label = { Text("Share Feedback (Optional)") },
                                placeholder = { Text("How was your date experience?") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            DateMate3DGlowingButton(
                                text = "Submit $selectedStars★ Rating & Complete",
                                onClick = { onSubmitRating(selectedStars, feedbackText) },
                                style = GlowButtonStyle.ROYAL_GOLD,
                                leadingIcon = Icons.Rounded.CheckCircle,
                                isLoading = isBusy,
                                enabled = !isBusy,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_rating_button_${booking.id}")
                            )
                        }
                    }
                }

                booking.isCompleted -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(booking.userRating.coerceIn(1, 5)) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Rated ${booking.userRating}/5 · Thank you!",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (booking.userFeedback.isNotBlank()) {
                        Text(
                            text = "\"${booking.userFeedback}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffVirtualMoneyCard(
    pendingVirtualInr: Int,
    paidVirtualInr: Int,
    completedCount: Int,
    activeCount: Int,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_virtual_wallet_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Staff Virtual Money Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Virtual earnings unlock after client rating. Main Admin approves & marks Paid ✓.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WalletMetricTile(
                    label = "Pending Virtual",
                    value = "₹$pendingVirtualInr",
                    accent = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                WalletMetricTile(
                    label = "Paid Out ✓",
                    value = "₹$paidVirtualInr",
                    accent = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Active Dates: $activeCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Completed Dates: $completedCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun WalletMetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StaffBookingCard(
    booking: DateMateBooking,
    isBusy: Boolean,
    onMarkAttended: () -> Unit,
) {
    val (payoutLabel, payoutColor) = when (booking.payoutStatus) {
        PayoutStatus.PAID -> "Paid ✓ by Main Admin" to Color(0xFF16A34A)
        PayoutStatus.REJECTED -> "Payout Not Approved" to Color(0xFFDC2626)
        else -> "Pending Main Admin Approval" to Color(0xFFD97706)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_booking_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = booking.clientName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${booking.planLabel} (${booking.planPrice}) · Advance ₹${booking.advanceAmountInr} Paid ✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "₹${booking.effectiveVirtualEarningInr}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Full Date Details for Staff
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BookingDetailRow(Icons.Rounded.Phone, "Client Phone", booking.clientPhone.ifBlank { "Shared via Chat" })
                    BookingDetailRow(Icons.Rounded.CalendarMonth, "Date", booking.dateLabel)
                    BookingDetailRow(Icons.Rounded.Schedule, "Time Slot", booking.timeSlot)
                    BookingDetailRow(Icons.Rounded.LocationOn, "Meeting Location", booking.meetingLocation)
                    if (booking.notes.isNotBlank()) {
                        Text(
                            text = "Client Note: ${booking.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                !booking.staffAttended -> {
                    DateMate3DGlowingButton(
                        text = "Mark Date Attended ✓",
                        onClick = onMarkAttended,
                        style = GlowButtonStyle.EMERALD_CHAT,
                        leadingIcon = Icons.Rounded.CheckCircle,
                        isLoading = isBusy,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("staff_mark_attended_btn_${booking.id}")
                    )
                }

                booking.bookingStatus == BookingLifecycleStatus.AWAITING_RATING -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Attended ✓ · Waiting for client to submit Rating & Feedback ⭐",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                booking.isCompleted -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = payoutColor.copy(alpha = 0.13f),
                        border = BorderStroke(1.dp, payoutColor.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Virtual Earning: ₹${booking.effectiveVirtualEarningInr}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = payoutLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = payoutColor
                                )
                            }
                            Text(
                                text = "Client Rating: ${booking.userRating}★ ${if (booking.userFeedback.isNotBlank()) "· \"${booking.userFeedback}\"" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (booking.payoutNote.isNotBlank()) {
                                Text(
                                    text = "Main Admin Note: ${booking.payoutNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = payoutColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
