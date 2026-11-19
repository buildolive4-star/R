package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.AppShareConfig
import com.example.data.model.BookingRatePlan
import com.example.data.model.GalleryImage
import com.example.ui.theme.DateMateSuccessGreen

/**
 * Feature 6: Shareable Luxury Date Invitation Card powered by Nano Banana VIP Rooftop Lounge artwork.
 */
@Composable
fun LuxuryDateInviteCardDialog(
    profile: GalleryImage,
    appShareConfig: AppShareConfig,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("luxury_date_invite_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(215.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.nano_vip_lounge_1795114685085),
                        contentDescription = "VIP Date Lounge Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(215.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(215.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x55120815), Color(0xCC120815))
                                )
                            )
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Invite Card", tint = Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC1D1122),
                        border = BorderStroke(1.dp, Color(0xFFFFD166).copy(alpha = 0.7f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "✨ VIP DATE INVITATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD166),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        PixoraNetworkImage(
                            imageUrl = profile.displayThumbnailUrl,
                            contentDescription = profile.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${profile.title}, ${profile.age}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (profile.city.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = profile.city,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            PriceBadgePill(price = profile.displayPrice)
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = profile.description.ifBlank {
                            "Verified DateMate Companion available for Dinner, Coffee & Luxury VIP Dates."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    DateMate3DGlowingButton(
                        text = "Share Date Card Link",
                        leadingIcon = Icons.Rounded.Share,
                        style = GlowButtonStyle.ROYAL_GOLD,
                        onClick = {
                            val shareText = buildString {
                                append("✨ DateMate VIP Companion Spotlight ✨\n")
                                append("💖 ${profile.title}, ${profile.age}")
                                if (profile.city.isNotBlank()) append(" · 📍 ${profile.city}")
                                append("\n💎 Starting at ${profile.displayPrice}\n\n")
                                append(appShareConfig.buildShareText())
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(shareIntent, "Share ${profile.title} on DateMate"))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("luxury_date_invite_share_button")
                    )
                }
            }
        }
    }
}

/**
 * Feature 8: Interactive Date Budget & Pre-Booking Advance Estimator Dialog.
 */
@Composable
fun DateBudgetEstimatorDialog(
    profile: GalleryImage,
    defaultAdvanceInr: Int,
    onProceedToBook: () -> Unit,
    onDismiss: () -> Unit,
) {
    val plans = remember(profile.id, profile.bookingPlans, profile.price) {
        BookingRatePlan.parseList(profile.bookingPlans, profile.displayPrice)
    }
    var selectedIdx by remember { mutableIntStateOf(0) }
    val selectedPlan = plans.getOrElse(selectedIdx) { plans.first() }
    val advanceInr = profile.resolvedAdvanceAmountInr(defaultAdvanceInr).coerceAtMost(selectedPlan.numericPrice.coerceAtLeast(1))
    val balanceOnMeet = (selectedPlan.numericPrice - advanceInr).coerceAtLeast(0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("budget_estimator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date Price Breakdown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "${profile.title} · Transparent Pre-Booking Split",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Price Breakdown")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    plans.forEachIndexed { index, plan ->
                        val isSelected = index == selectedIdx
                        Surface(
                            onClick = { selectedIdx = index },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("budget_plan_row_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = plan.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                )
                                Text(
                                    text = plan.priceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selected Package (${selectedPlan.label})", style = MaterialTheme.typography.bodySmall)
                            Text("₹${selectedPlan.numericPrice}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Advance Payable Now", style = MaterialTheme.typography.bodySmall, color = DateMateSuccessGreen, fontWeight = FontWeight.Bold)
                            Text("₹$advanceInr", style = MaterialTheme.typography.titleSmall, color = DateMateSuccessGreen, fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining Balance on Meet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹$balanceOnMeet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                DateMate3DGlowingButton(
                    text = "Book ${selectedPlan.label} (Pay ₹$advanceInr Advance)",
                    leadingIcon = Icons.Rounded.CalendarMonth,
                    style = GlowButtonStyle.ROSE_PINK,
                    onClick = {
                        onDismiss()
                        onProceedToBook()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_proceed_book_button")
                )
            }
        }
    }
}

data class VirtualGiftItem(
    val id: String,
    val emoji: String,
    val title: String,
    val coinCost: Int,
    val chatText: String,
)

val LUXURY_VIRTUAL_GIFTS = listOf(
    VirtualGiftItem("rose", "🌹", "Velvet Rose", 0, "🌹 Sent you a Fresh Velvet Rose!"),
    VirtualGiftItem("bouquet", "💐", "Royal Bouquet", 10, "💐 Sent you a Luxury Royal Bouquet!"),
    VirtualGiftItem("chocolates", "🍫", "Belgian Truffles", 15, "🍫 Sent you Swiss & Belgian Truffles!"),
    VirtualGiftItem("champagne", "🥂", "Sparkling Rosé", 25, "🥂 Sent a Sparkling Rosé Toast for our date!"),
    VirtualGiftItem("tiara", "👑", "VIP Tiara", 40, "👑 Sent you a Royal VIP Tiara!"),
    VirtualGiftItem("ring", "💎", "Diamond Spark", 50, "💎 Sent you a Luxury Diamond Spark Gift!"),
)

/**
 * Feature 5: Luxury 3D Virtual Gift Tray for ChatRoomScreen.
 */
@Composable
fun LuxuryChatGiftTrayDialog(
    dateCoinsBalance: Int,
    onSelectGift: (VirtualGiftItem) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("luxury_gift_tray_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎁 Luxury Gift Tray",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "Your Balance: 🪙 $dateCoinsBalance DateCoins",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Gift Tray")
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    items(LUXURY_VIRTUAL_GIFTS, key = { it.id }) { gift ->
                        val affordable = gift.coinCost == 0 || dateCoinsBalance >= gift.coinCost
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (affordable) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp,
                                if (affordable) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = affordable) {
                                    onSelectGift(gift)
                                    onDismiss()
                                }
                                .testTag("gift_item_${gift.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(gift.emoji, fontSize = 28.sp)
                                Text(
                                    text = gift.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (gift.coinCost == 0) "FREE" else "🪙 ${gift.coinCost}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (gift.coinCost == 0) DateMateSuccessGreen else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
