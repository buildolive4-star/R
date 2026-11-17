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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
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
import com.example.data.repository.GalleryRepository
import com.example.notifications.PixoraNotificationManager
import com.example.ui.admin.AdminPasscodeSheet
import com.example.ui.admin.AdminStudioScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.detail.ImageDetailScreen
import com.example.ui.gallery.CollectionsScreen
import com.example.ui.gallery.ExploreScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.gallery.SavedScreen
import com.example.ui.gallery.SettingsScreen
import com.example.ui.theme.PixoraTheme
import com.example.ui.theme.ThemeMode
import com.example.workers.BackgroundSyncScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_EXPLORE = "explore"
const val ROUTE_COLLECTIONS = "collections"
const val ROUTE_SAVED = "saved"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_DETAIL = "detail/{imageId}"
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
        route = ROUTE_COLLECTIONS,
        labelRes = R.string.nav_collections,
        selectedIcon = Icons.Rounded.Collections,
        unselectedIcon = Icons.Outlined.Collections,
        testTag = "nav_tab_collections"
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
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
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

            PixoraTheme(themeMode = themeMode) {
                PixoraUserPanelApp(
                    prefsRepo = prefsRepo,
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
fun PixoraUserPanelApp(
    prefsRepo: ThemePreferencesRepository,
    deepLinkImageId: String?,
    onConsumeDeepLink: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasNotificationPermission by remember {
        mutableStateOf(PixoraNotificationManager.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val viewModel: GalleryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GalleryViewModel(
                    repository = GalleryRepository(db),
                    bookmarksRepository = LocalBookmarksRepository(app),
                    testUploadsRepository = LocalTestUploadsRepository(app),
                    prefsRepository = ThemePreferencesRepository(app),
                    notifiedImagesStore = NotifiedImagesStore(app),
                    hiddenRevealStateStore = HiddenRevealStateStore(app),
                    appPackageName = app.packageName,
                )
            }
        }
    )

    val galleryState by viewModel.galleryState.collectAsStateWithLifecycle()
    val filteredImagesState by viewModel.filteredImages.collectAsStateWithLifecycle()
    val featuredImages by viewModel.featuredImages.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val isStaggeredLayout by viewModel.isStaggeredLayout.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val backgroundSyncEnabled by viewModel.backgroundSyncEnabled.collectAsStateWithLifecycle()
    val lastSyncCheckEpochMs by viewModel.lastSyncCheckEpochMs.collectAsStateWithLifecycle()

    var showAdminPasscodeSheet by remember { mutableStateOf(false) }
    var activeBannerImage by remember { mutableStateOf<GalleryImage?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
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

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevelRoute = currentRoute in TopTabs.map { it.route }

    LaunchedEffect(deepLinkImageId) {
        if (!deepLinkImageId.isNullOrBlank()) {
            navController.navigate("detail/$deepLinkImageId") {
                launchSingleTop = true
            }
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
                                    filteredImagesState = filteredImagesState,
                                    featuredImages = featuredImages,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    bookmarkedIds = bookmarkedIds,
                                    isStaggeredLayout = isStaggeredLayout,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSearchQueryChange = viewModel::updateSearchQuery,
                                    onResetFilters = viewModel::resetFilters,
                                    onToggleLayout = viewModel::toggleLayout,
                                    onCheckNow = { viewModel.triggerBackgroundCheckNow(context) },
                                    onSecretAdminTrigger = { showAdminPasscodeSheet = true },
                                    onImageClick = { img ->
                                        if (img.isDeletedBlurred) {
                                            viewModel.notifyExpiredBlurredTap()
                                        } else {
                                            navController.navigate("detail/${img.id}")
                                        }
                                    },
                                    onToggleBookmark = viewModel::toggleBookmark
                                )
                            }

                            composable(ROUTE_COLLECTIONS) {
                                CollectionsScreen(
                                    collections = collections,
                                    onSelectCollection = { categoryName ->
                                        viewModel.selectCategory(categoryName)
                                        navController.navigate(ROUTE_EXPLORE) {
                                            popUpTo(ROUTE_EXPLORE) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }

                            composable(ROUTE_SAVED) {
                                SavedScreen(
                                    bookmarks = bookmarks,
                                    onOpenImageId = { imageId ->
                                        navController.navigate("detail/$imageId")
                                    },
                                    onRemoveBookmark = viewModel::removeBookmark
                                )
                            }

                            composable(ROUTE_SETTINGS) {
                                val galleryCount = (galleryState as? UiState.Success)?.data?.size ?: 0
                                SettingsScreen(
                                    galleryCount = galleryCount,
                                    savedCount = bookmarks.size,
                                    hasNotificationPermission = hasNotificationPermission,
                                    backgroundSyncEnabled = backgroundSyncEnabled,
                                    lastSyncCheckEpochMs = lastSyncCheckEpochMs,
                                    themeMode = themeMode,
                                    isStaggeredLayout = isStaggeredLayout,
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
                                    onLayoutModeChange = viewModel::setStaggeredLayout,
                                    onClearSaved = viewModel::clearAllBookmarks,
                                    onSecretAdminTrigger = { showAdminPasscodeSheet = true },
                                    onSendTestAlert = {
                                        val sample = (galleryState as? UiState.Success)?.data?.firstOrNull()
                                            ?: GalleryImage(
                                                id = "test_alert_preview",
                                                title = "New Companion Moment",
                                                category = "Date Night",
                                                price = "₹299",
                                            )
                                        viewModel.emitInAppAlertPreview(sample)
                                    }
                                )
                            }

                            composable(
                                route = ROUTE_DETAIL,
                                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
                            ) { entry ->
                                val imageId = entry.arguments?.getString("imageId").orEmpty()
                                val pagerImages = remember(galleryState, bookmarks, imageId) {
                                    viewModel.resolvePagerImagesFromState(galleryState, bookmarks, imageId)
                                }

                                ImageDetailScreen(
                                    images = pagerImages,
                                    initialImageId = imageId,
                                    bookmarkedIds = bookmarkedIds,
                                    onToggleBookmark = viewModel::toggleBookmark,
                                    onRevealHiddenImage = { img -> viewModel.revealHiddenImage(img) },
                                    onShowMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_ADMIN) {
                                val adminViewModel: AdminViewModel = viewModel(
                                    factory = viewModelFactory {
                                        initializer {
                                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                                "APPLICATION_KEY missing from CreationExtras"
                                            }
                                            val databaseId = app.getString(R.string.firestore_database_id)
                                            AdminViewModel(
                                                uploadsRepository = LocalTestUploadsRepository(app),
                                                galleryRepository = GalleryRepository(FirebaseFirestore.getInstance(databaseId)),
                                            )
                                        }
                                    }
                                )
                                val publishState by adminViewModel.publishState.collectAsStateWithLifecycle()
                                val uploadedImages by adminViewModel.uploadedImages.collectAsStateWithLifecycle()

                                val adminSnackbar = remember { SnackbarHostState() }

                                LaunchedEffect(adminViewModel) {
                                    adminViewModel.events.collect { msg ->
                                        adminSnackbar.showSnackbar(msg)
                                    }
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
                                    publishState = publishState,
                                    uploadedImages = uploadedImages,
                                    onUploadPicture = { uri, manualUrl, title, desc, cat, price, tags, featured, isHidden ->
                                        adminViewModel.uploadPicture(
                                            context = context,
                                            selectedUri = uri,
                                            manualImageUrl = manualUrl,
                                            title = title,
                                            description = desc,
                                            category = cat,
                                            price = price,
                                            tagsRaw = tags,
                                            featured = featured,
                                            isHidden = isHidden,
                                        )
                                    },
                                    onResetPublishState = adminViewModel::resetPublishState,
                                    onToggleFeatured = adminViewModel::toggleFeatured,
                                    onRefreshBlurredUpload = adminViewModel::refreshBlurredUpload,
                                    onRefreshAllBlurred = adminViewModel::refreshAllBlurredUploads,
                                    onUpdateMetadata = adminViewModel::updateMetadata,
                                    onRemoveUpload = adminViewModel::removeUpload,
                                    onClearAllUploads = adminViewModel::clearAllUploads,
                                    onBack = { navController.popBackStack() },
                                    snackbarHostState = adminSnackbar,
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp-style floating top notification banner
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
