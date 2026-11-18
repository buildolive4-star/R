package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.GalleryImage
import com.example.data.model.UiState
import com.example.data.preferences.HiddenRevealStateStore
import com.example.data.preferences.LocalBookmarksRepository
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GalleryRepository
import com.example.data.repository.UserCrmRepository
import com.example.notifications.PixoraNotificationManager
import com.example.ui.admin.AdminPasscodeSheet
import com.example.ui.admin.AdminStudioScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.chat.ChatOnboardingSheet
import com.example.ui.chat.ChatRoomScreen
import com.example.ui.chat.ChatsScreen
import com.example.ui.chat.SessionAndChatViewModel
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.detail.ImageDetailScreen
import com.example.ui.gallery.ExploreScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.gallery.SavedScreen
import com.example.ui.gallery.SettingsScreen
import com.example.ui.theme.JustUsTheme
import com.example.ui.theme.ThemeMode
import com.example.workers.BackgroundSyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_EXPLORE = "explore"
const val ROUTE_CHATS = "chats"
const val ROUTE_SAVED = "saved"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_DETAIL = "detail/{imageId}"
const val ROUTE_CHAT_ROOM = "chat/{chatId}/{senderRole}"
const val ROUTE_ADMIN = "admin_studio"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val TopTabs = listOf(
    NavTab(
        route = ROUTE_EXPLORE,
        labelRes = R.string.nav_explore,
        selectedIcon = Icons.Rounded.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_tab_explore"
    ),
    NavTab(
        route = ROUTE_CHATS,
        labelRes = R.string.nav_chats,
        selectedIcon = Icons.AutoMirrored.Rounded.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat,
        testTag = "nav_tab_chats"
    ),
    NavTab(
        route = ROUTE_SAVED,
        labelRes = R.string.nav_saved,
        selectedIcon = Icons.Rounded.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder,
        testTag = "nav_tab_saved"
    ),
    NavTab(
        route = ROUTE_SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_settings"
    ),
)

class MainActivity : ComponentActivity() {
    private val pendingDeepLinkImageId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        PixoraNotificationManager.ensureChannel(this)
        lifecycleScope.launch {
            if (NotifiedImagesStore(applicationContext).isBackgroundSyncEnabled()) {
                BackgroundSyncScheduler.schedulePeriodicSync(applicationContext)
            }
        }
        pendingDeepLinkImageId.value = intent?.getStringExtra(PixoraNotificationManager.EXTRA_IMAGE_ID)

        setContent {
            val prefsRepo = remember { ThemePreferencesRepository(applicationContext) }
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)

            val sessionViewModel: SessionAndChatViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        SessionAndChatViewModel(
                            userCrmRepository = UserCrmRepository(db),
                            chatRepository = ChatRepository(db),
                        )
                    }
                }
            )
            val isStaffTheme by sessionViewModel.isApprovedStaffTheme.collectAsStateWithLifecycle()

            JustUsTheme(
                themeMode = themeMode,
                isStaffTheme = isStaffTheme,
            ) {
                JustUsAppRoot(
                    prefsRepo = prefsRepo,
                    sessionViewModel = sessionViewModel,
                    isStaffTheme = isStaffTheme,
                    deepLinkImageId = pendingDeepLinkImageId.value,
                    onConsumeDeepLink = { pendingDeepLinkImageId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val imageId = intent.getStringExtra(PixoraNotificationManager.EXTRA_IMAGE_ID)
        if (!imageId.isNullOrBlank()) {
            pendingDeepLinkImageId.value = imageId
        }
    }
}

@Composable
fun JustUsAppRoot(
    prefsRepo: ThemePreferencesRepository,
    sessionViewModel: SessionAndChatViewModel,
    isStaffTheme: Boolean,
    deepLinkImageId: String?,
    onConsumeDeepLink: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, sessionViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sessionViewModel.setOnlineState(true)
                Lifecycle.Event.ON_STOP -> sessionViewModel.setOnlineState(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(PixoraNotificationManager.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasNotificationPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val viewModel: GalleryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                GalleryViewModel(
                    repository = GalleryRepository(db),
                    bookmarksRepository = LocalBookmarksRepository(app),
                    testUploadsRepository = LocalTestUploadsRepository(app),
                    prefsRepository = ThemePreferencesRepository(app),
                    notifiedImagesStore = NotifiedImagesStore(app),
                    hiddenRevealStateStore = HiddenRevealStateStore(app),
                )
            }
        }
    )

    val galleryState by viewModel.galleryState.collectAsStateWithLifecycle()
    val filteredGridState by viewModel.filteredGridProfiles.collectAsStateWithLifecycle()
    val vipProfiles by viewModel.vipProfiles.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val backgroundSyncEnabled by viewModel.backgroundSyncEnabled.collectAsStateWithLifecycle()
    val lastSyncCheckEpochMs by viewModel.lastSyncCheckEpochMs.collectAsStateWithLifecycle()

    val firebaseUser by sessionViewModel.firebaseUser.collectAsStateWithLifecycle()
    val userAccount by sessionViewModel.userAccount.collectAsStateWithLifecycle()
    val isProfileBusy by sessionViewModel.isBusy.collectAsStateWithLifecycle()
    val clientChats by sessionViewModel.clientChats.collectAsStateWithLifecycle()
    val staffInboxChats by sessionViewModel.staffInboxChats.collectAsStateWithLifecycle()
    val pendingChatProfile by sessionViewModel.pendingChatProfile.collectAsStateWithLifecycle()
    val authErrorMessage by sessionViewModel.authErrorMessage.collectAsStateWithLifecycle()

    var showAdminPasscodeSheet by remember { mutableStateOf(false) }
    var activeBannerImage by remember { mutableStateOf<GalleryImage?>(null) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = currentRoute in TopTabs.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.events.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.navigateToChatId.collect { chatId ->
            navController.navigate("chat/$chatId/client") {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.newArrivalEvents.collect { newImage ->
            activeBannerImage = newImage
            scope.launch {
                PixoraNotificationManager.notifyNewImage(context, newImage)
            }
        }
    }

    LaunchedEffect(activeBannerImage) {
        if (activeBannerImage != null) {
            delay(5000)
            activeBannerImage = null
        }
    }

    LaunchedEffect(deepLinkImageId) {
        if (!deepLinkImageId.isNullOrBlank()) {
            navController.navigate("detail/$deepLinkImageId") { launchSingleTop = true }
            onConsumeDeepLink()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                contentWindowInsets = if (isTopLevelRoute) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (isTopLevelRoute && !useRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp
                        ) {
                            TopTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_EXPLORE) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(text = label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isTopLevelRoute && useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            TopTabs.forEach { tab ->
                                val selected = currentRoute == tab.route
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(ROUTE_EXPLORE) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(text = label, maxLines = 1) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = ROUTE_EXPLORE
                        ) {
                            composable(ROUTE_EXPLORE) {
                                ExploreScreen(
                                    galleryState = galleryState,
                                    filteredGridState = filteredGridState,
                                    vipProfiles = vipProfiles,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    bookmarkedIds = bookmarkedIds,
                                    isStaffTheme = isStaffTheme,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onResetFilters = viewModel::resetFilters,
                                    onCheckNow = { viewModel.triggerBackgroundCheckNow(context) },
                                    onSecretAdminTrigger = { showAdminPasscodeSheet = true },
                                    onProfileClick = { img ->
                                        if (img.isDeletedBlurred) {
                                            viewModel.notifyExpiredBlurredTap()
                                        } else {
                                            navController.navigate("detail/${img.id}")
                                        }
                                    },
                                    onChatClick = sessionViewModel::requestChatWithProfile,
                                    onToggleBookmark = viewModel::toggleBookmark,
                                )
                            }

                            composable(ROUTE_CHATS) {
                                ChatsScreen(
                                    clientChats = clientChats,
                                    staffInboxChats = staffInboxChats,
                                    userAccount = userAccount,
                                    onOpenChat = { chatId, senderRole ->
                                        navController.navigate("chat/$chatId/$senderRole") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onGoToProfile = {
                                        navController.navigate(ROUTE_SETTINGS) {
                                            popUpTo(ROUTE_EXPLORE) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }

                            composable(ROUTE_SAVED) {
                                SavedScreen(
                                    bookmarks = bookmarks,
                                    onOpenImageId = { imageId ->
                                        navController.navigate("detail/$imageId")
                                    },
                                    onRemoveBookmark = viewModel::removeBookmark,
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                val galleryCount = (galleryState as? UiState.Success)?.data?.size ?: 0
                                SettingsScreen(
                                    firebaseUser = firebaseUser,
                                    userAccount = userAccount,
                                    isProfileBusy = isProfileBusy,
                                    galleryCount = galleryCount,
                                    savedCount = bookmarks.size,
                                    hasNotificationPermission = hasNotificationPermission,
                                    backgroundSyncEnabled = backgroundSyncEnabled,
                                    lastSyncCheckEpochMs = lastSyncCheckEpochMs,
                                    themeMode = themeMode,
                                    onSignInGoogle = { sessionViewModel.signInWithGoogle(context) },
                                    onSignOut = { sessionViewModel.signOut(context) },
                                    onSaveProfile = { name, avatarUri, galleryUris, phone, ageText, address, city, bio, interests, genderPref ->
                                        sessionViewModel.saveFullProfile(
                                            context = context,
                                            name = name,
                                            selectedAvatarUri = avatarUri,
                                            selectedGalleryUris = galleryUris,
                                            phone = phone,
                                            ageText = ageText,
                                            address = address,
                                            city = city,
                                            bio = bio,
                                            interests = interests,
                                            genderPreference = genderPref,
                                        )
                                    },
                                    onApplyForStaff = sessionViewModel::applyForStaff,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    onToggleBackgroundSync = { enabled ->
                                        viewModel.setBackgroundSyncEnabled(context, enabled)
                                    },
                                    onCheckSyncNow = { viewModel.triggerBackgroundCheckNow(context) },
                                    onThemeModeChange = { mode ->
                                        scope.launch { prefsRepo.setThemeMode(mode) }
                                    },
                                    onClearSaved = viewModel::clearAllBookmarks,
                                    onSecretAdminTrigger = { showAdminPasscodeSheet = true },
                                    onSendTestAlert = {
                                        val sample = (galleryState as? UiState.Success)?.data?.firstOrNull()
                                            ?: GalleryImage(
                                                id = "test_alert_preview",
                                                title = "Verified Companion",
                                                category = "VIP",
                                                price = "₹499",
                                            )
                                        viewModel.emitInAppAlertPreview(sample)
                                    },
                                    authErrorMessage = authErrorMessage,
                                )
                            }

                            composable(
                                route = ROUTE_DETAIL,
                                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
                            ) { entry ->
                                val imageId = entry.arguments?.getString("imageId").orEmpty()
                                val profile = remember(galleryState, bookmarks, imageId) {
                                    viewModel.resolvePagerImagesFromState(galleryState, bookmarks, imageId)
                                        .firstOrNull { it.id == imageId }
                                }

                                ImageDetailScreen(
                                    profile = profile,
                                    bookmarkedIds = bookmarkedIds,
                                    onToggleBookmark = viewModel::toggleBookmark,
                                    onRevealHiddenImage = { img -> viewModel.revealHiddenImage(img) },
                                    onChatClick = sessionViewModel::requestChatWithProfile,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = ROUTE_CHAT_ROOM,
                                arguments = listOf(
                                    navArgument("chatId") { type = NavType.StringType },
                                    navArgument("senderRole") { type = NavType.StringType },
                                )
                            ) { entry ->
                                val chatId = entry.arguments?.getString("chatId").orEmpty()
                                val senderRole = entry.arguments?.getString("senderRole") ?: "client"
                                val chatThread by remember(chatId) {
                                    sessionViewModel.observeChatThread(chatId)
                                }.collectAsStateWithLifecycle(initialValue = sessionViewModel.findChatById(chatId))
                                val messages by remember(chatId) {
                                    sessionViewModel.observeMessages(chatId)
                                }.collectAsStateWithLifecycle(initialValue = emptyList())

                                ChatRoomScreen(
                                    chat = chatThread,
                                    messages = messages,
                                    senderRole = senderRole,
                                    onSendMessage = { text, onError, onSent ->
                                        val active = chatThread ?: return@ChatRoomScreen
                                        sessionViewModel.sendChatMessage(
                                            chat = active,
                                            senderRole = senderRole,
                                            text = text,
                                            onBlockedOrError = onError,
                                            onSent = onSent,
                                        )
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_ADMIN) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                            AdminViewModel(
                                                uploadsRepository = LocalTestUploadsRepository(app),
                                                galleryRepository = GalleryRepository(db),
                                                userCrmRepository = UserCrmRepository(db),
                                                chatRepository = ChatRepository(db),
                                            )
                                        }
                                    }
                                )
                                val authUser by adminViewModel.authUser.collectAsStateWithLifecycle()
                                val isVerifiedAdmin by adminViewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
                                val isSuperAdmin by adminViewModel.isSuperAdmin.collectAsStateWithLifecycle()
                                val adminEmails by adminViewModel.adminEmails.collectAsStateWithLifecycle()
                                val adminAccessCodes by adminViewModel.adminAccessCodes.collectAsStateWithLifecycle()
                                val adminMembers by adminViewModel.adminMembers.collectAsStateWithLifecycle()
                                val myAdminMember by adminViewModel.myAdminMember.collectAsStateWithLifecycle()
                                val isAdminBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
                                val publishState by adminViewModel.publishState.collectAsStateWithLifecycle()
                                val managedProfiles by adminViewModel.combinedManagedImages.collectAsStateWithLifecycle()
                                val filteredUsers by adminViewModel.filteredUsers.collectAsStateWithLifecycle()
                                val pagedUsers by adminViewModel.pagedUsers.collectAsStateWithLifecycle()
                                val hasMoreUsers by adminViewModel.hasMoreUsersToLoad.collectAsStateWithLifecycle()
                                val selectedFilter by adminViewModel.selectedUserFilter.collectAsStateWithLifecycle()
                                val pendingStaffUsers by adminViewModel.pendingStaffUsers.collectAsStateWithLifecycle()
                                val allChats by adminViewModel.allChats.collectAsStateWithLifecycle()
                                val selectedContact by adminViewModel.selectedProfileContact.collectAsStateWithLifecycle()
                                val adminAuthError by adminViewModel.authErrorMessage.collectAsStateWithLifecycle()

                                val adminSnackbar = remember { SnackbarHostState() }

                                LaunchedEffect(adminViewModel) {
                                    adminViewModel.events.collect { msg -> adminSnackbar.showSnackbar(msg) }
                                }
                                LaunchedEffect(adminViewModel) {
                                    adminViewModel.uploadedPreviewAlert.collect { previewImg ->
                                        activeBannerImage = previewImg
                                        scope.launch {
                                            PixoraNotificationManager.notifyNewImage(context, previewImg)
                                        }
                                    }
                                }

                                AdminStudioScreen(
                                    authUser = authUser,
                                    isVerifiedAdmin = isVerifiedAdmin,
                                    isSuperAdmin = isSuperAdmin,
                                    adminEmails = adminEmails,
                                    adminAccessCodes = adminAccessCodes,
                                    adminMembers = adminMembers,
                                    myAdminMember = myAdminMember,
                                    isAuthBusy = isAdminBusy,
                                    publishState = publishState,
                                    managedProfiles = managedProfiles,
                                    pagedUsers = pagedUsers,
                                    totalFilteredUsers = filteredUsers.size,
                                    hasMoreUsers = hasMoreUsers,
                                    selectedUserFilter = selectedFilter,
                                    pendingStaffUsers = pendingStaffUsers,
                                    allChats = allChats,
                                    selectedProfileContact = selectedContact,
                                    onSignInAdminGoogle = { adminViewModel.signInAdminWithGoogle(context) },
                                    onSignOutAdmin = { adminViewModel.signOutAdmin(context) },
                                    onSaveMainAdminProfile = adminViewModel::saveMainAdminProfile,
                                    onCreateAccessCode = adminViewModel::createAdminAccessCode,
                                    onDeleteAccessCode = adminViewModel::deleteAdminAccessCode,
                                    onRemoveAdminMember = adminViewModel::removeAdminMember,
                                    onRedeemAccessCode = adminViewModel::redeemAdminAccessCode,
                                    onAddAdminEmail = adminViewModel::addAdminEmail,
                                    onRemoveAdminEmail = adminViewModel::removeAdminEmail,
                                    onPublishCompanionProfile = { uris, manualUrl, name, phone, ageText, price, address, role, tier, cat, bio, hiddenTimed ->
                                        adminViewModel.publishCompanionProfile(
                                            context = context,
                                            selectedUris = uris,
                                            manualUrl = manualUrl,
                                            name = name,
                                            phone = phone,
                                            ageText = ageText,
                                            price = price,
                                            address = address,
                                            role = role,
                                            tier = tier,
                                            category = cat,
                                            bio = bio,
                                            hiddenTimed = hiddenTimed,
                                        )
                                    },
                                    onResetPublishState = adminViewModel::resetPublishState,
                                    onRefreshAllBlurred = adminViewModel::refreshAllBlurredUploads,
                                    onRefreshProfile = adminViewModel::refreshBlurredUpload,
                                    onDeleteProfile = adminViewModel::deleteProfile,
                                    onSelectUserFilter = adminViewModel::selectUserFilter,
                                    onLoadNextUsersPage = adminViewModel::loadNextUsersPage,
                                    onSetUserLeadTag = adminViewModel::setUserLeadTag,
                                    onSetUserModeration = adminViewModel::setUserModerationStatus,
                                    onApproveStaff = adminViewModel::approveStaffUser,
                                    onRejectStaff = adminViewModel::rejectStaffUser,
                                    onLoadPrivateContact = adminViewModel::loadPrivateContactForProfile,
                                    onOpenChatAsAdmin = { chatId ->
                                        navController.navigate("chat/$chatId/admin") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    snackbarHostState = adminSnackbar,
                                    authErrorMessage = adminAuthError,
                                )
                            }
                        }
                    }
                }
            }

            InAppNotificationBanner(
                image = activeBannerImage,
                onViewClick = { img ->
                    activeBannerImage = null
                    if (img.isDeletedBlurred) {
                        viewModel.notifyExpiredBlurredTap()
                    } else {
                        navController.navigate("detail/${img.id}") {
                            launchSingleTop = true
                        }
                    }
                },
                onDismiss = { activeBannerImage = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    val targetChatProfile = pendingChatProfile
    if (targetChatProfile != null) {
        ChatOnboardingSheet(
            targetProfile = targetChatProfile,
            existingAccount = userAccount,
            defaultDisplayName = firebaseUser?.displayName.orEmpty(),
            isBusy = isProfileBusy,
            isSignedIn = firebaseUser != null,
            errorMessage = authErrorMessage,
            onSubmit = { name, phone, ageText, address ->
                sessionViewModel.completeOnboardingAndOpenChat(
                    activityContext = context,
                    name = name,
                    phone = phone,
                    ageText = ageText,
                    address = address,
                )
            },
            onDismiss = sessionViewModel::dismissChatOnboarding,
        )
    }

    if (showAdminPasscodeSheet) {
        AdminPasscodeSheet(
            onUnlocked = {
                showAdminPasscodeSheet = false
                navController.navigate(ROUTE_ADMIN) {
                    launchSingleTop = true
                }
            },
            onDismiss = { showAdminPasscodeSheet = false }
        )
    }
}
