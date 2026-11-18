package com.example.ui.datemate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DateMateNotification
import com.example.data.model.MembershipTier
import com.example.data.model.NotificationCategory
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateRoseGold
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateWhite

// ============================================================================
// 11. PREMIUM MEMBERSHIP SCREEN
// Plans: Free | Silver | Gold (Highlighted Recommended Plan) | Platinum
// Features: Monthly & Yearly plans, Benefits list, Animated 3D crown icon,
// Subscribe button (NO wallet section)
// ============================================================================
@Composable
fun DateMateMembershipScreen(
    currentTier: MembershipTier,
    currentIsYearly: Boolean,
    onSelectPlan: (MembershipTier, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var isYearlyBilling by remember(currentIsYearly) { mutableStateOf(currentIsYearly) }
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        contentPadding = PaddingValues(
            top = (statusTopPadding + 10.dp).coerceAtLeast(16.dp),
            bottom = 48.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("membership_screen_root")
    ) {
        // Top Header with Back Button
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = DateMateWhite,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(46.dp)
                        .border(1.dp, DateMateSoftBorder, CircleShape)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("membership_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = DateMateDarkText
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                ) {
                    Text(
                        text = "CURRENT PLAN: ${currentTier.title.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Animated 3D Crown Hero Header
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Animated3DCrownBadge(sizeDp = 76.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DateMate Premium Membership",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Elevate every connection with priority companion dates, unlimited chats & VIP visibility.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = DateMateSecondaryText
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Monthly / Yearly Billing Toggle
        item {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateSoftBlush,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    listOf(
                        false to "Monthly Plans",
                        true to "Yearly Plans (Save 33%)",
                    ).forEach { (yearlyOption, label) ->
                        val selected = isYearlyBilling == yearlyOption
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (selected) DateMateWhite else Color.Transparent)
                                .clickable { isYearlyBilling = yearlyOption }
                                .padding(vertical = 11.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (selected) DateMatePrimaryPink else DateMateSecondaryText,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4 Membership Plan Cards: Free, Silver, Gold (Highlighted Recommended), Platinum
        items(MembershipTier.entries, key = { it.name }) { tier ->
            val isCurrent = tier == currentTier && isYearlyBilling == currentIsYearly
            MembershipPlanCard(
                tier = tier,
                isYearly = isYearlyBilling,
                isCurrentPlan = isCurrent,
                onSubscribe = { onSelectPlan(tier, isYearlyBilling) }
            )
        }
    }
}

@Composable
private fun MembershipPlanCard(
    tier: MembershipTier,
    isYearly: Boolean,
    isCurrentPlan: Boolean,
    onSubscribe: () -> Unit,
) {
    val isRecommended = tier.isRecommended
    val priceAmount = if (isYearly) tier.yearlyPriceInr else tier.monthlyPriceInr
    val priceLabel = if (priceAmount == 0) {
        "Free Forever"
    } else if (isYearly) {
        "₹$priceAmount / year"
    } else {
        "₹$priceAmount / month"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 16.dp else 8.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isRecommended) 2.5.dp else 1.dp,
                color = if (isRecommended) DateMatePrimaryPink else DateMateSoftBorder,
                shape = RoundedCornerShape(28.dp)
            )
            .testTag("membership_card_${tier.name.lowercase()}")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isRecommended) {
                        Brush.verticalGradient(
                            colors = listOf(
                                DateMateSoftBlush.copy(alpha = 0.65f),
                                DateMateWhite
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(DateMateWhite, DateMateWhite))
                    }
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DateMate ${tier.title}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        if (isRecommended) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = DateMatePrimaryPink
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DateMateWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = tier.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = DateMateSecondaryText
                        )
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.WorkspacePremium,
                    contentDescription = tier.title,
                    tint = if (isRecommended) DateMatePrimaryPink else DateMateRoseGold,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = priceLabel,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = DateMatePrimaryPink,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tier.benefits.forEach { benefit ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = DateMateDarkText
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (isCurrentPlan) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 14.dp)
                    ) {
                        Text(
                            text = "Active Plan ✓",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            } else {
                DateMateGradientButton(
                    text = "Subscribe to ${tier.title}",
                    onClick = onSubscribe,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "subscribe_btn_${tier.name.lowercase()}"
                )
            }
        }
    }
}

// ============================================================================
// 12. NOTIFICATIONS SCREEN
// Categories: New Likes, New Matches, New Messages, Booking Requests,
// Booking Confirmations, Profile Verification, Membership Updates
// ============================================================================
@Composable
fun DateMateNotificationsScreen(
    notifications: List<DateMateNotification>,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (DateMateNotification) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var selectedCategory by remember { mutableStateOf<NotificationCategory?>(null) }
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val filteredList = remember(notifications, selectedCategory) {
        if (selectedCategory == null) {
            notifications
        } else {
            notifications.filter { it.category == selectedCategory }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            top = (statusTopPadding + 10.dp).coerceAtLeast(16.dp),
            bottom = 48.dp,
            start = 20.dp,
            end = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("notifications_screen_root")
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = DateMateWhite,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .border(1.dp, DateMateSoftBorder, CircleShape)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("notifications_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = DateMateDarkText
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = "Likes, matches, bookings & updates",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = DateMateSecondaryText
                            )
                        )
                    }
                }

                TextButton(
                    onClick = onMarkAllRead,
                    modifier = Modifier.testTag("notifications_mark_all_read_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = DateMatePrimaryPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mark Read",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DateMatePrimaryPink,
                        selectedLabelColor = DateMateWhite,
                        containerColor = DateMateWhite,
                        labelColor = DateMateDarkText
                    ),
                    shape = RoundedCornerShape(999.dp)
                )
                NotificationCategory.entries.forEach { category ->
                    val selected = selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DateMatePrimaryPink,
                            selectedLabelColor = DateMateWhite,
                            containerColor = DateMateWhite,
                            labelColor = DateMateDarkText
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }
        }

        items(filteredList, key = { it.id }) { notif ->
            NotificationItemCard(
                notification = notif,
                onClick = { onNotificationClick(notif) }
            )
        }
    }
}

@Composable
private fun NotificationItemCard(
    notification: DateMateNotification,
    onClick: () -> Unit,
) {
    val categoryIcon = when (notification.category) {
        NotificationCategory.NEW_LIKE, NotificationCategory.NEW_MATCH -> Icons.Rounded.Favorite
        NotificationCategory.NEW_MESSAGE -> Icons.AutoMirrored.Rounded.Chat
        NotificationCategory.BOOKING_REQUEST, NotificationCategory.BOOKING_CONFIRMATION -> Icons.Rounded.CalendarMonth
        NotificationCategory.PROFILE_VERIFICATION -> Icons.Rounded.Verified
        NotificationCategory.MEMBERSHIP_UPDATE -> Icons.Rounded.WorkspacePremium
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) DateMateWhite else DateMateSoftBlush
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (notification.isRead) DateMateSoftBorder else DateMatePrimaryPink.copy(alpha = 0.35f),
                RoundedCornerShape(22.dp)
            )
            .bouncy3DPress(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (notification.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = notification.avatarUrl,
                    contentDescription = notification.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, DateMatePrimaryPink, CircleShape)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = DateMateWhite,
                    modifier = Modifier
                        .size(52.dp)
                        .border(1.dp, DateMateSoftBorder, CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = notification.category.label,
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = notification.category.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = notification.timestampLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMateSecondaryText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )
            }
        }
    }
}
