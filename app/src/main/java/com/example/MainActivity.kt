package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.example.data.model.ActiveCallUi
import com.example.data.model.GalleryImage
import com.example.data.model.UiState
import com.example.data.preferences.HiddenRevealStateStore
import com.example.data.preferences.LocalBookmarksRepository
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.ThemePreferencesRepository
import com.example.data.repository.CallRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.GalleryRepository
import com.example.data.repository.UserCrmRepository
import com.example.notifications.OneSignalPushManager
import com.example.notifications.PixoraNotificationManager
import com.example.ui.admin.AdminPasscodeSheet
import com.example.ui.admin.AdminStudioScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.call.AudioCallOverlay
import com.example.ui.chat.ChatOnboardingSheet
import com.example.ui.chat.ChatRoomScreen
import com.example.ui.chat.ChatsScreen
import com.example.ui.chat.SessionAndChatViewModel
import com.example.ui.components.InAppBannerEvent
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.LaunchBannerOverlay
import com.example.ui.detail.ImageDetailScreen
import com.example.ui.gallery.ExploreScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.gallery.SavedScreen
import com.example.ui.gallery.SettingsScreen
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateWhite
import com.example.ui.theme.JustUsTheme
import com.example.ui.theme.ThemeMode
import com.example.workers.BackgroundSyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
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
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
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
        selectedIcon = Icons.Rounded.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
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
    private val pendingDeepLinkChat = mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            )
        )
        PixoraNotificationManager.ensureChannel(this)
        lifecycleScope.launch {
            if (NotifiedImagesStore(applicationContext).isBackgroundSyncEnabled()) {
                BackgroundSyncScheduler.schedulePeriodicSync(applicationContext)
            }
        }
        handleDeepLinkIntent(intent)

        setContent {
            val prefsRepo = remember { ThemePreferencesRepository(applicationContext) }
            val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)

            val sessionViewModel: SessionAndChatViewModel = viewModel(
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        SessionAndChatViewModel(
                            userCrmRepository = UserCrmRepository(db),
                            chatRepository = ChatRepository(db),
                            callRepository = CallRepository(db),
                            appContext = app.applicationContext,
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
                    deepLinkChat = pendingDeepLinkChat.value,
                    onConsumeDeepLinkImage = { pendingDeepLinkImageId.value = null },
                    onConsumeDeepLinkChat = { pendingDeepLinkChat.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val imageId = intent?.getStringExtra(PixoraNotificationManager.EXTRA_IMAGE_ID)
        if (!imageId.isNullOrBlank()) {
            pendingDeepLinkImageId.value = imageId
        }
        val chatId = intent?.getStringExtra(PixoraNotificationManager.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            val role = intent.getStringExtra(PixoraNotificationManager.EXTRA_CHAT_ROLE) ?: "client"
            pendingDeepLinkChat.value = chatId to role
        }
    }
}

@Composable
fun JustUsAppRoot(
    prefsRepo: ThemePreferencesRepository,
    sessionViewModel: SessionAndChatViewModel,
    isStaffTheme: Boolean,
    deepLinkImageId: String?,
    deepLinkChat: Pair<String, String>?,
    onConsumeDeepLinkImage: () -> Unit,
    onConsumeDeepLinkChat: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val notifiedStore = remember(context) { NotifiedImagesStore(context.applicationContext) }
    val db = remember(context) {
        FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id))
    }
    val chatRepo = remember(db) { ChatRepository(db) }

    DisposableEffect(lifecycleOwner, sessionViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    OneSignalPushManager.isAppInForeground = true
                    sessionViewModel.setOnlineState(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    OneSignalPushManager.isAppInForeground = false
                    sessionViewModel.setOnlineState(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            OneSignalPushManager.isAppInForeground = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMicAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            val action = pendingMicAction
            pendingMicAction = null
            if (granted) {
                action?.invoke()
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Microphone permission is required for WebRTC voice calls.")
                }
            }
        }
    )

    fun runWithMicPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingMicAction = action
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val viewModel: GalleryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val appDb = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                GalleryViewModel(
                    repository = GalleryRepository(appDb),
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
    val selectedVibe by viewModel.selectedVibe.collectAsStateWithLifecycle()
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
    val activeCallUi by sessionViewModel.activeCallUi.collectAsStateWithLifecycle()
    val isVerifiedAdminForCalls by sessionViewModel.isVerifiedAdminForCalls.collectAsStateWithLifecycle()
    val launchBanner by sessionViewModel.launchBanner.collectAsStateWithLifecycle()

    val adminChatsObserved by remember(isVerifiedAdminForCalls) {
        if (isVerifiedAdminForCalls) chatRepo.observeAllChatsForAdmin() else flowOf(emptyList())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(clientChats) {
        sessionViewModel.markChatsDeliveredIfNeeded(clientChats, "client")
    }
    LaunchedEffect(staffInboxChats) {
        sessionViewModel.markChatsDeliveredIfNeeded(staffInboxChats, "staff")
    }
    LaunchedEffect(adminChatsObserved, isVerifiedAdminForCalls) {
        if (isVerifiedAdminForCalls) {
            sessionViewModel.markChatsDeliveredIfNeeded(adminChatsObserved, "admin")
        }
    }

    val totalUnreadMessages = remember(clientChats, staffInboxChats) {
        clientChats.sumOf { it.unreadForClient } + staffInboxChats.sumOf { it.unreadForStaff }
    }

    var showAdminPasscodeSheet by remember { mutableStateOf(false) }
    var activeBannerEvent by remember { mutableStateOf<InAppBannerEvent?>(null) }
    var launchBannerDismissed by rememberSaveable { mutableStateOf(false) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentViewingChatId = backStackEntry?.arguments?.getString("chatId").orEmpty()
    val currentViewingChatRole = backStackEntry?.arguments?.getString("senderRole").orEmpty()
    val isTopLevelRoute = currentRoute in TopTabs.map { it.route }

    val oneSignalDeepLinkImageId by OneSignalPushManager.deepLinkImageId.collectAsStateWithLifecycle()
    val oneSignalDeepLinkChat by OneSignalPushManager.deepLinkChat.collectAsStateWithLifecycle()

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

    var hasFiredHomeWelcomePush by rememberSaveable { mutableStateOf(false) }

    // 0. Automatic Home Page Welcome Push Notification once 3D Welcome Screen is dismissed
    LaunchedEffect(launchBannerDismissed) {
        if (launchBannerDismissed && !hasFiredHomeWelcomePush) {
            hasFiredHomeWelcomePush = true
            delay(650)
            viewModel.triggerBackgroundCheckNow(context)
        }
    }

    // 1. Live Image / Profile Upload & Refresh Notifications
    LaunchedEffect(viewModel) {
        viewModel.newArrivalEvents.collect { newImage ->
            activeBannerEvent = InAppBannerEvent.ProfileArrival(newImage)
            PixoraNotificationManager.playAlertChime(context)
            scope.launch {
                PixoraNotificationManager.notifyNewImage(context, newImage)
            }
        }
    }

    // 2. Live Chat Message Notifications (Client Replies + Staff Inbox + Admin Inquiries)
    LaunchedEffect(clientChats, staffInboxChats, adminChatsObserved, isVerifiedAdminForCalls) {
        data class PendingChatAlert(
            val chatId: String,
            val role: String,
            val senderName: String,
            val message: String,
            val avatar: String,
            val updatedMs: Long,
            val unreadCount: Int,
        )
        val candidates = mutableListOf<PendingChatAlert>()
        clientChats.filter { it.unreadForClient > 0 && it.lastSenderRole != "client" }.forEach { thread ->
            candidates.add(
                PendingChatAlert(
                    chatId = thread.id,
                    role = "client",
                    senderName = thread.profileName.ifBlank { "Companion" },
                    message = thread.lastMessage,
                    avatar = thread.profilePhotoUrl,
                    updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                    unreadCount = thread.unreadForClient,
                )
            )
        }
        staffInboxChats.filter { it.unreadForStaff > 0 && it.lastSenderRole == "client" }.forEach { thread ->
            candidates.add(
                PendingChatAlert(
                    chatId = thread.id,
                    role = "staff",
                    senderName = thread.clientName.ifBlank { "Client" },
                    message = thread.lastMessage,
                    avatar = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                    updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                    unreadCount = thread.unreadForStaff,
                )
            )
        }
        if (isVerifiedAdminForCalls) {
            adminChatsObserved.filter { it.unreadForAdmin > 0 && it.lastSenderRole == "client" }.forEach { thread ->
                candidates.add(
                    PendingChatAlert(
                        chatId = thread.id,
                        role = "admin",
                        senderName = "${thread.clientName.ifBlank { "Client" }} → ${thread.profileName}",
                        message = thread.lastMessage,
                        avatar = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                        updatedMs = thread.updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
                        unreadCount = thread.unreadForAdmin,
                    )
                )
            }
        }

        for (item in candidates.distinctBy { "${it.chatId}_${it.role}" }) {
            val trackKey = "${item.chatId}_${item.role}"
            val lastNotified = notifiedStore.getLastNotifiedChatEpoch(trackKey)
            val messageStamp = (
                (item.updatedMs xor item.message.hashCode().toLong() xor (item.unreadCount.toLong() shl 16))
                    and Long.MAX_VALUE
                ).coerceAtLeast(1L)
            if (messageStamp != lastNotified) {
                val isCurrentlyInsideSameRoleChat =
                    currentRoute == ROUTE_CHAT_ROOM &&
                        currentViewingChatId == item.chatId &&
                        currentViewingChatRole == item.role
                notifiedStore.markChatNotified(trackKey, messageStamp)
                if (!isCurrentlyInsideSameRoleChat) {
                    activeBannerEvent = InAppBannerEvent.ChatMessageArrival(
                        chatId = item.chatId,
                        senderRoleForOpen = item.role,
                        title = item.senderName,
                        preview = item.message.ifBlank { "New message received" },
                        avatarUrl = item.avatar,
                    )
                    PixoraNotificationManager.playAlertChime(context)
                    PixoraNotificationManager.notifyNewChatMessage(
                        context = context,
                        chatId = item.chatId,
                        recipientRole = item.role,
                        senderName = item.senderName,
                        messageText = item.message,
                        avatarUrl = item.avatar,
                    )
                }
            }
        }
    }

    // 3. Incoming WebRTC Call Notification
    LaunchedEffect(sessionViewModel) {
        sessionViewModel.incomingCallAlert.collect { incoming ->
            PixoraNotificationManager.notifyIncomingCall(
                context = context,
                callId = incoming.id,
                callerName = incoming.callerName,
                profileName = incoming.profileName,
            )
        }
    }

    LaunchedEffect(activeBannerEvent) {
        if (activeBannerEvent != null) {
            delay(5000)
            activeBannerEvent = null
        }
    }

    LaunchedEffect(deepLinkImageId, oneSignalDeepLinkImageId) {
        val targetImageId = deepLinkImageId?.takeIf { it.isNotBlank() } ?: oneSignalDeepLinkImageId
        if (!targetImageId.isNullOrBlank()) {
            navController.navigate("detail/$targetImageId") { launchSingleTop = true }
            if (!deepLinkImageId.isNullOrBlank()) onConsumeDeepLinkImage()
            if (!oneSignalDeepLinkImageId.isNullOrBlank()) OneSignalPushManager.consumeDeepLinkImage()
        }
    }

    LaunchedEffect(deepLinkChat, oneSignalDeepLinkChat) {
        val targetChat = deepLinkChat ?: oneSignalDeepLinkChat
        if (targetChat != null) {
            navController.navigate("chat/${targetChat.first}/${targetChat.second}") {
                launchSingleTop = true
            }
            if (deepLinkChat != null) onConsumeDeepLinkChat()
            if (oneSignalDeepLinkChat != null) OneSignalPushManager.consumeDeepLinkChat()
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
                        DateMateLuxuryFloatingNavBar(
                            currentRoute = currentRoute,
                            totalUnreadMessages = totalUnreadMessages,
                            savedCount = bookmarks.size,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(ROUTE_EXPLORE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
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
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (tab.route == ROUTE_CHATS && totalUnreadMessages > 0) {
                                                    Badge { Text("${totalUnreadMessages.coerceAtMost(99)}") }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = label
                                            )
                                        }
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
                                    selectedVibe = selectedVibe,
                                    searchQuery = searchQuery,
                                    bookmarkedIds = bookmarkedIds,
                                    isStaffTheme = isStaffTheme,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSelectVibe = viewModel::selectVibe,
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
                                val presenceKeys = remember(clientChats, staffInboxChats) {
                                    (clientChats.map { it.staffOwnerUid.ifBlank { UserCrmRepository.ADMIN_PRESENCE_ID } } +
                                        staffInboxChats.map { it.clientUid })
                                        .filter { it.isNotBlank() }
                                        .distinct()
                                }
                                val peerPresences by remember(presenceKeys) {
                                    sessionViewModel.observePresenceMap(presenceKeys)
                                }.collectAsStateWithLifecycle(initialValue = emptyMap())

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
                                    peerPresences = peerPresences,
                                    onDeleteChat = { chatId ->
                                        sessionViewModel.deleteChatThread(chatId)
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
                                    oneSignalPushConfigured = OneSignalPushManager.isFullyConfigured(),
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
                                    onRevealHiddenImage = { img, photoIdx ->
                                        viewModel.revealHiddenImage(img, photoIdx)
                                    },
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
                                val peerPresence by remember(chatThread?.clientUid, chatThread?.staffOwnerUid, senderRole) {
                                    sessionViewModel.observePeerPresence(chatThread, senderRole)
                                }.collectAsStateWithLifecycle(initialValue = null)

                                LaunchedEffect(chatId, senderRole, messages.size) {
                                    sessionViewModel.markChatRead(chatId, senderRole)
                                }

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
                                    onStartVoiceCall = {
                                        val active = chatThread ?: return@ChatRoomScreen
                                        runWithMicPermission {
                                            sessionViewModel.startVoiceCallFromChat(context, active, senderRole)
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    peerPresence = peerPresence,
                                    onDeleteMessages = { messageIds, remainingMessages ->
                                        val active = chatThread ?: return@ChatRoomScreen
                                        sessionViewModel.deleteChatMessages(
                                            chat = active,
                                            messageIds = messageIds,
                                            remainingMessages = remainingMessages,
                                        )
                                    },
                                    onDeleteChat = {
                                        sessionViewModel.deleteChatThread(chatId) {
                                            navController.popBackStack()
                                        }
                                    },
                                )
                            }

                            composable(ROUTE_ADMIN) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY])
                                            val appDb = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                                            AdminViewModel(
                                                uploadsRepository = LocalTestUploadsRepository(app),
                                                galleryRepository = GalleryRepository(appDb),
                                                userCrmRepository = UserCrmRepository(appDb),
                                                chatRepository = ChatRepository(appDb),
                                            )
                                        }
                                    }
                                )

                                val adminAuthUser by adminViewModel.authUser.collectAsStateWithLifecycle()
                                val isVerifiedAdmin by adminViewModel.isVerifiedAdmin.collectAsStateWithLifecycle()
                                val isSuperAdmin by adminViewModel.isSuperAdmin.collectAsStateWithLifecycle()
                                val adminEmails by adminViewModel.adminEmails.collectAsStateWithLifecycle()
                                val adminAccessCodes by adminViewModel.adminAccessCodes.collectAsStateWithLifecycle()
                                val adminMembers by adminViewModel.adminMembers.collectAsStateWithLifecycle()
                                val myAdminMember by adminViewModel.myAdminMember.collectAsStateWithLifecycle()
                                val isAdminBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
                                val publishState by adminViewModel.publishState.collectAsStateWithLifecycle()
                                val managedProfiles by adminViewModel.combinedManagedImages.collectAsStateWithLifecycle()
                                val pagedUsers by adminViewModel.pagedUsers.collectAsStateWithLifecycle()
                                val filteredUsers by adminViewModel.filteredUsers.collectAsStateWithLifecycle()
                                val hasMoreUsers by adminViewModel.hasMoreUsersToLoad.collectAsStateWithLifecycle()
                                val selectedUserFilter by adminViewModel.selectedUserFilter.collectAsStateWithLifecycle()
                                val pendingStaffUsers by adminViewModel.pendingStaffUsers.collectAsStateWithLifecycle()
                                val allChats by adminViewModel.allChats.collectAsStateWithLifecycle()
                                val selectedProfileContact by adminViewModel.selectedProfileContact.collectAsStateWithLifecycle()
                                val adminAuthError by adminViewModel.authErrorMessage.collectAsStateWithLifecycle()
                                val adminLaunchBanner by adminViewModel.launchBanner.collectAsStateWithLifecycle()

                                LaunchedEffect(adminViewModel) {
                                    adminViewModel.events.collect { msg ->
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                                LaunchedEffect(adminViewModel) {
                                    adminViewModel.uploadedPreviewAlert.collect { newImg ->
                                        viewModel.emitInAppAlertPreview(newImg)
                                    }
                                }
                                LaunchedEffect(isVerifiedAdmin) {
                                    sessionViewModel.refreshAdminCallMonitoring()
                                }

                                AdminStudioScreen(
                                    authUser = adminAuthUser,
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
                                    selectedUserFilter = selectedUserFilter,
                                    pendingStaffUsers = pendingStaffUsers,
                                    allChats = allChats,
                                    selectedProfileContact = selectedProfileContact,
                                    onSignInAdminGoogle = { adminViewModel.signInAdminWithGoogle(context) },
                                    onSignOutAdmin = { adminViewModel.signOutAdmin(context) },
                                    onSaveMainAdminProfile = adminViewModel::saveMainAdminProfile,
                                    onCreateAccessCode = adminViewModel::createAdminAccessCode,
                                    onDeleteAccessCode = adminViewModel::deleteAdminAccessCode,
                                    onRemoveAdminMember = adminViewModel::removeAdminMember,
                                    onRedeemAccessCode = adminViewModel::redeemAdminAccessCode,
                                    onAddAdminEmail = adminViewModel::addAdminEmail,
                                    onRemoveAdminEmail = adminViewModel::removeAdminEmail,
                                    onPublishCompanionProfile = { uris, manualUrl, name, phone, ageText, price, address, role, tier, category, bio, hiddenTimed ->
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
                                            category = category,
                                            bio = bio,
                                            hiddenTimed = hiddenTimed,
                                        )
                                    },
                                    onResetPublishState = adminViewModel::resetPublishState,
                                    onRefreshAllBlurred = adminViewModel::refreshAllBlurredUploads,
                                    onRefreshProfile = adminViewModel::refreshBlurredUpload,
                                    onUpdateProfile = adminViewModel::updateCompanionProfileDetails,
                                    onDeleteProfile = adminViewModel::deleteProfile,
                                    onSelectUserFilter = adminViewModel::selectUserFilter,
                                    onLoadNextUsersPage = adminViewModel::loadNextUsersPage,
                                    onSetUserLeadTag = adminViewModel::setUserLeadTag,
                                    onSetUserModeration = adminViewModel::setUserModerationStatus,
                                    onCallUser = { targetUser, relatedChat ->
                                        runWithMicPermission {
                                            sessionViewModel.startDirectAdminVoiceCallToUser(context, targetUser, relatedChat)
                                        }
                                    },
                                    onApproveStaff = adminViewModel::approveStaffUser,
                                    onRejectStaff = adminViewModel::rejectStaffUser,
                                    onLoadPrivateContact = adminViewModel::loadPrivateContactForProfile,
                                    onOpenChatAsAdmin = { chatId ->
                                        navController.navigate("chat/$chatId/admin") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    snackbarHostState = snackbarHostState,
                                    authErrorMessage = adminAuthError,
                                    launchBanner = adminLaunchBanner,
                                    onPowerResetAllChats = adminViewModel::powerResetAllChats,
                                    onUploadLaunchBanner = { selectedUri, manualUrl, title, onSuccess ->
                                        adminViewModel.uploadLaunchBannerImage(
                                            context = context,
                                            selectedUri = selectedUri,
                                            manualUrl = manualUrl,
                                            title = title,
                                            onSuccess = onSuccess,
                                        )
                                    },
                                    onRemoveLaunchBanner = adminViewModel::removeLaunchBanner,
                                )
                            }
                        }
                    }
                }
            }

            // Floating In-App Alert Banner (New Profiles + Chat Messages)
            InAppNotificationBanner(
                event = activeBannerEvent,
                onOpenProfile = { target ->
                    activeBannerEvent = null
                    navController.navigate("detail/${target.id}") { launchSingleTop = true }
                },
                onOpenChat = { chatId, role ->
                    activeBannerEvent = null
                    navController.navigate("chat/$chatId/$role") { launchSingleTop = true }
                },
                onDismiss = { activeBannerEvent = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // 3D Glassmorphic Welcome Screen & 3-Second Admin Launch Info Overlay
            val currentLaunchBanner = launchBanner
            if (!launchBannerDismissed) {
                LaunchBannerOverlay(
                    banner = currentLaunchBanner,
                    onSkip = { launchBannerDismissed = true }
                )
            }

            // Full-Screen WebRTC Voice Call Overlay
            if (activeCallUi !is ActiveCallUi.Idle) {
                AudioCallOverlay(
                    callUi = activeCallUi,
                    currentUid = firebaseUser?.uid.orEmpty(),
                    onAccept = { session ->
                        runWithMicPermission {
                            sessionViewModel.acceptIncomingCall(context, session)
                        }
                    },
                    onDecline = { session ->
                        sessionViewModel.declineIncomingCall(context, session)
                    },
                    onHangUp = { sessionViewModel.hangUpCall(context) },
                    onToggleMute = sessionViewModel::toggleCallMute,
                    onToggleSpeaker = sessionViewModel::toggleCallSpeaker,
                )
            }
        }
    }

    val onboardingTarget = pendingChatProfile
    if (onboardingTarget != null) {
        ChatOnboardingSheet(
            targetProfile = onboardingTarget,
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
            onDismiss = { showAdminPasscodeSheet = false },
            onUnlocked = {
                showAdminPasscodeSheet = false
                sessionViewModel.unlockPinAdminAndRefreshMonitoring()
                navController.navigate(ROUTE_ADMIN) {
                    launchSingleTop = true
                }
            }
        )
    }
}

@Composable
private fun DateMateLuxuryFloatingNavBar(
    currentRoute: String?,
    totalUnreadMessages: Int,
    savedCount: Int,
    onNavigate: (String) -> Unit,
) {
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = (navBottomPadding + 10.dp).coerceAtLeast(14.dp)
            )
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = DateMateWhite,
            shadowElevation = 18.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DateMateSoftBorder, RoundedCornerShape(30.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                TopTabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val label = stringResource(tab.labelRes)
                    val badgeCount = when (tab.route) {
                        ROUTE_CHATS -> totalUnreadMessages
                        ROUTE_SAVED -> savedCount
                        else -> 0
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (selected) DateMateSoftBlush else Color.Transparent)
                            .clickable { onNavigate(tab.route) }
                            .testTag(tab.testTag)
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = label,
                                    tint = if (selected) DateMatePrimaryPink else DateMateSecondaryText,
                                    modifier = Modifier.size(23.dp)
                                )
                                if (badgeCount > 0) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-4).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(DateMatePrimaryPink)
                                    ) {
                                        Text(
                                            text = badgeCount.coerceAtMost(99).toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = DateMateWhite,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (selected) DateMatePrimaryPink else DateMateSecondaryText,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = if (selected) 18.dp else 0.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(DateMatePrimaryPink)
                            )
                        }
                    }
                }
            }
        }
    }
}
