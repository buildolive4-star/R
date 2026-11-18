package com.example.ui.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
import com.example.data.model.AdminUserFilter
import com.example.data.model.ChatThread
import com.example.data.model.CompanionPrivateContact
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileTier
import com.example.data.model.UserAccount
import com.google.firebase.auth.FirebaseUser

private val COMPANION_CATEGORIES = listOf(
    "Companion",
    "Date Night",
    "Dinner",
    "Travel",
    "VIP",
    "Verified Staff",
)

private val QUICK_PRICES = listOf("₹299", "₹499", "₹799", "₹1,499", "₹2,999")

@Composable
fun AdminStudioScreen(
    authUser: FirebaseUser?,
    isVerifiedAdmin: Boolean,
    isSuperAdmin: Boolean,
    adminEmails: List<AdminEmailEntry>,
    adminAccessCodes: List<AdminAccessCode> = emptyList(),
    adminMembers: List<AdminMemberProfile> = emptyList(),
    myAdminMember: AdminMemberProfile? = null,
    isAuthBusy: Boolean = false,
    publishState: AdminPublishState,
    managedProfiles: List<GalleryImage>,
    pagedUsers: List<UserAccount>,
    totalFilteredUsers: Int,
    hasMoreUsers: Boolean,
    selectedUserFilter: AdminUserFilter,
    pendingStaffUsers: List<UserAccount>,
    allChats: List<ChatThread>,
    selectedProfileContact: Pair<String, CompanionPrivateContact>?,
    onSignInAdminGoogle: () -> Unit,
    onSignOutAdmin: () -> Unit,
    onSaveMainAdminProfile: (name: String, phone: String, email: String) -> Unit = { _, _, _ -> },
    onCreateAccessCode: (code: String, label: String) -> Unit = { _, _ -> },
    onDeleteAccessCode: (code: String) -> Unit = {},
    onRemoveAdminMember: (uid: String) -> Unit = {},
    onRedeemAccessCode: (name: String, phone: String, email: String, code: String) -> Unit = { _, _, _, _ -> },
    onAddAdminEmail: (String) -> Unit,
    onRemoveAdminEmail: (String) -> Unit,
    onPublishCompanionProfile: (
        selectedUris: List<Uri>,
        manualUrl: String,
        name: String,
        phone: String,
        ageText: String,
        price: String,
        address: String,
        role: String,
        tier: String,
        category: String,
        bio: String,
        hiddenTimed: Boolean,
    ) -> Unit,
    onResetPublishState: () -> Unit,
    onRefreshAllBlurred: () -> Unit,
    onRefreshProfile: (String) -> Unit,
    onUpdateProfile: (
        profileId: String,
        name: String,
        ageText: String,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        phone: String?,
        address: String?,
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onDeleteProfile: (String) -> Unit,
    onSelectUserFilter: (AdminUserFilter) -> Unit,
    onLoadNextUsersPage: () -> Unit,
    onSetUserLeadTag: (String, String) -> Unit,
    onSetUserModeration: (String, String) -> Unit,
    onCallUser: (UserAccount, ChatThread?) -> Unit = { _, _ -> },
    onApproveStaff: (UserAccount, String) -> Unit,
    onRejectStaff: (String) -> Unit,
    onLoadPrivateContact: (String) -> Unit,
    onOpenChatAsAdmin: (String) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    authErrorMessage: String? = null,
) {
    BackHandler(onBack = onBack)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMultiAdminDialog by remember { mutableStateOf(false) }

    val blurredCount = remember(managedProfiles) {
        managedProfiles.count { it.isDeletedBlurred || it.isRevealStarted }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Admin Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("admin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.admin_studio_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = authUser?.email?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.admin_studio_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isSuperAdmin) {
                        FilledTonalButton(
                            onClick = { showMultiAdminDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("admin_multi_admin_button")
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Codes & Admins", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (authUser == null) {
                        Button(
                            onClick = onSignInAdminGoogle,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("admin_google_signin_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cloud Sign-In", style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (!isVerifiedAdmin) {
                        OutlinedButton(
                            onClick = onSignOutAdmin,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("Switch", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (!isVerifiedAdmin) {
                AdminAccessGateView(
                    authUser = authUser,
                    isBusy = isAuthBusy,
                    authErrorMessage = authErrorMessage,
                    onSignInGoogle = onSignInAdminGoogle,
                    onSignOut = onSignOutAdmin,
                    onRedeemAccessCode = onRedeemAccessCode,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Top Refresh Secret Photos bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    FilledTonalButton(
                        onClick = onRefreshAllBlurred,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("admin_refresh_all_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (blurredCount > 0) {
                                stringResource(R.string.admin_refresh_all_blurred, blurredCount)
                            } else {
                                stringResource(R.string.admin_refresh_top_ready)
                            }
                        )
                    }
                }

                // 3 Top Tabs: Create / Chats (CRM) / Staff
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Create") },
                        icon = { Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("admin_tab_create")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Chats (${totalFilteredUsers})") },
                        icon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("admin_tab_chats")
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Text(
                                if (pendingStaffUsers.isNotEmpty()) "Staff (${pendingStaffUsers.size})"
                                else "Staff"
                            )
                        },
                        icon = { Icon(Icons.Rounded.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("admin_tab_staff")
                    )
                }

                when (selectedTabIndex) {
                    0 -> AdminCreateProfileTab(
                        publishState = publishState,
                        onPublishProfile = onPublishCompanionProfile,
                        onResetPublishState = onResetPublishState,
                        modifier = Modifier.weight(1f)
                    )

                    1 -> AdminUsersCrmTab(
                        pagedUsers = pagedUsers,
                        totalFilteredCount = totalFilteredUsers,
                        hasMoreUsers = hasMoreUsers,
                        selectedFilter = selectedUserFilter,
                        allChats = allChats,
                        onSelectFilter = onSelectUserFilter,
                        onLoadNextPage = onLoadNextUsersPage,
                        onSetLeadTag = onSetUserLeadTag,
                        onSetModeration = onSetUserModeration,
                        onOpenChatAsAdmin = onOpenChatAsAdmin,
                        onCallUser = onCallUser,
                        modifier = Modifier.weight(1f)
                    )

                    else -> AdminStaffTab(
                        pendingStaffUsers = pendingStaffUsers,
                        managedProfiles = managedProfiles,
                        allChats = allChats,
                        selectedProfileContact = selectedProfileContact,
                        onApproveStaff = onApproveStaff,
                        onRejectStaff = onRejectStaff,
                        onLoadPrivateContact = onLoadPrivateContact,
                        onUpdateProfile = onUpdateProfile,
                        onRefreshProfile = onRefreshProfile,
                        onDeleteProfile = onDeleteProfile,
                        onOpenChatAsAdmin = onOpenChatAsAdmin,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showMultiAdminDialog && isSuperAdmin) {
        MainAdminTransferDialog(
            authUserEmail = authUser?.email.orEmpty(),
            myAdminMember = myAdminMember,
            adminAccessCodes = adminAccessCodes,
            adminMembers = adminMembers,
            adminEmails = adminEmails,
            isBusy = isAuthBusy,
            onSaveMainAdminProfile = onSaveMainAdminProfile,
            onCreateAccessCode = onCreateAccessCode,
            onDeleteAccessCode = onDeleteAccessCode,
            onRemoveAdminMember = onRemoveAdminMember,
            onAddEmail = onAddAdminEmail,
            onRemoveEmail = onRemoveAdminEmail,
            onDismiss = { showMultiAdminDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminCreateProfileTab(
    publishState: AdminPublishState,
    onPublishProfile: (
        selectedUris: List<Uri>,
        manualUrl: String,
        name: String,
        phone: String,
        ageText: String,
        price: String,
        address: String,
        role: String,
        tier: String,
        category: String,
        bio: String,
        hiddenTimed: Boolean,
    ) -> Unit,
    onResetPublishState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var manualUrl by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("22") }
    var price by remember { mutableStateOf("₹499") }
    var address by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("GF") }
    var tier by remember { mutableStateOf(ProfileTier.GRID_9_16) }
    var category by remember { mutableStateOf(COMPANION_CATEGORIES.first()) }
    var bio by remember { mutableStateOf("") }
    var hiddenTimed by remember { mutableStateOf(false) }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 8),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                selectedUris = uris
                onResetPublishState()
            }
        }
    )

    LaunchedEffect(publishState) {
        if (publishState is AdminPublishState.Success) {
            selectedUris = emptyList()
            manualUrl = ""
            name = ""
            phone = ""
            address = ""
            bio = ""
            hiddenTimed = false
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_create_profile_list")
    ) {
        item(key = "composer_card") {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create Companion Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Upload multiple photos + full details. Phone & Address stay private to Admin only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_pick_multi_photos")
                    ) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (selectedUris.isEmpty()) "Select Multiple Photos (up to 8)"
                            else "Selected ${selectedUris.size} Photo(s) · Tap to Change"
                        )
                    }

                    if (selectedUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(selectedUris) { uri ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(uri).crossfade(true).build(),
                                    contentDescription = "Selected photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 76.dp, height = 104.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Home Placement Ratio", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = tier == ProfileTier.VIP_16_9,
                            onClick = { tier = ProfileTier.VIP_16_9 },
                            label = { Text("Top VIP (16:9 · 3s Slide)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tier_vip_chip")
                        )
                        FilterChip(
                            selected = tier == ProfileTier.GRID_9_16,
                            onClick = { tier = ProfileTier.GRID_9_16 },
                            label = { Text("2×2 Grid (9:16)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tier_grid_chip")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("GF", "BF").forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(if (r == "GF") "GF Companion" else "BF Companion") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            singleLine = true,
                            label = { Text("Companion Name *") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("admin_profile_name_input")
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(2) },
                            singleLine = true,
                            label = { Text("Age *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.75f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            singleLine = true,
                            label = { Text("Mobile No. (Private)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            singleLine = true,
                            label = { Text("Manual Price *") },
                            placeholder = { Text("₹499 / custom") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.95f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QUICK_PRICES.forEach { preset ->
                            FilterChip(
                                selected = price == preset,
                                onClick = { price = preset },
                                label = { Text(preset) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        singleLine = true,
                        label = { Text("Address (Private to Admin)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        COMPANION_CATEGORIES.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        minLines = 2,
                        maxLines = 4,
                        label = { Text("Profile Bio / Vibes") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        singleLine = true,
                        label = { Text("Optional Direct HTTPS Photo URL") },
                        placeholder = { Text("https://i.ibb.co/...") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("40-Sec Disappearing Photo", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "No timer pre-warning shown. Soft memory blur after 40s.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(checked = hiddenTimed, onCheckedChange = { hiddenTimed = it })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(visible = publishState is AdminPublishState.Error) {
                        val msg = (publishState as? AdminPublishState.Error)?.message.orEmpty()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    AnimatedVisibility(visible = publishState is AdminPublishState.Success) {
                        val msg = (publishState as? AdminPublishState.Success)?.message.orEmpty()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    val busy = publishState is AdminPublishState.Uploading
                    Button(
                        onClick = {
                            onPublishProfile(
                                selectedUris,
                                manualUrl,
                                name,
                                phone,
                                ageText,
                                price,
                                address,
                                role,
                                tier,
                                category,
                                bio,
                                hiddenTimed,
                            )
                        },
                        enabled = !busy,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("admin_publish_profile_button")
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publishing Companion…")
                        } else {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.admin_upload_cta), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}


