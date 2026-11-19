package com.example.ui.gallery

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.FilterCategoryGroup
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileSortMode
import com.example.data.model.PromoTickerConfig
import com.example.data.model.UiState
import com.example.data.preferences.UserEngagementPrefs
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.Animated3DPageBackdrop
import com.example.ui.components.CupidLikeSuccessOverlay
import com.example.ui.components.DailyCheckInSpinDialog
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMate3DGlowingIconButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GalleryGridCard
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.LoveVibeQuizDialog
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.ShimmerBox
import com.example.ui.components.VipShowcaseCarousel
import com.example.ui.datemate.Floating3DHeartsBackground
import com.example.ui.theme.DateMateSuccessGreen

@Composable
fun ExploreScreen(
    galleryState: UiState<List<GalleryImage>>,
    filteredGridState: UiState<List<GalleryImage>>,
    vipProfiles: List<GalleryImage>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    bookmarkedIds: Set<String>,
    isStaffTheme: Boolean,
    isStaffOnline: Boolean = false,
    onToggleStaffOnline: (Boolean) -> Unit = {},
    onSelectCategory: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onResetFilters: () -> Unit,
    onCheckNow: () -> Unit,
    onSecretAdminTrigger: () -> Unit,
    onProfileClick: (GalleryImage) -> Unit,
    onChatClick: (GalleryImage) -> Unit,
    onToggleBookmark: (GalleryImage) -> Unit,
    smartFilters: List<String> = GalleryViewModel.DEFAULT_SMART_FILTERS,
    selectedSmartFilter: String = GalleryViewModel.FILTER_ALL,
    onSelectSmartFilter: (String) -> Unit = {},
    filterTaxonomy: List<FilterCategoryGroup> = FilterCategoryGroup.defaultTaxonomy(),
    userCity: String = "",
    userAddress: String = "",
    userGenderPreference: String = "",
    onLocationClick: () -> Unit = {},
    promoTickerConfig: PromoTickerConfig = PromoTickerConfig(),
    sortMode: ProfileSortMode = ProfileSortMode.RECOMMENDED,
    onSelectSortMode: (ProfileSortMode) -> Unit = {},
    engagementPrefs: UserEngagementPrefs = UserEngagementPrefs(),
    onClaimDailySpin: (coins: Int, rewardTitle: String) -> Unit = { _, _ -> },
    onSaveVibePersonality: (vibeName: String, filterKeyword: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val allProfiles = (galleryState as? UiState.Success)?.data.orEmpty()
    val totalCount = allProfiles.size
    val onlineStaffCount = allProfiles.count { it.staffOnline }
    val recentlyViewedProfiles = remember(allProfiles, engagementPrefs.recentlyViewedIds) {
        val byId = allProfiles.associateBy { it.id }
        engagementPrefs.recentlyViewedIds.mapNotNull { byId[it] }.filter { !it.isDeletedBlurred }
    }
    val context = LocalContext.current
    var cupidCelebrationName by remember { mutableStateOf<String?>(null) }
    var showAllFiltersSheet by remember { mutableStateOf(false) }
    var showInlineSearch by remember { mutableStateOf(false) }
    var showDailySpinDialog by remember { mutableStateOf(false) }
    var showVibeQuizDialog by remember { mutableStateOf(false) }

    val handleLikeToggle: (GalleryImage) -> Unit = { img ->
        val wasBookmarked = bookmarkedIds.contains(img.id)
        onToggleBookmark(img)
        if (!wasBookmarked) {
            cupidCelebrationName = img.title
        }
    }

    val pushPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onCheckNow()
    }

    val handlePushBellClick: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pushPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onCheckNow()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    )
                )
            )
    ) {
        // Animated 3D aurora orbs + floating glowing hearts backdrop
        Animated3DPageBackdrop()
        Floating3DHeartsBackground(heartCount = 8)

        when (filteredGridState) {
            is UiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "loading_header") {
                        DateMateScrollableHomeHeader(
                            isStaffTheme = isStaffTheme,
                            isStaffOnline = isStaffOnline,
                            onToggleStaffOnline = onToggleStaffOnline,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            onLocationClick = onLocationClick,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            onOpenAllFiltersSheet = { showAllFiltersSheet = true },
                            userCity = userCity,
                            userAddress = userAddress,
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }, key = "loading_lovemate") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Animated3DMascotOrb(
                                mascot = DateMateMascot.LOVE_MATE,
                                size = 56.dp,
                                showOrbitSparkles = true,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LoveMate is loading nearby matches…",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            }

            is UiState.Error -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        DateMateScrollableHomeHeader(
                            isStaffTheme = isStaffTheme,
                            isStaffOnline = isStaffOnline,
                            onToggleStaffOnline = onToggleStaffOnline,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            onLocationClick = onLocationClick,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            onOpenAllFiltersSheet = { showAllFiltersSheet = true },
                            userCity = userCity,
                            userAddress = userAddress,
                        )
                    }
                    ErrorStateCard(
                        message = filteredGridState.message,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            is UiState.Success -> {
                val gridProfiles = filteredGridState.data
                val showVipCarousel = vipProfiles.isNotEmpty()

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 112.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("explore_uniform_grid")
                ) {
                    // Clean Header (DateMate + Location directly below, NO vertical line, NO Search Bar!)
                    item(span = { GridItemSpan(maxLineSpan) }, key = "scrollable_home_header") {
                        DateMateScrollableHomeHeader(
                            isStaffTheme = isStaffTheme,
                            isStaffOnline = isStaffOnline,
                            onToggleStaffOnline = onToggleStaffOnline,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            onLocationClick = onLocationClick,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            onOpenAllFiltersSheet = { showAllFiltersSheet = true },
                            userCity = userCity,
                            userAddress = userAddress,
                        )
                    }

                    // Feature 2: Main Admin Live Flash Offer / Announcement Ticker
                    if (promoTickerConfig.isReadyToShow) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "promo_ticker_banner") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_promo_ticker_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Rounded.Campaign,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = promoTickerConfig.badgeLabel.ifBlank { "LIVE" },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = promoTickerConfig.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Features 3, 4 & 9: Compact Discovery Toolbar (On-Demand Search, Sort, Daily Bonus Spin & Vibe Quiz)
                    item(span = { GridItemSpan(maxLineSpan) }, key = "quick_discovery_toolbar") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item(key = "quick_search_toggle") {
                                    FilterChip(
                                        selected = showInlineSearch || searchQuery.isNotBlank(),
                                        onClick = {
                                            showInlineSearch = !showInlineSearch
                                            if (!showInlineSearch && searchQuery.isNotBlank()) {
                                                onSearchQueryChange("")
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                                        },
                                        label = { Text(if (searchQuery.isNotBlank()) "\"$searchQuery\"" else "Search") },
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("home_quick_search_toggle")
                                    )
                                }
                                item(key = "quick_sort_cycle") {
                                    FilterChip(
                                        selected = sortMode != ProfileSortMode.RECOMMENDED,
                                        onClick = {
                                            val values = ProfileSortMode.entries
                                            val next = values[(values.indexOf(sortMode) + 1) % values.size]
                                            onSelectSortMode(next)
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.SwapVert, contentDescription = "Sort", modifier = Modifier.size(16.dp))
                                        },
                                        label = { Text(sortMode.label) },
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("home_sort_mode_chip")
                                    )
                                }
                                item(key = "quick_daily_spin") {
                                    FilterChip(
                                        selected = false,
                                        onClick = { showDailySpinDialog = true },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFD97706))
                                        },
                                        label = { Text("🪙 ${engagementPrefs.dateCoins} · Spin") },
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("home_daily_spin_chip")
                                    )
                                }
                                item(key = "quick_vibe_quiz") {
                                    FilterChip(
                                        selected = engagementPrefs.vibePersonality.isNotBlank(),
                                        onClick = { showVibeQuizDialog = true },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        label = {
                                            Text(
                                                engagementPrefs.vibePersonality.ifBlank { "Love Vibe Quiz" },
                                                maxLines = 1
                                            )
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("home_vibe_quiz_chip")
                                    )
                                }
                            }

                            if (showInlineSearch) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text("Search name, city, dinner, night…") },
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { onSearchQueryChange("") }) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("home_inline_search_field")
                                )
                            }
                        }
                    }

                    // Feature 10: Recently Viewed Companions Compact Strip
                    if (recentlyViewedProfiles.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "recently_viewed_strip") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Recently Viewed",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recentlyViewedProfiles, key = { "recent_${it.id}" }) { recent ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(62.dp)
                                                .clickable { onProfileClick(recent) }
                                                .testTag("recent_profile_${recent.id}")
                                        ) {
                                            Box {
                                                PixoraNetworkImage(
                                                    imageUrl = recent.displayThumbnailUrl,
                                                    contentDescription = recent.title,
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(CircleShape)
                                                        .border(
                                                            2.dp,
                                                            if (recent.staffOnline) DateMateSuccessGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                            CircleShape
                                                        )
                                                )
                                                if (recent.staffOnline) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.BottomEnd)
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(DateMateSuccessGreen)
                                                            .border(1.5.dp, Color.White, CircleShape)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = recent.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Admin-Uploaded 16:9 VIP Spotlight
                    if (showVipCarousel) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "vip_section_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp, bottom = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Animated3DMascotOrb(
                                        mascot = DateMateMascot.ROYAL_MATE,
                                        size = 40.dp,
                                        showOrbitSparkles = true,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = stringResource(R.string.vip_showcase_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "RoyalMate · 16:9 Exclusive Spotlights",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = "👑 ${vipProfiles.size} VIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, key = "vip_carousel") {
                            VipShowcaseCarousel(
                                profiles = vipProfiles,
                                bookmarkedIds = bookmarkedIds,
                                onProfileClick = onProfileClick,
                                onChatClick = onChatClick,
                                onToggleBookmark = handleLikeToggle,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    if (gridProfiles.isEmpty() && !showVipCarousel) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_gallery_card") {
                            if (totalCount == 0) {
                                EmptyGalleryState(
                                    title = stringResource(R.string.empty_gallery_title),
                                    subtitle = stringResource(R.string.empty_gallery_subtitle),
                                    actionLabel = stringResource(R.string.check_now_button),
                                    onAction = onCheckNow,
                                    mascot = DateMateMascot.LOVE_MATE,
                                )
                            } else {
                                EmptyGalleryState(
                                    title = stringResource(R.string.empty_search_title),
                                    subtitle = stringResource(R.string.empty_search_subtitle),
                                    actionLabel = stringResource(R.string.reset_filters),
                                    onAction = onResetFilters,
                                    mascot = DateMateMascot.CUPID,
                                )
                            }
                        }
                    } else if (gridProfiles.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "grid_section_header") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Animated3DMascotOrb(
                                        mascot = DateMateMascot.CUPID,
                                        size = 38.dp,
                                        showOrbitSparkles = true,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "${stringResource(R.string.grid_companions_title)} (${gridProfiles.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = if (userCity.isNotBlank()) {
                                                "Cupid picks near $userCity"
                                            } else {
                                                "Tap any profile to Chat or Book a Date"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (onlineStaffCount > 0 && selectedSmartFilter != GalleryViewModel.FILTER_ONLINE) {
                                        Surface(
                                            onClick = { onSelectSmartFilter(GalleryViewModel.FILTER_ONLINE) },
                                            shape = RoundedCornerShape(999.dp),
                                            color = Color(0xFF16A34A).copy(alpha = 0.16f),
                                            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.65f)),
                                            modifier = Modifier.testTag("home_online_count_pill")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF22C55E))
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = "$onlineStaffCount Online",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF16A34A)
                                                )
                                            }
                                        }
                                    }
                                    if (selectedSmartFilter != GalleryViewModel.FILTER_ALL || selectedCategory != GalleryViewModel.CATEGORY_ALL) {
                                        Surface(
                                            onClick = onResetFilters,
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "Reset ✕",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        items(gridProfiles, key = { it.id }) { profile ->
                            val isNearby = remember(profile.id, profile.city, userCity, userAddress) {
                                GalleryViewModel.isProfileNearby(profile, userCity, userAddress)
                            }
                            GalleryGridCard(
                                image = profile,
                                isBookmarked = bookmarkedIds.contains(profile.id),
                                isNearby = isNearby,
                                onClick = { onProfileClick(profile) },
                                onChatClick = { onChatClick(profile) },
                                onToggleBookmark = { handleLikeToggle(profile) }
                            )
                        }
                    }
                }
            }
        }

        // Animated 3D Cupid Match / Like Success Celebration Overlay
        CupidLikeSuccessOverlay(
            likedProfileName = cupidCelebrationName,
            onFinished = { cupidCelebrationName = null },
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (showAllFiltersSheet) {
        AllFilterCategoriesBottomSheet(
            filterTaxonomy = filterTaxonomy,
            smartFilters = smartFilters,
            selectedCategory = selectedCategory,
            selectedSmartFilter = selectedSmartFilter,
            onSelectCategory = onSelectCategory,
            onSelectFilterOption = { option ->
                onSelectSmartFilter(option)
                showAllFiltersSheet = false
            },
            onResetAll = {
                onResetFilters()
                showAllFiltersSheet = false
            },
            onDismiss = { showAllFiltersSheet = false }
        )
    }

    if (showDailySpinDialog) {
        DailyCheckInSpinDialog(
            engagementPrefs = engagementPrefs,
            onClaimReward = onClaimDailySpin,
            onDismiss = { showDailySpinDialog = false },
        )
    }

    if (showVibeQuizDialog) {
        LoveVibeQuizDialog(
            currentVibe = engagementPrefs.vibePersonality,
            onSaveVibeAndMatch = onSaveVibePersonality,
            onDismiss = { showVibeQuizDialog = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateMateScrollableHomeHeader(
    isStaffTheme: Boolean,
    isStaffOnline: Boolean,
    onToggleStaffOnline: (Boolean) -> Unit,
    onSecretAdminTrigger: () -> Unit,
    onPushBellClick: () -> Unit,
    onLocationClick: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    smartFilters: List<String>,
    selectedSmartFilter: String,
    onSelectSmartFilter: (String) -> Unit,
    onOpenAllFiltersSheet: () -> Unit,
    userCity: String,
    userAddress: String,
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    val displayLocation = remember(userCity, userAddress) {
        when {
            userCity.isNotBlank() -> userCity.trim()
            userAddress.isNotBlank() -> userAddress.split(",").firstOrNull()?.trim().orEmpty().ifBlank { "Set Location" }
            else -> "Set Location"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        // Top Brand Row: LoveMate Mascot + DateMate + Location directly below (NO vertical line!) + Clean 3D Glowing Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            val now = System.currentTimeMillis()
                            tapCount = if (now - lastTapMs <= 2200L) tapCount + 1 else 1
                            lastTapMs = now
                            if (tapCount >= 5) {
                                tapCount = 0
                                onSecretAdminTrigger()
                            }
                        },
                        onLongClick = onSecretAdminTrigger
                    )
                    .testTag("explore_brand_header")
            ) {
                Animated3DMascotOrb(
                    mascot = DateMateMascot.LOVE_MATE,
                    size = 46.dp,
                    showOrbitSparkles = true,
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        if (isStaffTheme) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                onClick = { onToggleStaffOnline(!isStaffOnline) },
                                shape = RoundedCornerShape(50),
                                color = if (isStaffOnline) Color(0xFF16A34A).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isStaffOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.testTag("home_staff_online_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .alpha(if (isStaffOnline) pulseAlpha else 1f)
                                            .clip(CircleShape)
                                            .background(if (isStaffOnline) Color(0xFF22C55E) else Color(0xFF9CA3AF))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isStaffOnline) "ONLINE" else "OFFLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isStaffOnline) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Location name right below "DateMate" — clean, zero vertical line!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onLocationClick)
                            .padding(vertical = 2.dp)
                            .testTag("home_location_chip")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Your Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = displayLocation,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Polished Single 3D Glowing Notification Button (Clean, no cluttered pill next to it)
            Box(contentAlignment = Alignment.Center) {
                DateMate3DGlowingIconButton(
                    icon = Icons.Rounded.NotificationsActive,
                    contentDescription = "Instant Push Notifications",
                    onClick = onPushBellClick,
                    style = GlowButtonStyle.ROSE_PINK,
                    size = 46.dp,
                    iconSize = 22.dp,
                    modifier = Modifier.testTag("home_push_bell_button")
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                        .size(10.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(DateMateSuccessGreen)
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Role & Category Row — Tapping "All" opens ALL Admin-created Filter Categories & Options Sheet!
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it }) { category ->
                val isAllButton = category == GalleryViewModel.CATEGORY_ALL
                val hasActiveSmartFilter = selectedSmartFilter != GalleryViewModel.FILTER_ALL
                val selected = if (isAllButton) {
                    category == selectedCategory || hasActiveSmartFilter
                } else {
                    category == selectedCategory
                }
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (isAllButton) {
                            onOpenAllFiltersSheet()
                        } else {
                            onSelectCategory(category)
                        }
                    },
                    leadingIcon = if (isAllButton) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    label = {
                        Text(
                            text = if (isAllButton && hasActiveSmartFilter) "All · $selectedSmartFilter" else if (isAllButton) "All Filters" else category,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = MaterialTheme.colorScheme.primary,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("category_chip_$category")
                )
            }
        }

        // Quick Smart Filter Chips Row (All opens complete Admin Filter Sheet too)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(smartFilters, key = { "smart_$it" }) { filter ->
                val isAllSmart = filter.equals(GalleryViewModel.FILTER_ALL, ignoreCase = true)
                val isOnlineFilter = filter.equals(GalleryViewModel.FILTER_ONLINE, ignoreCase = true)
                val selected = filter == selectedSmartFilter
                val leadingIcon = when {
                    filter.equals(GalleryViewModel.FILTER_NEARBY, ignoreCase = true) -> Icons.Rounded.LocationOn
                    filter.equals(GalleryViewModel.FILTER_BUDGET, ignoreCase = true) ||
                        filter.contains("₹") || filter.contains("Under", ignoreCase = true) -> Icons.Rounded.CurrencyRupee
                    filter.equals(GalleryViewModel.FILTER_PREMIUM, ignoreCase = true) -> Icons.Rounded.AutoAwesome
                    filter.equals(GalleryViewModel.FILTER_VERIFIED, ignoreCase = true) -> Icons.Rounded.Verified
                    isAllSmart -> Icons.Rounded.FilterList
                    else -> Icons.Rounded.AutoAwesome
                }
                Surface(
                    onClick = {
                        if (isAllSmart) {
                            onOpenAllFiltersSheet()
                        } else {
                            onSelectSmartFilter(filter)
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = when {
                        selected && isOnlineFilter -> Color(0xFF16A34A)
                        selected -> MaterialTheme.colorScheme.primary
                        isOnlineFilter -> Color(0xFF16A34A).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    shadowElevation = if (selected) 8.dp else 3.dp,
                    border = BorderStroke(
                        width = if (selected || isOnlineFilter) 1.3.dp else 1.dp,
                        color = when {
                            isOnlineFilter -> Color(0xFF22C55E)
                            selected -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    ),
                    modifier = Modifier.testTag("smart_filter_chip_$filter")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        if (isOnlineFilter) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .alpha(pulseAlpha)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White else Color(0xFF22C55E))
                            )
                        } else {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isAllSmart) "All" else filter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected || isOnlineFilter) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = when {
                                    selected -> Color.White
                                    isOnlineFilter -> Color(0xFF16A34A)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AllFilterCategoriesBottomSheet(
    filterTaxonomy: List<FilterCategoryGroup>,
    smartFilters: List<String>,
    selectedCategory: String,
    selectedSmartFilter: String,
    onSelectCategory: (String) -> Unit,
    onSelectFilterOption: (String) -> Unit,
    onResetAll: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("home_all_filters_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "All Filter Categories",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Browse by category & option to find your ideal match",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            // Admin-Created Filter Categories & Their Sub-Options
            filterTaxonomy.forEach { group ->
                if (group.options.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = group.category,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                group.options.forEach { option ->
                                    val isRoleOpt = option.equals("GF", true) || option.equals("BF", true)
                                    val isSelected = if (isRoleOpt) {
                                        selectedCategory.equals(option, true)
                                    } else {
                                        selectedSmartFilter.equals(option, true)
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isRoleOpt) {
                                                onSelectCategory(option.uppercase())
                                                onDismiss()
                                            } else {
                                                onSelectFilterOption(option)
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = option,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("taxonomy_opt_${group.category}_$option")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Smart Filters
            val extraSmart = smartFilters.filter { sf ->
                !sf.equals(GalleryViewModel.FILTER_ALL, true) &&
                    filterTaxonomy.none { grp -> grp.options.any { it.equals(sf, true) } }
            }
            if (extraSmart.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Quick Discovery Filters",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            extraSmart.forEach { sf ->
                                val isSelected = selectedSmartFilter.equals(sf, true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectFilterOption(sf) },
                                    label = { Text(sf) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DateMate3DGlowingButton(
                    text = "Show All Profiles",
                    onClick = onResetAll,
                    style = GlowButtonStyle.ROSE_PINK,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sheet_reset_all_filters_btn")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
