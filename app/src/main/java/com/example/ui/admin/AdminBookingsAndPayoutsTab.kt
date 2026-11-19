package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BookingLifecycleStatus
import com.example.data.model.DateMateBooking
import com.example.data.model.PaymentServerConfig
import com.example.data.model.PayoutStatus
import com.example.data.model.UserComplaint

@Composable
fun AdminBookingsAndPayoutsTab(
    isSuperAdmin: Boolean,
    isBusy: Boolean,
    paymentServerConfig: PaymentServerConfig,
    allBookings: List<DateMateBooking>,
    allComplaints: List<UserComplaint>,
    onSavePaymentServerConfig: (serverBaseUrl: String, defaultAdvanceInr: Int) -> Unit,
    onTestPaymentServer: (serverBaseUrl: String) -> Unit,
    onApproveStaffPayout: (bookingId: String, note: String) -> Unit,
    onRejectStaffPayout: (bookingId: String, note: String) -> Unit,
    onResolveComplaint: (complaintId: String, reply: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var serverUrl by remember(paymentServerConfig.serverBaseUrl) {
        mutableStateOf(paymentServerConfig.cleanBaseUrl)
    }
    var defaultAdvanceText by remember(paymentServerConfig.defaultAdvanceInr) {
        mutableStateOf(paymentServerConfig.defaultAdvanceInr.toString())
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_bookings_payouts_tab")
    ) {
        // 1. Render Razorpay Payment Server Configuration Card
        item(key = "payment_server_config_card") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Chrome Razorpay Payment Server",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connected to https://jaiambey.shop by default. Users pay dynamic pre-booking advance in Chrome.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it.take(300) },
                        singleLine = true,
                        label = { Text("Render Server Base URL") },
                        placeholder = { Text("https://jaiambey.shop") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_payment_server_url_input")
                    )

                    OutlinedTextField(
                        value = defaultAdvanceText,
                        onValueChange = { defaultAdvanceText = it.filter { ch -> ch.isDigit() }.take(6) },
                        singleLine = true,
                        label = { Text("Fallback Pre-Booking Advance (₹)") },
                        placeholder = { Text("299 (Any custom amount)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_default_advance_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = (defaultAdvanceText.toIntOrNull() ?: 299).coerceIn(1, 500_000)
                                onSavePaymentServerConfig(serverUrl.trim(), amt)
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("admin_save_payment_server_button")
                        ) {
                            Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Config", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { onTestPaymentServer(serverUrl.trim()) },
                            enabled = !isBusy && serverUrl.trim().isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("admin_test_payment_server_button")
                        ) {
                            Text("Test Server", maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. Bookings & Staff Virtual Money Ledger
        item(key = "bookings_ledger_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bookings & Staff Payouts (${allBookings.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSuperAdmin) {
                            "Main Admin Authority Active ✓ — Approve/Mark Paid or Reject Staff Virtual Money below."
                        } else {
                            "View-only payout ledger. Only Main Admin can approve Staff Virtual Money payouts."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuperAdmin) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (allBookings.isEmpty()) {
            item(key = "bookings_empty_card") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No client bookings yet. Once users pre-book profiles, full payment, attendance, ratings & Staff Virtual Money appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(allBookings, key = { it.id }) { booking ->
                AdminBookingLedgerCard(
                    booking = booking,
                    isSuperAdmin = isSuperAdmin,
                    isBusy = isBusy,
                    onApprovePaid = { note -> onApproveStaffPayout(booking.id, note) },
                    onRejectPayout = { note -> onRejectStaffPayout(booking.id, note) },
                )
            }
        }

        // 3. User Complaints Box Inbox
        item(key = "complaints_header") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Feedback,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "User Complaints Inbox (${allComplaints.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (allComplaints.isEmpty()) {
            item(key = "complaints_empty_card") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No user complaints submitted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(allComplaints, key = { it.id }) { complaint ->
                AdminComplaintCard(
                    complaint = complaint,
                    isBusy = isBusy,
                    onResolve = { reply -> onResolveComplaint(complaint.id, reply) }
                )
            }
        }
    }
}

@Composable
private fun AdminBookingLedgerCard(
    booking: DateMateBooking,
    isSuperAdmin: Boolean,
    isBusy: Boolean,
    onApprovePaid: (String) -> Unit,
    onRejectPayout: (String) -> Unit,
) {
    var payoutNote by remember(booking.id, booking.payoutNote) { mutableStateOf(booking.payoutNote) }

    val statusColor = when (booking.bookingStatus) {
        BookingLifecycleStatus.COMPLETED -> Color(0xFF0F766E)
        BookingLifecycleStatus.CONFIRMED -> Color(0xFF16A34A)
        BookingLifecycleStatus.AWAITING_RATING -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFD97706)
    }
    val payoutColor = when (booking.payoutStatus) {
        PayoutStatus.PAID -> Color(0xFF16A34A)
        PayoutStatus.REJECTED -> Color(0xFFDC2626)
        else -> Color(0xFFD97706)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_booking_card_${booking.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${booking.clientName} → ${booking.profileName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${booking.planLabel} (${booking.planPrice}) · Advance ₹${booking.advanceAmountInr} ${if (booking.advancePaid) "PAID ✓" else "PENDING"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = booking.bookingStatus.replace("_", " ").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "Date: ${booking.dateLabel} · ${booking.timeSlot} | Phone: ${booking.clientPhone.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Venue: ${booking.meetingLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (booking.paymentId.isNotBlank()) {
                Text(
                    text = "Razorpay Ref: ${booking.paymentId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Staff Attended: ${if (booking.staffAttended) "YES ✓" else "Not Yet"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (booking.userRating > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${booking.userRating}/5",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (booking.userFeedback.isNotBlank()) {
                Text(
                    text = "Client Feedback: \"${booking.userFeedback}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Staff Virtual Money Payout Controls
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = payoutColor.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, payoutColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = payoutColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Staff Virtual Money: ₹${booking.effectiveVirtualEarningInr}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = when (booking.payoutStatus) {
                                PayoutStatus.PAID -> "PAID ✓"
                                PayoutStatus.REJECTED -> "REJECTED"
                                else -> "PENDING"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = payoutColor
                        )
                    }

                    if (isSuperAdmin) {
                        OutlinedTextField(
                            value = payoutNote,
                            onValueChange = { payoutNote = it.take(180) },
                            singleLine = true,
                            label = { Text("Main Admin Payout Note / UTR (Optional)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApprovePaid(payoutNote) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.25f)
                                    .testTag("superadmin_approve_payout_${booking.id}")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Approve & Paid ✓", maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { onRejectPayout(payoutNote) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .testTag("superadmin_reject_payout_${booking.id}")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Not Approved", maxLines = 1)
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Main Admin Approval Only 🔒",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun AdminComplaintCard(
    complaint: UserComplaint,
    isBusy: Boolean,
    onResolve: (String) -> Unit,
) {
    var replyText by remember(complaint.id, complaint.adminReply) { mutableStateOf(complaint.adminReply) }
    val isResolved = complaint.status.equals("resolved", ignoreCase = true)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.dp,
            if (isResolved) Color(0xFF16A34A).copy(alpha = 0.45f) else MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_complaint_card_${complaint.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = complaint.subject,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${complaint.userName} · ${complaint.userPhone.ifBlank { complaint.userEmail }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isResolved) Color(0xFF16A34A).copy(alpha = 0.14f) else MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isResolved) "RESOLVED ✓" else "OPEN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isResolved) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = complaint.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it.take(400) },
                minLines = 2,
                maxLines = 3,
                label = { Text("Admin Reply") },
                placeholder = { Text("Write resolution note for the user...") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onResolve(replyText) },
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_resolve_complaint_${complaint.id}")
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isResolved) "Update Reply" else "Reply & Resolve ✓")
            }
        }
    }
}
