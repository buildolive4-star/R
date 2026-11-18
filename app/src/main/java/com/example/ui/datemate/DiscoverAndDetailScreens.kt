package com.example.ui.datemate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.CompanionReview
import com.example.data.model.DateMateCatalog
import com.example.data.model.DiscoverFilters
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateSuccessGreen
import com.example.ui.theme.DateMateWhite
import kotlin.math.roundToInt

// ============================================================================
// 7. DISCOVER SCREEN
// Interactive Swipeable 3D Cards + Pass / Super Like / Like + Filters Sheet
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DateMateDiscoverScreen(
    deck: List<DateMateCompanionProfile>,
    filters: DiscoverFilters,
    onUpdateFilters: (DiscoverFilters) -> Unit,
    onResetFilters: () -> Unit,
    onPassProfile: (String) -> Unit,
    onLikeProfile: (String, Boolean) -> Unit,
    onOpenProfileDetail: (DateMateCompanionProfile) -> Unit,
) {
    var showFiltersSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .padding(bottom = 96.dp)
            .testTag("discover_screen_root")
    ) {
        Floating3DHeartsBackground(heartCount = 6)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Top Bar: Title + Active Filter Summary + Filter Sheet Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Discover Matches",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "${filters.selectedCity} • Up to ${filters.maxDistanceKm} km • Ages ${filters.ageRange.first}-${filters.ageRange.last}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = DateMateWhite,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, DateMateSoftBorder, CircleShape)
                    ) {
                        IconButton(
                            onClick = onResetFilters,
                            modifier = Modifier.testTag("discover_reset_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset Deck",
                                tint = DateMatePrimaryPink
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = DateMatePrimaryPink,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        IconButton(
                            onClick = { showFiltersSheet = true },
                            modifier = Modifier.testTag("discover_filters_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Filters",
                                tint = DateMateWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Horizontal Interest Filter Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                DateMateCatalog.allInterests.take(7).forEach { interest ->
                    val selected = filters.selectedInterest.equals(interest, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateFilters(filters.copy(selectedInterest = interest))
                        },
                        label = {
                            Text(
                                text = interest,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
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
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3D Swipeable Card Stack
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (deck.isEmpty()) {
                    EmptyDiscoverDeckCard(onReset = onResetFilters)
                } else {
                    val topProfile = deck.first()
                    val secondProfile = deck.getOrNull(1)
                    val thirdProfile = deck.getOrNull(2)

                    if (thirdProfile != null) {
                        DiscoverProfile3DCard(
                            profile = thirdProfile,
                            stackDepth = 2,
                            onPass = {},
                            onLike = {},
                            onOpenDetail = {},
                            isInteractive = false
                        )
                    }
                    if (secondProfile != null) {
                        DiscoverProfile3DCard(
                            profile = secondProfile,
                            stackDepth = 1,
                            onPass = {},
                            onLike = {},
                            onOpenDetail = {},
                            isInteractive = false
                        )
                    }
                    DiscoverProfile3DCard(
                        profile = topProfile,
                        stackDepth = 0,
                        onPass = { onPassProfile(topProfile.id) },
                        onLike = { onLikeProfile(topProfile.id, false) },
                        onOpenDetail = { onOpenProfileDetail(topProfile) },
                        isInteractive = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Buttons: Pass, Super Like, Like
            if (deck.isNotEmpty()) {
                val activeProfile = deck.first()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pass Button
                    Surface(
                        shape = CircleShape,
                        color = DateMateWhite,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(60.dp)
                            .border(1.dp, DateMateSoftBorder, CircleShape)
                            .bouncy3DPress(onClick = { onPassProfile(activeProfile.id) })
                            .testTag("discover_pass_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Pass",
                                tint = DateMateSecondaryText,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Super Like Button
                    Surface(
                        shape = CircleShape,
                        color = DateMateSoftBlush,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.5.dp, DateMateSecondaryPink, CircleShape)
                            .bouncy3DPress(onClick = { onLikeProfile(activeProfile.id, true) })
                            .testTag("discover_superlike_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Super Like",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Like Button (3D Heart)
                    Surface(
                        shape = CircleShape,
                        color = DateMatePrimaryPink,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .size(66.dp)
                            .bouncy3DPress(onClick = { onLikeProfile(activeProfile.id, false) })
                            .testTag("discover_like_btn")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.background(DateMatePinkGradient)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Like",
                                tint = DateMateWhite,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFiltersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFiltersSheet = false },
            containerColor = DateMateWhite,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            DiscoverFiltersBottomSheet(
                filters = filters,
                onUpdateFilters = onUpdateFilters,
                onReset = {
                    onResetFilters()
                    showFiltersSheet = false
                },
                onClose = { showFiltersSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverProfile3DCard(
    profile: DateMateCompanionProfile,
    stackDepth: Int,
    onPass: () -> Unit,
    onLike: () -> Unit,
    onOpenDetail: () -> Unit,
    isInteractive: Boolean,
) {
    var dragOffsetX by remember(profile.id) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember(profile.id) { mutableFloatStateOf(0f) }

    val animatedX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_drag_x"
    )
    val animatedY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_drag_y"
    )

    val stackScale = 1f - (stackDepth * 0.055f)
    val stackOffsetY = (stackDepth * 16).dp

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (stackDepth == 0) 18.dp else 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .offset(y = stackOffsetY)
            .offset {
                if (isInteractive) {
                    IntOffset(animatedX.roundToInt(), animatedY.roundToInt())
                } else {
                    IntOffset.Zero
                }
            }
            .graphicsLayer {
                scaleX = stackScale
                scaleY = stackScale
                if (isInteractive) {
                    rotationZ = (animatedX / 25f).coerceIn(-16f, 16f)
                }
            }
            .border(1.5.dp, DateMateSoftBorder, RoundedCornerShape(32.dp))
            .then(
                if (isInteractive) {
                    Modifier
                        .pointerInput(profile.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    when {
                                        dragOffsetX > 240f -> {
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                            onLike()
                                        }
                                        dragOffsetX < -240f -> {
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                            onPass()
                                        }
                                        else -> {
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        }
                                    }
                                },
                                onDragCancel = {
                                    dragOffsetX = 0f
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += (dragAmount.y * 0.35f)
                                }
                            )
                        }
                        .clickable { onOpenDetail() }
                } else Modifier
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = profile.primaryPhotoUrl,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badges
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                VerifiedOnlinePill(
                    isVerified = profile.isVerified,
                    isOnline = profile.isOnline
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateWhite.copy(alpha = 0.92f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${profile.distanceKm} km",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Drag Swipe Direction Stamp ("LIKE ❤️" or "PASS")
            if (isInteractive && kotlin.math.abs(animatedX) > 70f) {
                val isLikeDir = animatedX > 0
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isLikeDir) DateMatePrimaryPink else DateMateDarkText.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(if (isLikeDir) Alignment.TopStart else Alignment.TopEnd)
                        .padding(top = 70.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = if (isLikeDir) "LIKE ❤️" else "PASS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = DateMateWhite,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Bottom Glassmorphism Details Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF25212A).copy(alpha = 0.72f),
                                Color(0xFF25212A).copy(alpha = 0.92f),
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = DateMateWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = DateMatePrimaryPink,
                        ) {
                            Text(
                                text = "View Profile",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = "${profile.city} • ★ ${profile.rating} (${profile.reviewCount} verified reviews)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMateSecondaryPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Text(
                        text = profile.tagline,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DateMateWhite.copy(alpha = 0.92f)
                        )
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        profile.interests.take(4).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = DateMateWhite.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DateMateWhite,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDiscoverDeckCard(
    onReset: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(30.dp))
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            Animated3DHeartLogo(sizeDp = 86.dp, showDmMonogram = false)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You've Explored Every Match Nearby!",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Widen your distance or reset your Discover deck to revisit verified companions.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DateMateSecondaryText
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            DateMateGradientButton(
                text = "Refresh Discover Deck",
                onClick = onReset,
                testTag = "empty_deck_reset_btn"
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverFiltersBottomSheet(
    filters: DiscoverFilters,
    onUpdateFilters: (DiscoverFilters) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Discover Filters",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Bold
                )
            )
            TextButton(onClick = onReset) {
                Text("Reset All", color = DateMatePrimaryPink, fontWeight = FontWeight.Bold)
            }
        }

        // Distance Filter
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Maximum Distance",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "${filters.maxDistanceKm} km",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Slider(
                value = filters.maxDistanceKm.toFloat(),
                onValueChange = { onUpdateFilters(filters.copy(maxDistanceKm = it.roundToInt())) },
                valueRange = 2f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = DateMatePrimaryPink,
                    activeTrackColor = DateMatePrimaryPink,
                    inactiveTrackColor = DateMateSoftBlush
                )
            )
        }

        // Age Range Filter (18+ Strict Floor)
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Preferred Age Range (18+)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "${filters.ageRange.first} – ${filters.ageRange.last} yrs",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            RangeSlider(
                value = filters.ageRange.first.toFloat()..filters.ageRange.last.toFloat(),
                onValueChange = { range ->
                    val start = range.start.roundToInt().coerceAtLeast(18)
                    val end = range.endInclusive.roundToInt().coerceAtLeast(start + 1)
                    onUpdateFilters(filters.copy(ageRange = start..end))
                },
                valueRange = 18f..45f,
                colors = SliderDefaults.colors(
                    thumbColor = DateMatePrimaryPink,
                    activeTrackColor = DateMatePrimaryPink,
                    inactiveTrackColor = DateMateSoftBlush
                )
            )
        }

        // Location Filter
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.SemiBold
                )
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateMateCatalog.cities.forEach { city ->
                    val selected = filters.selectedCity == city
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateFilters(filters.copy(selectedCity = city)) },
                        label = { Text(city) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DateMatePrimaryPink,
                            selectedLabelColor = DateMateWhite,
                            containerColor = DateMateSoftBlush,
                            labelColor = DateMateDarkText
                        )
                    )
                }
            }
        }

        // Online Status & Verified Profile Toggles
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Online Companions Only",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Medium
                )
            )
            Switch(
                checked = filters.onlineOnly,
                onCheckedChange = { onUpdateFilters(filters.copy(onlineOnly = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DateMateWhite,
                    checkedTrackColor = DateMatePrimaryPink
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Verified Profiles Only",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Medium
                )
            )
            Switch(
                checked = filters.verifiedOnly,
                onCheckedChange = { onUpdateFilters(filters.copy(verifiedOnly = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DateMateWhite,
                    checkedTrackColor = DateMatePrimaryPink
                )
            )
        }

        DateMateGradientButton(
            text = "Apply Filters",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            testTag = "apply_discover_filters_btn"
        )
    }
}

// ============================================================================
// 8. PROFILE DETAILS SCREEN
// Photo Gallery + Verified + Online + About Me + Interests + Hobbies +
// Languages + Personality + Availability + Reviews & Ratings + Report & Block
// Primary Buttons: Chat Now | Book Now | Add to Favourite
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateMateProfileDetailScreen(
    profile: DateMateCompanionProfile,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChatNow: () -> Unit,
    onBookNow: () -> Unit,
    onReportProfile: (String) -> Unit,
    onBlockProfile: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var showReportDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    val photos = remember(profile) {
        profile.galleryPhotos.ifEmpty { listOf(profile.primaryPhotoUrl) }
    }
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateBackground)
            .testTag("profile_detail_screen")
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Photo Gallery Pager Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { index ->
                        AsyncImage(
                            model = photos[index],
                            contentDescription = "${profile.name} photo ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Top Navigation & Safety Actions
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = (statusTopPadding + 12.dp).coerceAtLeast(18.dp),
                                start = 18.dp,
                                end = 18.dp
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DateMateWhite.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(46.dp)
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.testTag("detail_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = DateMateDarkText
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = DateMateWhite.copy(alpha = 0.92f),
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(46.dp)
                            ) {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.testTag("detail_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Add to Favourite",
                                        tint = if (isFavorite) DateMatePrimaryPink else DateMateDarkText
                                    )
                                }
                            }
                        }
                    }

                    // Photo Dots & Bottom Gradient Title
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xFF25212A).copy(alpha = 0.86f))
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            photos.indices.forEach { idx ->
                                val active = pagerState.currentPage == idx
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (active) 24.dp else 6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(
                                            if (active) DateMatePrimaryPink else DateMateWhite.copy(alpha = 0.55f)
                                        )
                                )
                            }
                        }

                        VerifiedOnlinePill(
                            isVerified = profile.isVerified,
                            isOnline = profile.isOnline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = DateMateWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "${profile.city} • ${profile.distanceKm} km away • ★ ${profile.rating} (${profile.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DateMateWhite.copy(alpha = 0.9f)
                            )
                        )
                    }
                }
            }

            // About Me Card
            item {
                DetailSectionCard(
                    title = "About Me",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = profile.tagline,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile.aboutMe,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DateMateSecondaryText,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // Interests & Hobbies
            item {
                DetailSectionCard(
                    title = "Interests & Hobbies",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Interests",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMateSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.interests, isPink = true)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Hobbies",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMateSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.hobbies, isPink = false)
                }
            }

            // Personality & Languages
            item {
                DetailSectionCard(
                    title = "Personality & Languages",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Personality Traits",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMateSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.personalityTraits, isPink = true)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Spoken Languages",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMateSecondaryText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.languages, isPink = false)
                }
            }

            // Availability Schedule
            item {
                DetailSectionCard(
                    title = "Availability & Date Schedule",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = "Available Days",
                        style = MaterialTheme.typography.labelMedium.copy(color = DateMateSecondaryText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.availabilityDays, isPink = true)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Preferred Time Slots",
                        style = MaterialTheme.typography.labelMedium.copy(color = DateMateSecondaryText)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TagFlowChips(tags = profile.availableTimeSlots, isPink = false)
                }
            }

            // Verified Reviews & Ratings
            item {
                DetailSectionCard(
                    title = "Reviews & Ratings (★ ${profile.rating})",
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.reviews.forEach { review ->
                            ReviewItemRow(review = review)
                        }
                    }
                }
            }

            // Trust & Safety Controls: Report Profile & Block Profile
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DateMateWhite,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(18.dp))
                            .clickable { showReportDialog = true }
                            .testTag("report_profile_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = "Report Profile",
                                tint = DateMateSecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Report Profile",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMateSecondaryText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DateMateWhite,
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(18.dp))
                            .clickable { showBlockDialog = true }
                            .testTag("block_profile_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Block,
                                contentDescription = "Block Profile",
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Block Profile",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFFE11D48),
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sticky Primary Bottom Bar: Add to Favourite | Chat Now | Book Now
        Surface(
            color = DateMateWhite,
            shadowElevation = 20.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(1.dp, DateMateSoftBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 12.dp,
                        bottom = (navBottomPadding + 12.dp).coerceAtLeast(16.dp)
                    )
            ) {
                // Add to Favourite Button
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
                        .clickable { onToggleFavorite() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Add to Favourite",
                            tint = DateMatePrimaryPink
                        )
                    }
                }

                // Chat Now Button
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .border(1.dp, DateMatePrimaryPink.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .bouncy3DPress(onClick = onChatNow)
                        .testTag("detail_chat_now_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chat Now",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Book Now Button
                DateMateGradientButton(
                    text = "Book Now",
                    onClick = onBookNow,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = DateMateWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1.1f),
                    testTag = "detail_book_now_btn"
                )
            }
        }
    }

    if (showReportDialog) {
        val reasons = listOf(
            "Inaccurate photos or bio",
            "Unresponsive or spam behavior",
            "Inappropriate language",
            "Safety concern"
        )
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Report ${profile.name}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select a reason for our 24/7 Trust & Safety team:",
                        style = MaterialTheme.typography.bodySmall.copy(color = DateMateSecondaryText)
                    )
                    reasons.forEach { reason ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DateMateSoftBlush,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showReportDialog = false
                                    onReportProfile(reason)
                                }
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            containerColor = DateMateWhite,
            title = {
                Text(
                    text = "Block ${profile.name}?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "${profile.name} will no longer appear in your Discover deck, Home feed, or Chat conversations. You can unblock anytime from Profile Settings.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        onBlockProfile()
                    }
                ) {
                    Text("Block Profile", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel", color = DateMateSecondaryText)
                }
            }
        )
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = DateMateDarkText,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFlowChips(
    tags: List<String>,
    isPink: Boolean,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isPink) DateMateSoftBlush else DateMateBackground,
                modifier = Modifier.border(
                    1.dp,
                    if (isPink) DateMatePrimaryPink.copy(alpha = 0.28f) else DateMateSoftBorder,
                    RoundedCornerShape(999.dp)
                )
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (isPink) DateMatePrimaryPink else DateMateDarkText,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewItemRow(
    review: CompanionReview,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DateMateBackground,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = review.reviewerAvatar,
                        contentDescription = review.reviewerName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = review.reviewerName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = review.dateLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${review.rating}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = DateMateDarkText
                )
            )
        }
    }
}
