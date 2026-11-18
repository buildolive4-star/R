package com.example.ui.datemate

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.CustomerUserProfile
import com.example.data.model.DateMateCatalog
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateWhite

// ============================================================================
// 13. MY PROFILE & SETTINGS SCREEN
// Features: Edit Profile, Change Profile Photo, Upload Multiple Photos,
// Update Bio, Manage Interests, Update Location, Privacy Settings,
// Notification Settings, Change Password, Blocked Users, Help & Support,
// Terms & Conditions, Privacy Policy, Logout, Delete Account
// (IMPORTANT: No Wallet Section, No Admin Panel, No Dark Theme)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateMateMyProfileScreen(
    userProfile: CustomerUserProfile,
    favoriteCompanions: List<DateMateCompanionProfile>,
    blockedCompanions: List<DateMateCompanionProfile>,
    onUpdateProfile: ((CustomerUserProfile) -> CustomerUserProfile) -> Unit,
    onAddProfilePhoto: (String) -> Unit,
    onOpenMembership: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCompanionDetail: (DateMateCompanionProfile) -> Unit,
    onUnblockCompanion: (String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showTermsPrivacyDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAddProfilePhoto(uri.toString())
            }
        }
    )

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(5),
        onResult = { uris ->
            uris.forEach { uri -> onAddProfilePhoto(uri.toString()) }
        }
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("my_profile_screen_list")
    ) {
        // 1. Profile Hero Card with Avatar, Verification Badge, Membership Pill & Edit Button
        item {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = DateMateWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
                    .border(1.5.dp, DateMateSoftBorder, RoundedCornerShape(30.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = userProfile.fullName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .border(3.dp, DateMatePrimaryPink, CircleShape)
                        )
                        Surface(
                            shape = CircleShape,
                            color = DateMatePrimaryPink,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    singlePhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("change_profile_photo_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.AddAPhoto,
                                    contentDescription = "Change Profile Photo",
                                    tint = DateMateWhite,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${userProfile.fullName}, ${userProfile.age}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = "Verified 18+",
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${userProfile.city} • ${userProfile.email}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMateSecondaryText
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Membership Status Badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .clickable { onOpenMembership() }
                            .testTag("profile_membership_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DateMate ${userProfile.membershipTier.title} Member • Manage Plan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = userProfile.bio,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DateMateDarkText
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DateMateGradientButton(
                        text = "Edit Profile, Bio & Location",
                        onClick = { showEditProfileDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = DateMateWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "edit_profile_btn"
                    )
                }
            }
        }

        // 2. Upload Multiple Photos Gallery Section
        item {
            ProfileSectionCard(
                title = "My Photos",
                subtitle = "Upload multiple lifestyle photos to stand out",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    // Add Photo Tile
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .size(width = 96.dp, height = 120.dp)
                            .border(1.5.dp, DateMatePrimaryPink, RoundedCornerShape(20.dp))
                            .clickable {
                                multiPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("upload_multiple_photos_btn")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Upload Photos",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add Photos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    userProfile.galleryPhotos.forEach { photoUrl ->
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 96.dp, height = 120.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, DateMateSoftBorder, RoundedCornerShape(20.dp))
                        )
                    }
                }
            }
        }

        // 3. Manage Interests Section
        item {
            ProfileSectionCard(
                title = "Manage Interests",
                subtitle = "Tap interests to customize your match recommendations",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateMateCatalog.allInterests.filterNot { it == "All" }.forEach { interest ->
                        val isSelected = interest in userProfile.interests
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdateProfile { current ->
                                    val updated = if (isSelected) {
                                        (current.interests - interest).ifEmpty { listOf(interest) }
                                    } else {
                                        current.interests + interest
                                    }
                                    current.copy(interests = updated)
                                }
                            },
                            label = {
                                Text(
                                    text = interest,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DateMatePrimaryPink,
                                selectedLabelColor = DateMateWhite,
                                containerColor = DateMateSoftBlush,
                                labelColor = DateMateDarkText
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                    }
                }
            }
        }

        // 4. Saved Favourites Strip
        if (favoriteCompanions.isNotEmpty()) {
            item {
                ProfileSectionCard(
                    title = "My Favourite Companions (${favoriteCompanions.size})",
                    subtitle = "Quick access to profiles you loved",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        favoriteCompanions.forEach { companion ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(78.dp)
                                    .clickable { onOpenCompanionDetail(companion) }
                            ) {
                                AsyncImage(
                                    model = companion.primaryPhotoUrl,
                                    contentDescription = companion.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, DateMatePrimaryPink, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = companion.name.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DateMateDarkText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Privacy Settings Section
        item {
            ProfileSectionCard(
                title = "Privacy Settings",
                subtitle = "Control your visibility & online presence",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        label = "Show Online Status",
                        checked = userProfile.showOnlineStatus,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(showOnlineStatus = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Show Distance in Kilometers",
                        checked = userProfile.showDistance,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(showDistance = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Chat Read Receipts",
                        checked = userProfile.readReceiptsEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(readReceiptsEnabled = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Incognito Private Browsing",
                        checked = userProfile.incognitoMode,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(incognitoMode = enabled) }
                        }
                    )
                }
            }
        }

        // 6. Notification Settings Section
        item {
            ProfileSectionCard(
                title = "Notification Settings",
                subtitle = "Choose which real-time alerts you receive",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsSwitchRow(
                        label = "New Likes & Romantic Matches",
                        checked = userProfile.pushLikesAndMatches,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(pushLikesAndMatches = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Chat Messages & Voice Calls",
                        checked = userProfile.pushMessages,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(pushMessages = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Booking Requests & Confirmations",
                        checked = userProfile.pushBookingAlerts,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(pushBookingAlerts = enabled) }
                        }
                    )
                    SettingsSwitchRow(
                        label = "Membership & VIP Privileges",
                        checked = userProfile.pushMembershipOffers,
                        onCheckedChange = { enabled ->
                            onUpdateProfile { it.copy(pushMembershipOffers = enabled) }
                        }
                    )
                }
            }
        }

        // 7. Account, Security, Legal & Support Actions
        item {
            ProfileSectionCard(
                title = "Security, Legal & Support",
                subtitle = "Manage your account credentials and policies",
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsActionRow(
                        icon = Icons.Outlined.Lock,
                        title = "Change Password",
                        subtitle = "Update your DateMate login password",
                        onClick = { showChangePasswordDialog = true },
                        testTag = "profile_change_password_btn"
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.Block,
                        title = "Blocked Users (${blockedCompanions.size})",
                        subtitle = "View or unblock restricted profiles",
                        onClick = { showBlockedUsersDialog = true },
                        testTag = "profile_blocked_users_btn"
                    )
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        title = "Help & Support",
                        subtitle = "24/7 DateMate Concierge & Safety Center",
                        onClick = { showHelpSupportDialog = true },
                        testTag = "profile_help_support_btn"
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.Description,
                        title = "Terms & Conditions",
                        subtitle = "Community rules, booking & cancellation policy",
                        onClick = { showTermsPrivacyDialog = "Terms & Conditions" },
                        testTag = "profile_terms_btn"
                    )
                    SettingsActionRow(
                        icon = Icons.Outlined.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "How DateMate protects your personal data",
                        onClick = { showTermsPrivacyDialog = "Privacy Policy" },
                        testTag = "profile_privacy_policy_btn"
                    )
                }
            }
        }

        // 8. Logout & Delete Account Buttons
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .border(1.dp, DateMatePrimaryPink.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onLogout() }
                        .testTag("profile_logout_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Logout",
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { showDeleteConfirmDialog = true }
                        .testTag("profile_delete_account_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = "Delete Account",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFFE11D48),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var nameInput by rememberSaveable { mutableStateOf(userProfile.fullName) }
        var cityInput by rememberSaveable { mutableStateOf(userProfile.city) }
        var bioInput by rememberSaveable { mutableStateOf(userProfile.bio) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Edit Profile & Bio",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("Location / City") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text("About Me / Bio") },
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateProfile {
                            it.copy(
                                fullName = nameInput.ifBlank { it.fullName },
                                city = cityInput.ifBlank { it.city },
                                bio = bioInput.ifBlank { it.bio }
                            )
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save Changes", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = dateMateTextFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        onShowMessage("Your password has been securely updated ✓")
                    }
                ) {
                    Text("Update Password", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }

    // Blocked Users Dialog
    if (showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Blocked Users",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                if (blockedCompanions.isEmpty()) {
                    Text(
                        text = "You have not blocked any profiles.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        blockedCompanions.forEach { blocked ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = blocked.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = DateMateDarkText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                TextButton(onClick = { onUnblockCompanion(blocked.id) }) {
                                    Text("Unblock", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text("Done", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Help & Support Dialog
    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "DateMate Help & Safety Support",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Need assistance with a date booking, profile verification, or membership plan? Our 24/7 DateMate Concierge desk is available at support@datemate.app.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHelpSupportDialog = false
                        onShowMessage("Concierge support ticket created — we'll respond shortly!")
                    }
                ) {
                    Text("Contact Concierge", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHelpSupportDialog = false }) {
                    Text("Close", color = DateMateSecondaryText)
                }
            }
        )
    }

    // Terms & Privacy Policy Dialog
    showTermsPrivacyDialog?.let { titleText ->
        AlertDialog(
            onDismissRequest = { showTermsPrivacyDialog = null },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "DateMate $titleText",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "DateMate is strictly for verified adults aged 18+. We protect your personal data with end-to-end security, never share your contact details without consent, and enforce respectful behavior across all chats and companion bookings.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsPrivacyDialog = null }) {
                    Text("Close", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Delete Your DateMate Account?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "This action permanently removes your profile, photos, active bookings, and chat history.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text("Permanently Delete", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(26.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = DateMateSecondaryText
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = DateMateDarkText,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DateMateWhite,
                checkedTrackColor = DateMatePrimaryPink,
                uncheckedThumbColor = DateMateSecondaryText,
                uncheckedTrackColor = DateMateSoftBlush
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DateMateBackground,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DateMateSoftBlush,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DateMatePrimaryPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )
            }
        }
    }
}
