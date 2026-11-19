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
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import com.example.R
import com.example.data.model.AdminAccessCode
import com.example.data.model.AdminEmailEntry
import com.example.data.model.AdminMemberProfile
import com.example.data.model.AdminUserFilter
import com.example.data.model.BookingRatePlan
import com.example.data.model.ChatThread
import com.example.data.model.CompanionPrivateContact
import com.example.data.model.DateMateBooking
import com.example.data.model.FilterCategoryGroup
import com.example.data.model.GalleryImage
import com.example.data.model.LaunchBannerConfig
import com.example.data.model.PaymentServerConfig
import com.example.data.model.ProfileTier
import com.example.data.model.UserAccount
import com.example.data.model.UserComplaint
import com.example.ui.components.PixoraNetworkImage
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
        filterTags: List<String>,
        advanceBookingAmount: Int,
        bookingPlans: List<String>,
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
        filterTags: List<String>,
        advanceBookingAmount: Int,
        bookingPlans: List<String>,
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
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
    launchBanner: LaunchBannerConfig? = null,
    onPowerResetAllChats: () -> Unit = {},
    onUploadLaunchBanner: (selectedUri: Uri?, manualUrl: String, title: String, onSuccess: () -> Unit) -> Unit = { _, _, _, _ -> },
    onRemoveLaunchBanner: (onSuccess: () -> Unit) -> Unit = {},
    customHomeFilters: List<String> = emptyList(),
    onAddCustomHomeFilter: (String) -> Unit = {},
    onRemoveCustomHomeFilter: (String) -> Unit = {},
    filterTaxonomy: List<FilterCategoryGroup> = FilterCategoryGroup.defaultTaxonomy(),
    paymentServerConfig: PaymentServerConfig = PaymentServerConfig(),
    allBookings: List<DateMateBooking> = emptyList(),
    allComplaints: List<UserComplaint> = emptyList(),
    onSaveFilterTaxonomy: (List<FilterCategoryGroup>) -> Unit = {},
    onSavePaymentServerConfig: (String, Int) -> Unit = { _, _ -> },
    onTestPaymentServer: (String) -> Unit = {},
    onApproveStaffPayout: (String, String) -> Unit = { _, _ -> },
    onRejectStaffPayout: (String, String) -> Unit = { _, _ -> },
    onResolveComplaint: (String, String) -> Unit = { _, _ -> },
) {
    BackHandler(onBack = onBack)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMultiAdminDialog by remember { mutableStateOf(false) }
    var showPowerResetDialog by remember { mutableStateOf(false) }
    var showLaunchBannerDialog by remember { mutableStateOf(false) }
    var showHomeFiltersDialog by remember { mutableStateOf(false) }

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
                    } else {
                        OutlinedButton(
                            onClick = onSignOutAdmin,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("admin_switch_account_button")
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
                // Top Refresh Secret Photos + Main Admin Quick Controls (Power Reset + Upload Image Info)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onRefreshAllBlurred,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(46.dp)
                                .testTag("admin_refresh_all_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (blurredCount > 0) {
                                    stringResource(R.string.admin_refresh_all_blurred, blurredCount)
                                } else {
                                    stringResource(R.string.admin_refresh_top_ready)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        FilledTonalButton(
                            onClick = { showHomeFiltersDialog = true },
                            enabled = !isAuthBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("admin_home_filters_button")
                        ) {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Filters (${filterTaxonomy.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (isSuperAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { showPowerResetDialog = true },
                                enabled = !isAuthBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_power_reset_button")
                            ) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Power Reset", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showLaunchBannerDialog = true },
                                enabled = !isAuthBusy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                    contentColor = MaterialTheme.colorScheme.secondary,
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_upload_banner_button")
                            ) {
                                Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (launchBanner?.isReadyToDisplay == true) "Image Info (Live)" else "Upload Image Info",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // 4 Top Tabs: Create / Chats / Staff / Bookings & Payouts
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Create", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        modifier = Modifier.testTag("admin_tab_create")
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Chats (${totalFilteredUsers})", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        icon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        modifier = Modifier.testTag("admin_tab_chats")
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = {
                            Text(
                                text = if (pendingStaffUsers.isNotEmpty()) "Staff (${pendingStaffUsers.size})" else "Staff",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = { Icon(Icons.Rounded.Badge, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        modifier = Modifier.testTag("admin_tab_staff")
                    )
                    Tab(
                        selected = selectedTabIndex == 3,
                        onClick = { selectedTabIndex = 3 },
                        text = {
                            Text(
                                text = if (allBookings.isNotEmpty()) "Bookings (${allBookings.size})" else "Bookings",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(17.dp)) },
                        modifier = Modifier.testTag("admin_tab_bookings")
                    )
                }

                when (selectedTabIndex) {
                    0 -> AdminCreateProfileTab(
                        publishState = publishState,
                        filterTaxonomy = filterTaxonomy,
                        defaultAdvanceInr = paymentServerConfig.defaultAdvanceInr,
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

                    2 -> AdminStaffTab(
                        pendingStaffUsers = pendingStaffUsers,
                        managedProfiles = managedProfiles,
                        allChats = allChats,
                        selectedProfileContact = selectedProfileContact,
                        filterTaxonomy = filterTaxonomy,
                        onApproveStaff = onApproveStaff,
                        onRejectStaff = onRejectStaff,
                        onLoadPrivateContact = onLoadPrivateContact,
                        onUpdateProfile = onUpdateProfile,
                        onRefreshProfile = onRefreshProfile,
                        onDeleteProfile = onDeleteProfile,
                        onOpenChatAsAdmin = onOpenChatAsAdmin,
                        modifier = Modifier.weight(1f)
                    )

                    else -> AdminBookingsAndPayoutsTab(
                        isSuperAdmin = isSuperAdmin,
                        isBusy = isAuthBusy,
                        paymentServerConfig = paymentServerConfig,
                        allBookings = allBookings,
                        allComplaints = allComplaints,
                        onSavePaymentServerConfig = onSavePaymentServerConfig,
                        onTestPaymentServer = onTestPaymentServer,
                        onApproveStaffPayout = onApproveStaffPayout,
                        onRejectStaffPayout = onRejectStaffPayout,
                        onResolveComplaint = onResolveComplaint,
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

    if (showPowerResetDialog && isSuperAdmin) {
        AlertDialog(
            onDismissRequest = { showPowerResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(30.dp)
                )
            },
            title = { Text("Power Reset All Chats?") },
            text = {
                Text(
                    "This will permanently delete all user chat threads, every nested message, and clear call sessions from Firebase to free database load. Companion profiles and user accounts stay untouched."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPowerResetDialog = false
                        onPowerResetAllChats()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_power_reset_button")
                ) {
                    Text("Yes, Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPowerResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLaunchBannerDialog && isSuperAdmin) {
        LaunchBannerManagerDialog(
            currentBanner = launchBanner,
            isBusy = isAuthBusy,
            onUpload = { uri, url, title ->
                onUploadLaunchBanner(uri, url, title) {
                    showLaunchBannerDialog = false
                }
            },
            onRemove = {
                onRemoveLaunchBanner {
                    showLaunchBannerDialog = false
                }
            },
            onDismiss = { showLaunchBannerDialog = false }
        )
    }

    if (showHomeFiltersDialog) {
        AdminFilterTaxonomyDialog(
            filterTaxonomy = filterTaxonomy,
            customFilters = customHomeFilters,
            isBusy = isAuthBusy,
            onSaveTaxonomy = onSaveFilterTaxonomy,
            onAddQuickFilter = onAddCustomHomeFilter,
            onRemoveQuickFilter = onRemoveCustomHomeFilter,
            onDismiss = { showHomeFiltersDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminFilterTaxonomyDialog(
    filterTaxonomy: List<FilterCategoryGroup>,
    customFilters: List<String>,
    isBusy: Boolean,
    onSaveTaxonomy: (List<FilterCategoryGroup>) -> Unit,
    onAddQuickFilter: (String) -> Unit,
    onRemoveQuickFilter: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var groups by remember(filterTaxonomy) { mutableStateOf(filterTaxonomy) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedCategoryForOption by remember(groups) {
        mutableStateOf(groups.firstOrNull()?.category.orEmpty())
    }
    var newOptionName by remember { mutableStateOf("") }
    var quickFilterText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Admin Filter Categories & Options",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Create custom Filter Categories and sub-options. Users see all categories when tapping 'All' on Home, and you can tag profiles with these options!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Add New Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it.take(30) },
                        singleLine = true,
                        label = { Text("New Category") },
                        placeholder = { Text("e.g. Occasion / Vibe / City") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_new_filter_category_input")
                    )
                    Button(
                        onClick = {
                            val clean = newCategoryName.trim()
                            if (clean.length >= 2 && groups.none { it.category.equals(clean, ignoreCase = true) }) {
                                val updated = groups + FilterCategoryGroup(clean, emptyList())
                                groups = updated
                                selectedCategoryForOption = clean
                                newCategoryName = ""
                                onSaveTaxonomy(updated)
                            }
                        },
                        enabled = !isBusy && newCategoryName.trim().length >= 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("admin_add_filter_category_btn")
                    ) {
                        Text("+ Cat")
                    }
                }

                // Add Option inside selected Category
                if (groups.isNotEmpty()) {
                    Text(
                        text = "Select Category to Add Options:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        groups.forEach { g ->
                            FilterChip(
                                selected = selectedCategoryForOption.equals(g.category, ignoreCase = true),
                                onClick = { selectedCategoryForOption = g.category },
                                label = { Text(g.category, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newOptionName,
                            onValueChange = { newOptionName = it.take(30) },
                            singleLine = true,
                            label = { Text("Option in ${selectedCategoryForOption.ifBlank { "Category" }}") },
                            placeholder = { Text("e.g. Dinner, Night, Coffee") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_new_filter_option_input")
                        )
                        Button(
                            onClick = {
                                val opt = newOptionName.trim()
                                val targetCat = selectedCategoryForOption.ifBlank { groups.first().category }
                                if (opt.length >= 2) {
                                    val updated = groups.map { g ->
                                        if (g.category.equals(targetCat, ignoreCase = true)) {
                                            val merged = (g.options + opt).distinctBy { it.lowercase() }
                                            g.copy(options = merged)
                                        } else g
                                    }
                                    groups = updated
                                    newOptionName = ""
                                    onSaveTaxonomy(updated)
                                }
                            },
                            enabled = !isBusy && newOptionName.trim().length >= 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("admin_add_filter_option_btn")
                        ) {
                            Text("+ Opt")
                        }
                    }

                    // Active Categories & Options List (Tap Option to Delete, Trash Icon to Delete Category)
                    groups.forEach { group ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${group.category} (${group.options.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = {
                                            val updated = groups.filterNot { it.category == group.category }
                                            groups = updated
                                            onSaveTaxonomy(updated)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete ${group.category}",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (group.options.isEmpty()) {
                                    Text(
                                        text = "No options added yet.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        group.options.forEach { opt ->
                                            FilterChip(
                                                selected = true,
                                                onClick = {
                                                    val updated = groups.map { g ->
                                                        if (g.category == group.category) {
                                                            g.copy(options = g.options.filterNot { it == opt })
                                                        } else g
                                                    }
                                                    groups = updated
                                                    onSaveTaxonomy(updated)
                                                },
                                                label = { Text("$opt ×", style = MaterialTheme.typography.labelSmall) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Top-Row Home Chips
                Text(
                    text = "Quick Home Row Chips (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = quickFilterText,
                        onValueChange = { quickFilterText = it.take(28) },
                        singleLine = true,
                        label = { Text("Quick Chip") },
                        placeholder = { Text("e.g. Under ₹299") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(
                        onClick = {
                            if (quickFilterText.trim().length >= 2) {
                                onAddQuickFilter(quickFilterText.trim())
                                quickFilterText = ""
                            }
                        },
                        enabled = !isBusy && quickFilterText.trim().length >= 2,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add")
                    }
                }
                if (customFilters.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        customFilters.forEach { label ->
                            FilterChip(
                                selected = true,
                                onClick = { onRemoveQuickFilter(label) },
                                label = { Text("$label ×", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun LaunchBannerManagerDialog(
    currentBanner: LaunchBannerConfig?,
    isBusy: Boolean,
    onUpload: (Uri?, String, String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var manualUrl by remember(currentBanner?.imageUrl) { mutableStateOf(currentBanner?.imageUrl.orEmpty()) }
    var title by remember(currentBanner?.title) { mutableStateOf(currentBanner?.title.orEmpty()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
            }
        }
    )

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Launch 3s Image Info") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Shows full-screen for 3 seconds whenever any user opens the app before entering Home.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val previewModel: Any? = selectedUri ?: currentBanner?.takeIf { it.isReadyToDisplay }?.imageUrl
                if (previewModel != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (selectedUri != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(selectedUri).crossfade(true).build(),
                                    contentDescription = "Selected launch image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                PixoraNetworkImage(
                                    imageUrl = currentBanner?.imageUrl.orEmpty(),
                                    contentDescription = "Active launch image",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (currentBanner?.isReadyToDisplay == true && selectedUri == null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE · 3s",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("launch_banner_pick_button")
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedUri != null) "Change Gallery Image" else "Pick Notification Image")
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Optional Title / Caption") },
                    placeholder = { Text("Weekend VIP Offer") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manualUrl,
                    onValueChange = { manualUrl = it },
                    singleLine = true,
                    label = { Text("Or Paste Direct HTTPS Image URL") },
                    placeholder = { Text("https://i.ibb.co/...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (currentBanner?.isReadyToDisplay == true) {
                    OutlinedButton(
                        onClick = onRemove,
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("launch_banner_remove_button")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Active Launch Image")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpload(selectedUri, manualUrl, title) },
                enabled = !isBusy && (selectedUri != null || manualUrl.trim().isNotEmpty()),
                modifier = Modifier.testTag("launch_banner_publish_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Publish 3s Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminCreateProfileTab(
    publishState: AdminPublishState,
    filterTaxonomy: List<FilterCategoryGroup>,
    defaultAdvanceInr: Int,
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
        filterTags: List<String>,
        advanceBookingAmount: Int,
        bookingPlans: List<String>,
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
    var advancePriceText by remember(defaultAdvanceInr) { mutableStateOf(defaultAdvanceInr.toString()) }
    var bookingPlans by remember {
        mutableStateOf(
            listOf(
                BookingRatePlan.fromInputs("Coffee", "₹499"),
                BookingRatePlan.fromInputs("Dinner", "₹1,499"),
                BookingRatePlan.fromInputs("Night", "₹2,999"),
            )
        )
    }
    var planLabelInput by remember { mutableStateOf("Dinner") }
    var planPriceInput by remember { mutableStateOf("₹1,499") }
    var selectedFilterTags by remember { mutableStateOf<List<String>>(emptyList()) }
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
            selectedFilterTags = emptyList()
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pre-Booking Advance Price & Custom Rate Plans (Dinner / Night / Coffee / Custom)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Booking Rates & Dynamic Pre-Booking Advance",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = advancePriceText,
                                onValueChange = { advancePriceText = it.filter { ch -> ch.isDigit() }.take(6) },
                                singleLine = true,
                                label = { Text("Pre-Booking Advance Price (₹) — Any Amount *") },
                                placeholder = { Text("e.g. 299, 500, 1500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_advance_price_input")
                            )

                            Text(
                                text = "Choose Rate Options & Custom Prices (Dinner / Night / etc.)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                BookingRatePlan.PRESET_PLAN_NAMES.forEach { preset ->
                                    FilterChip(
                                        selected = planLabelInput.equals(preset, ignoreCase = true),
                                        onClick = { planLabelInput = preset },
                                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = planLabelInput,
                                    onValueChange = { planLabelInput = it.take(24) },
                                    singleLine = true,
                                    label = { Text("Option") },
                                    placeholder = { Text("Dinner") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.1f)
                                )
                                OutlinedTextField(
                                    value = planPriceInput,
                                    onValueChange = { planPriceInput = it.take(18) },
                                    singleLine = true,
                                    label = { Text("Price") },
                                    placeholder = { Text("₹1,499") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.95f)
                                )
                                Button(
                                    onClick = {
                                        if (planLabelInput.isNotBlank() && planPriceInput.isNotBlank()) {
                                            val newPlan = BookingRatePlan.fromInputs(planLabelInput, planPriceInput)
                                            bookingPlans = bookingPlans.filterNot {
                                                it.label.equals(newPlan.label, ignoreCase = true)
                                            } + newPlan
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(50.dp)
                                        .testTag("admin_add_rate_plan_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add rate plan", modifier = Modifier.size(18.dp))
                                }
                            }

                            if (bookingPlans.isNotEmpty()) {
                                Text(
                                    text = "Active Booking Plans (Tap to Remove):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    bookingPlans.forEach { plan ->
                                        FilterChip(
                                            selected = true,
                                            onClick = { bookingPlans = bookingPlans.filterNot { it == plan } },
                                            label = { Text("${plan.label}: ${plan.priceText} ×", style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Admin Dynamic Filter Category Tags for User Search
                    if (filterTaxonomy.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Profile Filter Tags (Match Home 'All' Filters)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                filterTaxonomy.forEach { group ->
                                    if (group.options.isNotEmpty()) {
                                        Text(
                                            text = group.category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            group.options.forEach { opt ->
                                                val chosen = selectedFilterTags.any { it.equals(opt, ignoreCase = true) }
                                                FilterChip(
                                                    selected = chosen,
                                                    onClick = {
                                                        selectedFilterTags = if (chosen) {
                                                            selectedFilterTags.filterNot { it.equals(opt, ignoreCase = true) }
                                                        } else {
                                                            selectedFilterTags + opt
                                                        }
                                                    },
                                                    label = { Text(opt, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                            val safeAdvance = (advancePriceText.toIntOrNull() ?: defaultAdvanceInr).coerceIn(1, 500_000)
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
                                selectedFilterTags,
                                safeAdvance,
                                bookingPlans.map { it.encode() },
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


