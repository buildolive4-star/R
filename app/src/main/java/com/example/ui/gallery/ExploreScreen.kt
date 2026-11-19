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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.GalleryImage
import com.example.data.model.UiState
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.Animated3DPageBackdrop
import com.example.ui.components.CupidLikeSuccessOverlay
import com.example.ui.components.DateMate3DGlowingIconButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GalleryGridCard
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.ShimmerBox
import com.example.ui.components.VipShowcaseCarousel
import com.example.ui.datemate.Floating3DHeartsBackground
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateSuccessGreen
import com.example.ui.theme.DateMateWhite

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
    userCity: String = "",
    userAddress: String = "",
    userGenderPreference: String = "",
    modifier: Modifier = Modifier,
) {
    val allProfiles = (galleryState as? UiState.Success)?.data.orEmpty()
    val totalCount = allProfiles.size
    val context = LocalContext.current
    var cupidCelebrationName by remember { mutableStateOf<String?>(null) }

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
                        DateMateWhite,
                        DateMateBackground,
                        DateMateSoftBlush.copy(alpha = 0.65f),
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
                            totalCount = totalCount,
                            isStaffTheme = isStaffTheme,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            userCity = userCity,
                            userGenderPreference = userGenderPreference,
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
                                text = "LoveMate is loading nearby 3D matches…",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = DateMatePrimaryPink
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
                            totalCount = totalCount,
                            isStaffTheme = isStaffTheme,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            userCity = userCity,
                            userGenderPreference = userGenderPreference,
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
                // VIP 16:9 Spotlight shows whenever Admin Panel has uploaded matching VIP 16:9 profiles
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
                    // Scrollable DateMate Header + Search Bar + Smart Filters (scrolls up seamlessly for maximum space!)
                    item(span = { GridItemSpan(maxLineSpan) }, key = "scrollable_home_header") {
                        DateMateScrollableHomeHeader(
                            totalCount = totalCount,
                            isStaffTheme = isStaffTheme,
                            onSecretAdminTrigger = onSecretAdminTrigger,
                            onPushBellClick = handlePushBellClick,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onSelectCategory = onSelectCategory,
                            smartFilters = smartFilters,
                            selectedSmartFilter = selectedSmartFilter,
                            onSelectSmartFilter = onSelectSmartFilter,
                            userCity = userCity,
                            userGenderPreference = userGenderPreference,
                        )
                    }

                    // Admin-Uploaded 16:9 VIP Spotlight (No static 16:9 Hero Banner above it!)
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
                                            color = DateMateDarkText
                                        )
                                        Text(
                                            text = "RoyalMate · 16:9 Exclusive Spotlights",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DateMateSecondaryText
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = DateMateSoftBlush,
                                    border = BorderStroke(1.dp, DateMatePrimaryPink.copy(alpha = 0.4f)),
                                ) {
                                    Text(
                                        text = "👑 ${vipProfiles.size} VIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DateMatePrimaryPink,
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
                                            color = DateMateDarkText
                                        )
                                        Text(
                                            text = if (userCity.isNotBlank()) {
                                                "Cupid picks near $userCity · 3D Glow"
                                            } else {
                                                "Set city & GF/BF preference in Profile for nearby matches"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DateMateSecondaryText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = DateMateSoftBlush,
                                    border = BorderStroke(1.dp, DateMatePrimaryPink.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "3D · 2×2",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DateMatePrimaryPink,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DateMateScrollableHomeHeader(
    totalCount: Int,
    isStaffTheme: Boolean,
    onSecretAdminTrigger: () -> Unit,
    onPushBellClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    smartFilters: List<String>,
    selectedSmartFilter: String,
    onSelectSmartFilter: (String) -> Unit,
    userCity: String,
    userGenderPreference: String,
) {
    val pulseTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3D Glowing Pulsing DateMate Heart Emblem + Brand Title (5-tap opens Admin Studio!)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
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
                // LoveMate 3D Animated Brand Mascot in Home Header
                Animated3DMascotOrb(
                    mascot = DateMateMascot.LOVE_MATE,
                    size = 48.dp,
                    showOrbitSparkles = true,
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = DateMateDarkText
                            )
                        )
                        if (isStaffTheme) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = DateMateSoftBlush,
                                border = BorderStroke(1.dp, DateMatePrimaryPink.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = DateMatePrimaryPink,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "STAFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DateMatePrimaryPink
                                    )
                                }
                            }
                        }
                    }
                    val subtitle = when {
                        userCity.isNotBlank() && userGenderPreference.isNotBlank() -> "$userCity · $userGenderPreference"
                        userCity.isNotBlank() -> "Near $userCity · 3D Verified"
                        else -> stringResource(R.string.brand_tagline)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DateMate3DGlowingIconButton(
                        icon = Icons.Rounded.NotificationsActive,
                        contentDescription = "Instant Push Notifications",
                        onClick = onPushBellClick,
                        style = GlowButtonStyle.ROSE_PINK,
                        size = 42.dp,
                        iconSize = 20.dp,
                        modifier = Modifier.testTag("home_push_bell_button")
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(8.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(DateMateSuccessGreen)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }

                Surface(
                    onClick = onPushBellClick,
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateWhite,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, DateMatePrimaryPink.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(DateMateSuccessGreen)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (totalCount > 0) {
                                "${stringResource(R.string.live_sync_badge)} · $totalCount"
                            } else {
                                stringResource(R.string.live_sync_badge)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DateMateDarkText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Glowing 3D Search Bar (scrolls smoothly upward with the header!)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = DateMatePrimaryPink
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.clear_search),
                            tint = DateMateSecondaryText
                        )
                    }
                }
            },
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DateMateWhite,
                unfocusedContainerColor = DateMateWhite,
                focusedBorderColor = DateMatePrimaryPink,
                unfocusedBorderColor = DateMatePrimaryPink.copy(alpha = 0.32f),
                focusedTextColor = DateMateDarkText,
                unfocusedTextColor = DateMateDarkText,
                cursorColor = DateMatePrimaryPink,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = DateMatePrimaryPink.copy(alpha = 0.20f),
                    spotColor = DateMatePrimaryPink.copy(alpha = 0.25f)
                )
                .testTag("search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Role & Category Chips (All / GF / BF / ...)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it }) { category ->
                val selected = category == selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DateMatePrimaryPink,
                        selectedLabelColor = DateMateWhite,
                        containerColor = DateMateWhite,
                        labelColor = DateMateDarkText,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = DateMateSoftBorder,
                        selectedBorderColor = DateMatePrimaryPink
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("category_chip_$category")
                )
            }
        }

        // Smart Filters Row: Nearby You, Budget (≤₹399), Premium, Verified + Admin Custom Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(smartFilters, key = { "smart_$it" }) { filter ->
                val selected = filter == selectedSmartFilter
                val leadingIcon = when {
                    filter.equals(GalleryViewModel.FILTER_NEARBY, ignoreCase = true) -> Icons.Rounded.LocationOn
                    filter.equals(GalleryViewModel.FILTER_BUDGET, ignoreCase = true) ||
                        filter.contains("₹") || filter.contains("Under", ignoreCase = true) -> Icons.Rounded.CurrencyRupee
                    filter.equals(GalleryViewModel.FILTER_PREMIUM, ignoreCase = true) -> Icons.Rounded.AutoAwesome
                    filter.equals(GalleryViewModel.FILTER_VERIFIED, ignoreCase = true) -> Icons.Rounded.Verified
                    filter.equals(GalleryViewModel.FILTER_ALL, ignoreCase = true) -> Icons.Rounded.Tune
                    else -> Icons.Rounded.AutoAwesome
                }
                Surface(
                    onClick = { onSelectSmartFilter(filter) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) DateMatePrimaryPink else DateMateWhite,
                    shadowElevation = if (selected) 8.dp else 3.dp,
                    border = BorderStroke(
                        width = if (selected) 1.4.dp else 1.dp,
                        color = if (selected) Color(0xFFFF80AB) else DateMateSoftBorder
                    ),
                    modifier = Modifier.testTag("smart_filter_chip_$filter")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = if (selected) DateMateWhite else DateMatePrimaryPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) DateMateWhite else DateMateDarkText
                            )
                        )
                    }
                }
            }
        }
    }
}
