package com.example.ui.datemate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.CustomerUserProfile
import com.example.data.model.DateMateCatalog
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateRoseGold
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateSuccessGreen
import com.example.ui.theme.DateMateWhite

@Composable
fun DateMateHomeScreen(
    userProfile: CustomerUserProfile,
    selectedCity: String,
    searchQuery: String,
    companions: List<DateMateCompanionProfile>,
    favoriteIds: Set<String>,
    unreadNotificationCount: Int,
    onSelectCity: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMyProfile: () -> Unit,
    onBrowseProfilesClick: () -> Unit,
    onBookDateClick: (DateMateCompanionProfile?) -> Unit,
    onChatNowClick: (DateMateCompanionProfile?) -> Unit,
    onOpenMembership: () -> Unit,
    onOpenCompanionDetail: (DateMateCompanionProfile) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val filteredProfiles = remember(companions, selectedCity, searchQuery) {
        companions.filter { profile ->
            val matchesCity = selectedCity == "All Cities" ||
                profile.city.equals(selectedCity, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                profile.name.contains(q, ignoreCase = true) ||
                profile.city.contains(q, ignoreCase = true) ||
                profile.tagline.contains(q, ignoreCase = true) ||
                profile.interests.any { it.contains(q, ignoreCase = true) }
            matchesCity && matchesQuery
        }.ifEmpty { companions }
    }

    val popularProfiles = remember(companions) {
        companions.filter { it.isPopular }.ifEmpty { companions }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("home_screen_list")
    ) {
        // 1. Header: DateMate Logo, Location Selector, Notification Icon, User Avatar
        item {
            DateMateHomeHeader(
                selectedCity = selectedCity,
                userAvatarUrl = userProfile.avatarUrl,
                unreadNotificationCount = unreadNotificationCount,
                onSelectCity = onSelectCity,
                onOpenNotifications = onOpenNotifications,
                onOpenMyProfile = onOpenMyProfile,
            )
        }

        // Optional Search Bar for quick profile lookup
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = DateMatePrimaryPink
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = DateMateSecondaryText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = dateMateTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("home_search_input")
            )
        }

        // 2. Hero Banner Section
        item {
            DateMateHeroBanner(
                onExploreClick = onBrowseProfilesClick,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 3. Quick Actions (4 3D Action Cards)
        item {
            QuickActionsSection(
                onBrowseProfiles = onBrowseProfilesClick,
                onBookDate = { onBookDateClick(null) },
                onChatNow = { onChatNowClick(null) },
                onOpenMembership = onOpenMembership,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 4. Popular Profiles Horizontal Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column {
                        Text(
                            text = "Popular Profiles",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Verified 18+ companions online near you",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText
                            )
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier.clickable { onBrowseProfilesClick() }
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(popularProfiles, key = { it.id }) { profile ->
                        PopularCompanionCard(
                            profile = profile,
                            isFavorite = profile.id in favoriteIds,
                            onOpenProfile = { onOpenCompanionDetail(profile) },
                            onToggleFavorite = { onToggleFavorite(profile.id) },
                            onBookClick = { onBookDateClick(profile) },
                            onChatClick = { onChatNowClick(profile) },
                        )
                    }
                }
            }
        }

        // 5. Go Premium Banner Section
        item {
            GoPremiumBannerCard(
                onExploreMembership = onOpenMembership,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 6. Curated Feed for Selected City
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Column {
                    Text(
                        text = "Curated Companions in $selectedCity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Handpicked profiles ready for meaningful dates",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMateSecondaryText
                        )
                    )
                }
            }
        }

        items(filteredProfiles, key = { "feed_${it.id}" }) { profile ->
            FeaturedCompanionRowCard(
                profile = profile,
                isFavorite = profile.id in favoriteIds,
                onOpenProfile = { onOpenCompanionDetail(profile) },
                onToggleFavorite = { onToggleFavorite(profile.id) },
                onChatClick = { onChatNowClick(profile) },
                onBookClick = { onBookDateClick(profile) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun DateMateHomeHeader(
    selectedCity: String,
    userAvatarUrl: String,
    unreadNotificationCount: Int,
    onSelectCity: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMyProfile: () -> Unit,
) {
    var showCityMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
    ) {
        // Brand Logo + Tagline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DateMateWhite,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(16.dp))
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(
                        Brush.radialGradient(listOf(DateMateWhite, DateMateSoftBlush))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "DateMate Logo",
                        tint = DateMatePrimaryPink,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "DateMate",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                // Location Selector Pill
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .clickable { showCityMenu = true }
                            .testTag("home_location_selector")
                            .padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Location",
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = selectedCity,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = DateMateSecondaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Change city",
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showCityMenu,
                        onDismissRequest = { showCityMenu = false },
                        modifier = Modifier.background(DateMateWhite)
                    ) {
                        DateMateCatalog.cities.forEach { city ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = city,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (city == selectedCity) DateMatePrimaryPink else DateMateDarkText,
                                            fontWeight = if (city == selectedCity) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                onClick = {
                                    onSelectCity(city)
                                    showCityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Notification Bell + User Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = DateMateWhite,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .border(1.dp, DateMateSoftBorder, CircleShape)
                ) {
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = DateMateDarkText
                        )
                    }
                }
                if (unreadNotificationCount > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(DateMatePrimaryPink)
                    ) {
                        Text(
                            text = unreadNotificationCount.coerceAtMost(9).toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMateWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            AsyncImage(
                model = userAvatarUrl,
                contentDescription = "User Profile Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(2.dp, DateMatePrimaryPink, CircleShape)
                    .clickable { onOpenMyProfile() }
                    .testTag("home_avatar_button")
            )
        }
    }
}

@Composable
private fun DateMateHeroBanner(
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(30.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        modifier = modifier
            .fillMaxWidth()
            .height(215.dp)
            .border(1.5.dp, DateMateSoftBorder, RoundedCornerShape(30.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_datemate_hero_banner_1795008182368),
                contentDescription = stringResource(R.string.hero_headline),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Soft romantic gradient scrim ensuring crisp readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF25212A).copy(alpha = 0.78f),
                                Color(0xFF25212A).copy(alpha = 0.42f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateWhite.copy(alpha = 0.92f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.hero_subtitle),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.hero_headline),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = DateMateWhite,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        ),
                        modifier = Modifier.fillMaxWidth(0.76f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMatePrimaryPink,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .bouncy3DPress(onClick = onExploreClick)
                            .testTag("hero_explore_now_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hero_cta),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class QuickActionItem(
    val titleRes: Int,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun QuickActionsSection(
    onBrowseProfiles: () -> Unit,
    onBookDate: () -> Unit,
    onChatNow: () -> Unit,
    onOpenMembership: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = listOf(
        QuickActionItem(
            titleRes = R.string.quick_browse,
            subtitle = "3D Swipe Deck",
            icon = Icons.Rounded.Explore,
            accentColor = DateMatePrimaryPink,
            testTag = "quick_action_browse",
            onClick = onBrowseProfiles,
        ),
        QuickActionItem(
            titleRes = R.string.quick_book,
            subtitle = "6-Step Date Plan",
            icon = Icons.Rounded.CalendarMonth,
            accentColor = DateMateSecondaryPink,
            testTag = "quick_action_book",
            onClick = onBookDate,
        ),
        QuickActionItem(
            titleRes = R.string.quick_chat,
            subtitle = "Instant & Calls",
            icon = Icons.AutoMirrored.Rounded.Chat,
            accentColor = DateMatePrimaryPink,
            testTag = "quick_action_chat",
            onClick = onChatNow,
        ),
        QuickActionItem(
            titleRes = R.string.quick_premium,
            subtitle = "VIP Gold & Crown",
            icon = Icons.Rounded.WorkspacePremium,
            accentColor = Color(0xFFD97706),
            testTag = "quick_action_premium",
            onClick = onOpenMembership,
        ),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(
                color = DateMateDarkText,
                fontWeight = FontWeight.Bold
            )
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            actions.take(2).forEach { item ->
                QuickAction3DCard(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            actions.drop(2).forEach { item ->
                QuickAction3DCard(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickAction3DCard(
    item: QuickActionItem,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(22.dp))
            .bouncy3DPress(onClick = item.onClick)
            .testTag(item.testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DateMateSoftBlush,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.titleRes),
                        tint = item.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(item.titleRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = item.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PopularCompanionCard(
    profile: DateMateCompanionProfile,
    isFavorite: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBookClick: () -> Unit,
    onChatClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .width(245.dp)
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(26.dp))
            .bouncy3DPress(onClick = onOpenProfile)
            .testTag("popular_card_${profile.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(235.dp)
            ) {
                AsyncImage(
                    model = profile.primaryPhotoUrl,
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badges + Favourite Heart Button
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    VerifiedOnlinePill(
                        isVerified = profile.isVerified,
                        isOnline = profile.isOnline
                    )

                    Surface(
                        shape = CircleShape,
                        color = DateMateWhite.copy(alpha = 0.92f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("fav_btn_${profile.id}")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (isFavorite) DateMatePrimaryPink else DateMateDarkText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom gradient info overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF25212A).copy(alpha = 0.82f))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${profile.name}, ${profile.age}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${profile.city} • ${profile.distanceKm} km",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = DateMateWhite.copy(alpha = 0.88f)
                                )
                            )
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${profile.rating}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = profile.tagline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onChatClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMatePrimaryPink,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onBookClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Book Date",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoPremiumBannerCard(
    onExploreMembership: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, DateMateRoseGold.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            DateMateSoftBlush,
                            DateMateWhite,
                            Color(0xFFFFF7ED)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateWhite,
                ) {
                    Text(
                        text = "VIP LUXURY EXPERIENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.go_premium_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.go_premium_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                DateMateGradientButton(
                    text = stringResource(R.string.go_premium_cta),
                    onClick = onExploreMembership,
                    testTag = "home_explore_membership_btn"
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Animated3DCrownBadge(sizeDp = 64.dp)
        }
    }
}

@Composable
private fun FeaturedCompanionRowCard(
    profile: DateMateCompanionProfile,
    isFavorite: Boolean,
    onOpenProfile: () -> Unit,
    onToggleFavorite: () -> Unit,
    onChatClick: () -> Unit,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(24.dp))
            .bouncy3DPress(onClick = onOpenProfile)
            .testTag("featured_row_${profile.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box {
                AsyncImage(
                    model = profile.primaryPhotoUrl,
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                if (profile.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(DateMateSuccessGreen)
                            .border(2.dp, DateMateWhite, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.name}, ${profile.age}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (profile.isVerified) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = if (isFavorite) DateMatePrimaryPink else DateMateSecondaryText,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Text(
                    text = "${profile.city} • ${profile.distanceKm} km away • ★ ${profile.rating}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = profile.tagline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onChatClick() }
                    ) {
                        Text(
                            text = "Chat Now",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMatePrimaryPink,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onBookClick() }
                    ) {
                        Text(
                            text = "Book Now",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMateWhite,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
