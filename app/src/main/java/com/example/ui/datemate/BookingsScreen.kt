package com.example.ui.datemate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookingDraft
import com.example.data.model.BookingStatus
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.DateBooking
import com.example.data.model.DateMateCatalog
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateSuccessGreen
import com.example.ui.theme.DateMateWhite

// ============================================================================
// 9. BOOKING SYSTEM & 6-STEP BOOKING WIZARD
// Sections: Upcoming | Pending | Confirmed | Completed | Cancelled | History
// Steps:
// Step 1: Select Profile
// Step 2: Select Date
// Step 3: Select Available Time
// Step 4: Select Duration
// Step 5: Review Booking Details
// Step 6: Confirm Booking
// ============================================================================
private enum class BookingFilterTab(val label: String) {
    UPCOMING("Upcoming"),
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    HISTORY("Booking History"),
}

@Composable
fun DateMateBookingsScreen(
    bookings: List<DateBooking>,
    companions: List<DateMateCompanionProfile>,
    onOpenNewBookingWizard: () -> Unit,
    onCancelBooking: (String) -> Unit,
    onOpenChatForCompanion: (String) -> Unit,
    onOpenCompanionProfile: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BookingFilterTab.UPCOMING) }

    val filteredBookings = remember(bookings, selectedTab) {
        when (selectedTab) {
            BookingFilterTab.UPCOMING -> bookings.filter {
                it.status == BookingStatus.UPCOMING ||
                    it.status == BookingStatus.CONFIRMED ||
                    it.status == BookingStatus.PENDING
            }
            BookingFilterTab.PENDING -> bookings.filter { it.status == BookingStatus.PENDING }
            BookingFilterTab.CONFIRMED -> bookings.filter { it.status == BookingStatus.CONFIRMED }
            BookingFilterTab.COMPLETED -> bookings.filter { it.status == BookingStatus.COMPLETED }
            BookingFilterTab.CANCELLED -> bookings.filter { it.status == BookingStatus.CANCELLED }
            BookingFilterTab.HISTORY -> bookings
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("bookings_screen_list")
    ) {
        // Header + New Booking CTA
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
            ) {
                Column {
                    Text(
                        text = "Date Bookings",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "Plan & manage your verified companion experiences",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMateSecondaryText
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMatePrimaryPink,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .bouncy3DPress(onClick = onOpenNewBookingWizard)
                        .testTag("new_booking_wizard_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Book a Date",
                            tint = DateMateWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Book a Date",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMateWhite,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Status Filter Tabs
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                BookingFilterTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    FilterChip(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DateMatePrimaryPink,
                            selectedLabelColor = DateMateWhite,
                            containerColor = DateMateWhite,
                            labelColor = DateMateDarkText
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DateMateSoftBorder,
                            selectedBorderColor = DateMatePrimaryPink
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
        }

        if (filteredBookings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = DateMateWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .border(1.dp, DateMateSoftBorder, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Animated3DHeartLogo(sizeDp = 76.dp, showDmMonogram = false)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No ${selectedTab.label} Yet",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Launch our 6-Step Date Booking planner to schedule an unforgettable evening with a verified companion.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DateMateSecondaryText
                            ),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        DateMateGradientButton(
                            text = "Start 6-Step Date Booking",
                            onClick = onOpenNewBookingWizard
                        )
                    }
                }
            }
        } else {
            items(filteredBookings, key = { it.id }) { booking ->
                DateBookingItemCard(
                    booking = booking,
                    onCancel = { onCancelBooking(booking.id) },
                    onChat = { onOpenChatForCompanion(booking.companionId) },
                    onViewProfile = { onOpenCompanionProfile(booking.companionId) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun DateBookingItemCard(
    booking: DateBooking,
    onCancel: () -> Unit,
    onChat: () -> Unit,
    onViewProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED, BookingStatus.UPCOMING -> DateMateSuccessGreen
        BookingStatus.PENDING -> Color(0xFFD97706)
        BookingStatus.COMPLETED -> DateMatePrimaryPink
        BookingStatus.CANCELLED -> Color(0xFFE11D48)
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(26.dp))
            .testTag("booking_card_${booking.id}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onViewProfile() }
                ) {
                    AsyncImage(
                        model = booking.companionPhotoUrl,
                        contentDescription = booking.companionName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(2.dp, DateMatePrimaryPink, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = booking.companionName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "${booking.companionCity} • Code: ${booking.confirmationCode}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = booking.status.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = DateMateSoftBorder)

            // Date, Time, Duration, Experience Details
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DateMateSoftBlush.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = booking.dateLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${booking.timeSlot} (${booking.durationHours} hrs)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = "Experience: ${booking.venueExperience}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (booking.specialNote.isNotBlank()) {
                        Text(
                            text = "\"${booking.specialNote}\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText
                            )
                        )
                    }
                }
            }

            // Cancellation policy
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = DateMateSecondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = booking.cancellationPolicy,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText,
                        fontSize = 11.sp
                    )
                )
            }

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onChat() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Message Companion",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (booking.status != BookingStatus.CANCELLED && booking.status != BookingStatus.COMPLETED) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateBackground,
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onCancel() }
                            .testTag("cancel_booking_${booking.id}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMateSecondaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6-STEP BOOKING WIZARD MODAL SHEET
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SixStepBookingWizardSheet(
    draft: BookingDraft,
    companions: List<DateMateCompanionProfile>,
    onUpdateDraft: ((BookingDraft) -> BookingDraft) -> Unit,
    onConfirmBooking: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedCompanion = remember(draft.companionId, companions) {
        companions.firstOrNull { it.id == draft.companionId } ?: companions.firstOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DateMateWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
                .testTag("booking_wizard_sheet")
        ) {
            // Top Header + Step Counter (Step 1..6)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Step ${draft.step} of 6",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = when (draft.step) {
                            1 -> "Select Companion Profile"
                            2 -> "Select Date"
                            3 -> "Select Available Time"
                            4 -> "Select Duration & Venue"
                            5 -> "Review Booking Details"
                            else -> "Confirm Your Date Booking"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = DateMateSecondaryText
                    )
                }
            }

            LinearProgressIndicator(
                progress = { draft.step / 6f },
                color = DateMatePrimaryPink,
                trackColor = DateMateSoftBlush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp))
            )

            // Step Content
            when (draft.step) {
                // STEP 1: Select Profile
                1 -> {
                    Text(
                        text = "Choose a verified companion for your upcoming date:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        companions.forEach { companion ->
                            val isSelected = companion.id == draft.companionId
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) DateMateSoftBlush else DateMateBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) DateMatePrimaryPink else DateMateSoftBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        onUpdateDraft { it.copy(companionId = companion.id) }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    AsyncImage(
                                        model = companion.primaryPhotoUrl,
                                        contentDescription = companion.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${companion.name}, ${companion.age}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = DateMateDarkText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            text = "${companion.city} • ★ ${companion.rating}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = DateMateSecondaryText
                                            )
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = DateMatePrimaryPink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 2: Select Date
                2 -> {
                    Text(
                        text = "Select a preferred calendar date with ${selectedCompanion?.name ?: "your companion"}:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateMateCatalog.dateOptions.forEach { dateOption ->
                            val selected = draft.selectedDateLabel == dateOption
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) DateMatePrimaryPink else DateMateSoftBlush,
                                modifier = Modifier
                                    .clickable {
                                        onUpdateDraft { it.copy(selectedDateLabel = dateOption) }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (selected) DateMateWhite else DateMatePrimaryPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateOption,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (selected) DateMateWhite else DateMateDarkText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 3: Select Available Time
                3 -> {
                    Text(
                        text = "Choose an available time slot on ${draft.selectedDateLabel}:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DateMateCatalog.timeSlotOptions.forEach { slot ->
                            val selected = draft.selectedTimeSlot == slot
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) DateMatePrimaryPink else DateMateSoftBlush,
                                modifier = Modifier.clickable {
                                    onUpdateDraft { it.copy(selectedTimeSlot = slot) }
                                }
                            ) {
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (selected) DateMateWhite else DateMateDarkText,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }

                // STEP 4: Select Duration & Experience
                4 -> {
                    Text(
                        text = "Select duration (in hours):",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DateMateCatalog.durationOptions.forEach { hrs ->
                            val selected = draft.durationHours == hrs
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) DateMatePrimaryPink else DateMateSoftBlush,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUpdateDraft { it.copy(durationHours = hrs) }
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "${hrs}h",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            color = if (selected) DateMateWhite else DateMateDarkText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Choose Date Experience Style:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateMateCatalog.venueExperiences.forEach { venue ->
                            val selected = draft.venueExperience == venue
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) DateMateSoftBlush else DateMateBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (selected) DateMatePrimaryPink else DateMateSoftBorder,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        onUpdateDraft { it.copy(venueExperience = venue) }
                                    }
                            ) {
                                Text(
                                    text = venue,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (selected) DateMatePrimaryPink else DateMateDarkText,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // STEP 5: Review Booking Details
                5 -> {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(22.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Text(
                                text = "Booking Summary",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Companion: ${selectedCompanion?.name ?: ""} (${selectedCompanion?.city ?: ""})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Date & Time: ${draft.selectedDateLabel} at ${draft.selectedTimeSlot}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = DateMateDarkText)
                            )
                            Text(
                                text = "Duration: ${draft.durationHours} Hours • ${draft.venueExperience}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = DateMateDarkText)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = draft.specialNote,
                        onValueChange = { note -> onUpdateDraft { it.copy(specialNote = note) } },
                        label = { Text("Special Note or Venue Preference") },
                        shape = RoundedCornerShape(16.dp),
                        colors = dateMateTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateDraft { it.copy(agreedToPolicy = !it.agreedToPolicy) }
                            }
                    ) {
                        Checkbox(
                            checked = draft.agreedToPolicy,
                            onCheckedChange = { checked ->
                                onUpdateDraft { it.copy(agreedToPolicy = checked) }
                            },
                            colors = CheckboxDefaults.colors(checkedColor = DateMatePrimaryPink)
                        )
                        Text(
                            text = "Free cancellation up to 6 hours before your scheduled meet.",
                            style = MaterialTheme.typography.bodySmall.copy(color = DateMateSecondaryText)
                        )
                    }
                }

                // STEP 6: Final Confirmation
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Animated3DHeartLogo(sizeDp = 80.dp, showDmMonogram = false)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ready to Confirm Your Date!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${selectedCompanion?.name} on ${draft.selectedDateLabel} at ${draft.selectedTimeSlot} (${draft.durationHours}h) for ${draft.venueExperience}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DateMateSecondaryText
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Navigation Footer Buttons (Back / Next or Confirm Booking)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (draft.step > 1) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = 52.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                onUpdateDraft { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Previous",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                DateMateGradientButton(
                    text = if (draft.step < 6) "Continue to Step ${draft.step + 1}" else "Confirm Booking ✨",
                    onClick = {
                        if (draft.step < 6) {
                            onUpdateDraft { it.copy(step = it.step + 1) }
                        } else {
                            onConfirmBooking()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    testTag = "booking_wizard_next_btn"
                )
            }
        }
    }
}
