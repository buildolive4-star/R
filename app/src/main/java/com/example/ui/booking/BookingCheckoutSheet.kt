package com.example.ui.booking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.BookingRatePlan
import com.example.data.model.GalleryImage
import com.example.data.model.UserAccount
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.PixoraNetworkImage
import com.example.util.LocationAddressFetcher
import kotlinx.coroutines.launch

private val QUICK_DATE_SLOTS = listOf("Today", "Tomorrow", "This Weekend", "Next Friday")
private val QUICK_TIME_SLOTS = listOf("1:00 PM", "5:00 PM", "7:30 PM", "9:30 PM", "Full Night")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingCheckoutSheet(
    profile: GalleryImage,
    defaultAdvanceInr: Int,
    userAccount: UserAccount?,
    defaultUserName: String,
    isSignedIn: Boolean,
    isBusy: Boolean,
    hasServerUrlConfigured: Boolean,
    onSignInFirst: () -> Unit,
    onProceedToChromePayment: (
        plan: BookingRatePlan,
        clientName: String,
        clientPhone: String,
        dateLabel: String,
        timeSlot: String,
        meetingLocation: String,
        notes: String,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val plans = remember(profile.id, profile.bookingPlans, profile.price) {
        profile.resolvedBookingPlans()
    }
    val advanceInr = remember(profile.id, profile.advanceBookingAmount, defaultAdvanceInr) {
        profile.resolvedAdvanceAmountInr(defaultAdvanceInr)
    }

    var selectedPlan by remember(profile.id, plans) { mutableStateOf(plans.first()) }
    var clientName by remember(userAccount?.name, defaultUserName) {
        mutableStateOf(userAccount?.name?.ifBlank { defaultUserName } ?: defaultUserName)
    }
    var clientPhone by remember(userAccount?.phone) {
        mutableStateOf(userAccount?.phone.orEmpty())
    }
    var dateLabel by remember { mutableStateOf(QUICK_DATE_SLOTS.first()) }
    var timeSlot by remember { mutableStateOf("7:30 PM") }
    var meetingLocation by remember(userAccount?.address, userAccount?.city) {
        mutableStateOf(
            userAccount?.address?.ifBlank { userAccount.city } ?: ""
        )
    }
    var notes by remember { mutableStateOf("") }
    var fetchingLocation by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun fetchCurrentAddress() {
        scope.launch {
            fetchingLocation = true
            val res = LocationAddressFetcher.fetchCurrentAddress(context)
            fetchingLocation = false
            res.onSuccess { fetched ->
                meetingLocation = fetched.fullAddress.ifBlank { fetched.city }
                validationError = null
            }.onFailure { err ->
                validationError = err.localizedMessage ?: "Unable to fetch GPS address"
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        if (granted) {
            fetchCurrentAddress()
        } else {
            validationError = "Location permission denied — enter meeting address manually."
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isBusy) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("booking_checkout_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PixoraNetworkImage(
                        imageUrl = profile.displayThumbnailUrl,
                        contentDescription = profile.title,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Book ${profile.title}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "${profile.role} · ${profile.city.ifBlank { profile.category }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isBusy) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Admin-Defined Rate Plans Picker (Dinner / Night / Coffee / etc.)
            Text(
                text = "Select Date Package & Price",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plans.forEach { plan ->
                    val isSelected = selectedPlan.label == plan.label && selectedPlan.priceText == plan.priceText
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.8.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedPlan = plan }
                            .testTag("booking_plan_${plan.label}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = plan.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isSelected) "Selected package ✓" else "Tap to choose",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = plan.priceText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pre-Booking Advance Highlight Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pre-Booking Advance Token",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Booking confirms only after one-time Razorpay advance payment in Chrome. Remaining balance (${selectedPlan.priceText}) settles at meeting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "₹$advanceInr",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Date Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Preferred Date", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_DATE_SLOTS.forEach { slot ->
                    FilterChip(
                        selected = dateLabel == slot,
                        onClick = { dateLabel = slot },
                        label = { Text(slot) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            OutlinedTextField(
                value = dateLabel,
                onValueChange = { dateLabel = it.take(40) },
                singleLine = true,
                label = { Text("Date / Day (or type custom date)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Time Slot Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Timing", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_TIME_SLOTS.forEach { slot ->
                    FilterChip(
                        selected = timeSlot == slot,
                        onClick = { timeSlot = slot },
                        label = { Text(slot) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Client Name & Phone
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it.take(60) },
                    singleLine = true,
                    label = { Text("Your Name *") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = clientPhone,
                    onValueChange = { clientPhone = it.take(18) },
                    singleLine = true,
                    label = { Text("Mobile No. *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Meeting Location with Auto-Fetch Address
            OutlinedTextField(
                value = meetingLocation,
                onValueChange = { meetingLocation = it.take(220) },
                minLines = 2,
                maxLines = 3,
                label = { Text("Exact Meeting Cafe / Venue / Location *") },
                placeholder = { Text("Hotel, Cafe, Mall or Area address") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_meeting_location_input")
            )
            OutlinedButton(
                onClick = {
                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (fine || coarse) {
                        fetchCurrentAddress()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                enabled = !fetchingLocation && !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (fetchingLocation) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Current Location…")
                } else {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Fetch Current Location")
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(300) },
                minLines = 2,
                maxLines = 3,
                label = { Text("Special Notes / Preferences (Optional)") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (!hasServerUrlConfigured) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Admin needs to add the Render Payment Server URL in Admin Studio → Bookings before online payment can open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!isSignedIn) {
                DateMate3DGlowingButton(
                    text = "Sign In with Google to Book",
                    onClick = onSignInFirst,
                    style = GlowButtonStyle.ROSE_PINK,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_signin_button")
                )
            } else {
                DateMate3DGlowingButton(
                    text = "Pay ₹$advanceInr Advance in Chrome & Book",
                    onClick = {
                        when {
                            clientName.trim().length < 2 -> validationError = "Please enter your name."
                            clientPhone.filter { it.isDigit() }.length < 8 -> validationError = "Please enter a valid mobile number."
                            meetingLocation.trim().length < 4 -> validationError = "Please enter meeting cafe/venue address."
                            else -> {
                                validationError = null
                                onProceedToChromePayment(
                                    selectedPlan,
                                    clientName.trim(),
                                    clientPhone.trim(),
                                    dateLabel.trim().ifBlank { "Today" },
                                    timeSlot.trim().ifBlank { "7:30 PM" },
                                    meetingLocation.trim(),
                                    notes.trim(),
                                )
                            }
                        }
                    },
                    isLoading = isBusy,
                    enabled = !isBusy,
                    style = GlowButtonStyle.ROSE_PINK,
                    leadingIcon = Icons.Rounded.CreditCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_pay_chrome_button")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
