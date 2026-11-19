package com.example.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserAccount
import com.example.data.model.UserComplaint
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.Animated3DPageBackdrop
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMate3DGlowingIconButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.GlowButtonStyle
import com.example.ui.theme.AppLookTheme
import com.google.firebase.auth.FirebaseUser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    firebaseUser: FirebaseUser?,
    userAccount: UserAccount?,
    isProfileBusy: Boolean,
    galleryCount: Int,
    savedCount: Int,
    hasNotificationPermission: Boolean,
    oneSignalPushConfigured: Boolean = false,
    backgroundSyncEnabled: Boolean,
    lastSyncCheckEpochMs: Long,
    currentLookTheme: AppLookTheme = AppLookTheme.DEFAULT,
    myComplaints: List<UserComplaint> = emptyList(),
    onSelectLookTheme: (AppLookTheme) -> Unit = {},
    onSubmitComplaint: (subject: String, message: String, onDone: () -> Unit) -> Unit = { _, _, _ -> },
    onSignInGoogle: () -> Unit,
    onSignOut: () -> Unit,
    onSaveProfile: (
        name: String,
        avatarUri: Uri?,
        galleryUris: List<Uri>,
        phone: String,
        ageText: String,
        address: String,
        city: String,
        bio: String,
        interests: String,
        genderPreference: String,
    ) -> Unit,
    onApplyForStaff: (staffRole: String, staffPrice: String) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onToggleBackgroundSync: (Boolean) -> Unit,
    onCheckSyncNow: () -> Unit,
    onClearSaved: () -> Unit,
    onSecretAdminTrigger: () -> Unit,
    onSendTestAlert: () -> Unit = {},
    authErrorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showComplaintDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val shareDateMateApp: () -> Unit = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DateMate — Verified Companions & Date Bookings")
            putExtra(
                Intent.EXTRA_TEXT,
                "Join me on DateMate! Discover verified companions, chat privately, and book unforgettable dates safely. ❤️"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share DateMate App"))
    }

    val themeDisplayName = when (currentLookTheme) {
        AppLookTheme.DEFAULT -> "Default Rose"
        AppLookTheme.GOLD -> "Royal Gold"
        AppLookTheme.SILVER -> "Platinum Silver"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                // Header Row with dedicated 3D Glowing Settings Button (opens Theme Switcher & Preferences)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.profile_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.profile_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DateMate3DGlowingIconButton(
                        icon = Icons.Rounded.Settings,
                        contentDescription = "App Settings & Themes",
                        onClick = { showSettingsSheet = true },
                        style = GlowButtonStyle.ROSE_PINK,
                        size = 46.dp,
                        iconSize = 22.dp,
                        modifier = Modifier.testTag("profile_settings_button")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProfileEditorSection(
                    firebaseUser = firebaseUser,
                    userAccount = userAccount,
                    isBusy = isProfileBusy,
                    onSignInGoogle = onSignInGoogle,
                    onSignOut = onSignOut,
                    onSaveProfile = onSaveProfile,
                    onApplyForStaff = onApplyForStaff,
                    authErrorMessage = authErrorMessage,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = galleryCount.toString(),
                        label = stringResource(R.string.gallery_count_label),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = savedCount.toString(),
                        label = stringResource(R.string.saved_count_label),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Profile Settings, Themes, Complaint Box, Legal & Share Menu Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(
                        1.2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Settings, Support & Legal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        ProfileActionRow(
                            icon = Icons.Rounded.Palette,
                            title = "App Settings & Themes",
                            subtitle = "Active Look: $themeDisplayName (Default · Gold · Silver)",
                            onClick = { showSettingsSheet = true },
                            testTag = "profile_theme_settings_row"
                        )

                        ProfileActionRow(
                            icon = Icons.Rounded.Feedback,
                            title = "Complaint Box & Support",
                            subtitle = if (myComplaints.isEmpty()) {
                                "Report any booking, payment, or companion issue"
                            } else {
                                "${myComplaints.size} ticket(s) submitted · Tap to view or report"
                            },
                            onClick = { showComplaintDialog = true },
                            testTag = "profile_complaint_box_row"
                        )

                        ProfileActionRow(
                            icon = Icons.Rounded.Share,
                            title = "Share DateMate App",
                            subtitle = "Invite friends via WhatsApp, Telegram or SMS",
                            onClick = shareDateMateApp,
                            testTag = "profile_share_app_row"
                        )

                        ProfileActionRow(
                            icon = Icons.Rounded.Description,
                            title = "Terms & Conditions",
                            subtitle = "Booking rules, pre-booking advance & safety guidelines",
                            onClick = { showTermsDialog = true },
                            testTag = "profile_terms_row"
                        )

                        ProfileActionRow(
                            icon = Icons.Rounded.PrivacyTip,
                            title = "Privacy Policy",
                            subtitle = "Screenshot protection, private contacts & data safety",
                            onClick = { showPrivacyDialog = true },
                            testTag = "profile_privacy_row"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Instant Heads-Up Push Alerts Card (Sound + Vibration)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(
                        1.3.dp,
                        Brush.linearGradient(
                            listOf(
                                if (hasNotificationPermission) Color(0xFF00BFA5) else MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Animated3DMascotOrb(
                                mascot = DateMateMascot.SAFETY_MATE,
                                size = 48.dp,
                                showOrbitSparkles = true,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (hasNotificationPermission) {
                                        "SafetyMate & Push Alerts Active"
                                    } else {
                                        stringResource(R.string.enable_alerts_title)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Instant system push alerts with notification chime & vibration even outside the app.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (oneSignalPushConfigured) {
                                        stringResource(R.string.onesignal_push_active)
                                    } else {
                                        "Live Background Push Engine Active ✓"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (oneSignalPushConfigured) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (!hasNotificationPermission) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DateMate3DGlowingButton(
                                text = stringResource(R.string.enable_alerts_button),
                                onClick = onRequestNotificationPermission,
                                style = GlowButtonStyle.ROSE_PINK,
                                leadingIcon = Icons.Rounded.NotificationsActive,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_enable_alerts_button")
                            )
                        }
                    }
                }

                if (savedCount > 0) {
                    Spacer(modifier = Modifier.height(18.dp))

                    DateMate3DGlowingButton(
                        text = stringResource(R.string.clear_saved_button),
                        onClick = { showClearDialog = true },
                        style = GlowButtonStyle.DANGER_RED,
                        leadingIcon = Icons.Rounded.DeleteSweep,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_saved_button")
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Discreet footer: long-press opens hidden Admin Studio passcode gate
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = onSecretAdminTrigger
                        )
                        .testTag("studio_version_footer"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.studio_version_footer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }

                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    if (showSettingsSheet) {
        AppSettingsAndThemeSheet(
            currentLookTheme = currentLookTheme,
            onSelectLookTheme = onSelectLookTheme,
            onShareApp = shareDateMateApp,
            onOpenComplaint = {
                showSettingsSheet = false
                showComplaintDialog = true
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showComplaintDialog) {
        ComplaintBoxDialog(
            isSignedIn = firebaseUser != null,
            isBusy = isProfileBusy,
            myComplaints = myComplaints,
            onSubmit = { subject, msg ->
                onSubmitComplaint(subject, msg) {
                    showComplaintDialog = false
                }
            },
            onDismiss = { showComplaintDialog = false }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Adults Only (18+): DateMate is strictly for consenting adults aged 18 and above.\n\n" +
                            "2. Transparent Pre-Booking Advance: All bookings require a one-time Razorpay pre-booking advance payment displayed clearly before checkout. No hidden recurring charges are applied.\n\n" +
                            "3. Respect & Safety: Abusive behavior, harassment, or sharing private contact numbers inside monitored staff chat is prohibited and may lead to suspension.\n\n" +
                            "4. Attendance & Ratings: After a companion marks a date attended, clients submit an honest star rating & feedback to finalize the booking.\n\n" +
                            "5. Support: Use the Complaint Box anytime for fast assistance from the DateMate Admin team.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) { Text("I Understand") }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Screen Privacy: DateMate enables Android FLAG_SECURE across screens to prevent unauthorized screenshots and screen recordings.\n\n" +
                            "2. Private Contacts Protected: Companion phone numbers and exact home addresses are stored in restricted admin-only records and never exposed publicly.\n\n" +
                            "3. Location Privacy: GPS auto-fetch is used only when you request it to fill your city or meeting venue.\n\n" +
                            "4. Secure Authentication & Payments: Accounts are protected with Google Sign-In, and pre-booking advance payments are processed securely via Razorpay in Chrome.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(R.string.clear_saved_confirm_title)) },
            text = { Text(text = stringResource(R.string.clear_saved_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearSaved()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_clear_saved_button")
                ) {
                    Text(text = stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsAndThemeSheet(
    currentLookTheme: AppLookTheme,
    onSelectLookTheme: (AppLookTheme) -> Unit,
    onShareApp: () -> Unit,
    onOpenComplaint: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("app_settings_theme_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Settings & Look",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Change DateMate's luxury appearance instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Choose App Theme (3 Luxury Looks)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            ThemeOptionCard(
                title = "Default (Rose Pink)",
                subtitle = "Signature romantic rose-pink glow & soft blush cards",
                swatchColors = listOf(Color(0xFFE91E63), Color(0xFFFF6F9C), Color(0xFFFFF4F7)),
                selected = currentLookTheme == AppLookTheme.DEFAULT,
                onClick = { onSelectLookTheme(AppLookTheme.DEFAULT) },
                testTag = "theme_option_default"
            )

            ThemeOptionCard(
                title = "Gold (Royal Champagne)",
                subtitle = "Luxury 24K champagne gold accents & warm ivory aura",
                swatchColors = listOf(Color(0xFFC88B14), Color(0xFFE8B43C), Color(0xFFFFFBF2)),
                selected = currentLookTheme == AppLookTheme.GOLD,
                onClick = { onSelectLookTheme(AppLookTheme.GOLD) },
                testTag = "theme_option_gold"
            )

            ThemeOptionCard(
                title = "Silver (Platinum Mist)",
                subtitle = "Sleek metallic platinum slate & luminous silver glass",
                swatchColors = listOf(Color(0xFF475569), Color(0xFF7B8FA6), Color(0xFFF6F8FB)),
                selected = currentLookTheme == AppLookTheme.SILVER,
                onClick = { onSelectLookTheme(AppLookTheme.SILVER) },
                testTag = "theme_option_silver"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DateMate3DGlowingButton(
                    text = "Complaint Box",
                    onClick = onOpenComplaint,
                    style = GlowButtonStyle.GLASS_SOFT,
                    leadingIcon = Icons.Rounded.Feedback,
                    modifier = Modifier.weight(1f)
                )
                DateMate3DGlowingButton(
                    text = "Share App",
                    onClick = onShareApp,
                    style = GlowButtonStyle.ROSE_PINK,
                    leadingIcon = Icons.Rounded.Share,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    swatchColors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(swatchColors))
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ComplaintBoxDialog(
    isSignedIn: Boolean,
    isBusy: Boolean,
    myComplaints: List<UserComplaint>,
    onSubmit: (subject: String, message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complaint Box & Support", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isSignedIn) {
                    Text(
                        text = "Please sign in with Google from your Profile first to submit a complaint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it.take(100) },
                        singleLine = true,
                        label = { Text("Subject *") },
                        placeholder = { Text("e.g. Booking or Payment issue") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complaint_subject_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(800) },
                        minLines = 3,
                        maxLines = 5,
                        label = { Text("Describe Your Issue *") },
                        placeholder = { Text("Write booking ID or full details so Admin can resolve quickly…") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complaint_message_input")
                    )
                }

                if (myComplaints.isNotEmpty()) {
                    Text(
                        text = "Your Recent Complaints (${myComplaints.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    myComplaints.take(4).forEach { cmp ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cmp.subject,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (cmp.status == "resolved") "Resolved ✓" else "Open",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cmp.status == "resolved") Color(0xFF16A34A) else Color(0xFFD97706)
                                    )
                                }
                                Text(
                                    text = cmp.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (cmp.adminReply.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Admin Reply: ${cmp.adminReply}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSignedIn) {
                Button(
                    onClick = { onSubmit(subject, message) },
                    enabled = !isBusy && subject.trim().length >= 2 && message.trim().length >= 4,
                    modifier = Modifier.testTag("complaint_submit_button")
                ) {
                    Text("Submit Complaint")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(22.dp),
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
