package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AppUpdateConfig
import com.example.data.preferences.UserEngagementPrefs
import com.example.ui.theme.DateMateSuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 100% Airtight Hard-Blocking Force Update Wall.
 * Replaces the entire app UI (Home, Chats, Bookings, NavHost are never mounted) when Main Admin
 * sets forceUpdate = true with a newer versionCode + APK link.
 * - Swallows hardware/gesture Back press via BackHandler(enabled = true) {}
 * - Zero Close / Skip / Later buttons
 * - Survives offline restarts via DataStore anti-bypass cache
 * - Opens Admin APK URL via standard Android browser Intent (Play-policy compliant, zero DCL)
 * - Discreet footer long-press preserves Main Admin emergency access to Admin Studio.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForceUpdateHardBlockScreen(
    updateConfig: AppUpdateConfig,
    onSecretAdminTrigger: () -> Unit,
) {
    // Completely block back button so the user cannot exit or bypass the wall into the app
    BackHandler(enabled = true) { /* Hard locked until updated */ }

    val context = LocalContext.current
    var linkCopied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0816),
                        Color(0xFF280D24),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("force_update_hard_block_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.65f)),
                tonalElevation = 12.dp,
                shadowElevation = 18.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(206.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.nano_update_hero_1795114685140),
                            contentDescription = "Mandatory DateMate Update Required",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0x44000000), Color(0xCC180814))
                                    )
                                )
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDC2626),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "App Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "APP LOCKED · UPDATE REQUIRED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Update to DateMate v${updateConfig.latestVersionName}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "Purana version band kar diya gaya hai. App chalane ke liye naya version install karna zaroori hai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Old Version",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DateMateSuccessGreen.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, DateMateSuccessGreen.copy(alpha = 0.45f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Required Version",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DateMateSuccessGreen,
                                    )
                                    Text(
                                        text = "v${updateConfig.latestVersionName} (Build ${updateConfig.latestVersionCode})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DateMateSuccessGreen,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "What's New in v${updateConfig.latestVersionName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = updateConfig.releaseNotes.ifBlank {
                                    "• Important security & server upgrade by DateMate Admin\n• Faster live companion matching, chat & bookings\n• Install this new APK to unlock your account immediately"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        DateMate3DGlowingButton(
                            text = "Update Now (Download APK)",
                            leadingIcon = Icons.Rounded.SystemUpdate,
                            style = GlowButtonStyle.ROSE_PINK,
                            onClick = {
                                val url = updateConfig.apkDownloadUrl.trim()
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("force_update_download_now_button")
                        )

                        OutlinedButton(
                            onClick = {
                                val url = updateConfig.apkDownloadUrl.trim()
                                if (url.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("DateMate Update Link", url))
                                    linkCopied = true
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("force_update_copy_link_button")
                        ) {
                            Icon(
                                imageVector = if (linkCopied) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                                contentDescription = "Copy Download Link",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (linkCopied) "Link Copied! Paste in Chrome ✓" else "Copy Download Link",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shield,
                                    contentDescription = "Safe Update",
                                    tint = DateMateSuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Your account, chats, bookings & coins are 100% safe. Install the new APK over this app and open normally.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Discreet footer with Main Admin emergency long-press escape hatch
            Text(
                text = "DateMate v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) · Mandatory Update Lock Active",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.58f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onSecretAdminTrigger,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("force_update_admin_footer")
            )
        }
    }
}

/**
 * Option 1: In-App Live Update Checker & Optional Update Popup featuring Nano Banana hero artwork.
 * Opens the Admin-hosted APK URL in the system browser (strictly Play-policy compliant, zero DCL).
 */
@Composable
fun AppUpdateModalDialog(
    updateConfig: AppUpdateConfig,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isForced = updateConfig.forceUpdate

    Dialog(
        onDismissRequest = { if (!isForced) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isForced,
            dismissOnClickOutside = !isForced,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("app_update_modal_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(172.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.nano_update_hero_1795114685140),
                        contentDescription = "DateMate New Update Available",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(172.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color(0xBB160B18))
                                )
                            )
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isForced) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isForced) "MANDATORY UPDATE" else "NEW VERSION LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (!isForced) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("app_update_close_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close Update Popup", tint = Color.White)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "DateMate v${updateConfig.latestVersionName} Ready ✨",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Text(
                            text = "Your version: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) → New: Build ${updateConfig.latestVersionCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateConfig.releaseNotes.ifBlank {
                                "• Faster live companion matching & smoother private chats\n• New booking offers & enhanced safety features\n• Important stability & performance upgrades"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    DateMate3DGlowingButton(
                        text = "Update Now (Download APK)",
                        leadingIcon = Icons.Rounded.SystemUpdate,
                        style = GlowButtonStyle.ROSE_PINK,
                        onClick = {
                            val url = updateConfig.apkDownloadUrl.trim()
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_update_download_now_button")
                    )

                    if (!isForced) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("app_update_later_button")
                        ) {
                            Text("Remind Me Later", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(
                            text = "This update is required by DateMate Admin to continue using the app smoothly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private data class DailySpinPrize(
    val coins: Int,
    val title: String,
    val emoji: String,
)

private val DAILY_SPIN_PRIZES = listOf(
    DailySpinPrize(25, "Rose Gift Bonus", "🌹"),
    DailySpinPrize(50, "VIP Date Boost", "✨"),
    DailySpinPrize(75, "Diamond Charm", "💎"),
    DailySpinPrize(100, "Royal Jackpot", "👑"),
    DailySpinPrize(40, "Midnight Spark", "🌙"),
)

/**
 * Feature 3: Daily Check-In Streak & Lucky DateCoins Spin Modal featuring Nano Banana artwork.
 */
@Composable
fun DailyCheckInSpinDialog(
    engagementPrefs: UserEngagementPrefs,
    onClaimReward: (coins: Int, rewardTitle: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayEpochDay = remember { System.currentTimeMillis() / 86_400_000L }
    val alreadyClaimed = engagementPrefs.hasClaimedToday(todayEpochDay)
    var isSpinning by remember { mutableStateOf(false) }
    var spinDegrees by remember { mutableFloatStateOf(0f) }
    var wonPrize by remember { mutableStateOf<DailySpinPrize?>(null) }
    val scope = rememberCoroutineScope()

    val animatedRotation by animateFloatAsState(
        targetValue = spinDegrees,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "daily_spin_rotation"
    )

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
                .testTag("daily_spin_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Lucky Bonus",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Daily Bonus")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("DateCoins", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "🪙 ${engagementPrefs.dateCoins}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF59E0B).copy(alpha = 0.14f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Check-In Streak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "🔥 ${engagementPrefs.streakCount} Day(s)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(168.dp)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nano_daily_wheel_1795114685200),
                        contentDescription = "Daily Lucky Reward Box",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(168.dp)
                            .rotate(animatedRotation)
                    )
                }

                if (wonPrize != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DateMateSuccessGreen.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, DateMateSuccessGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${wonPrize!!.emoji} You Won +${wonPrize!!.coins} DateCoins (${wonPrize!!.title})!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DateMateSuccessGreen,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else if (alreadyClaimed) {
                    Text(
                        text = "Today's bonus claimed ✓ (${engagementPrefs.lastSpinRewardTitle.ifBlank { "DateCoins Added" }}). Come back tomorrow to grow your streak!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "Tap Spin once every day to unlock free DateCoins for luxury chat gifts!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                DateMate3DGlowingButton(
                    text = when {
                        isSpinning -> "Spinning Your Luck..."
                        alreadyClaimed -> "Claimed Today ✓"
                        else -> "Spin & Claim Free DateCoins"
                    },
                    enabled = !alreadyClaimed && !isSpinning,
                    style = GlowButtonStyle.ROYAL_GOLD,
                    leadingIcon = Icons.Rounded.AutoAwesome,
                    onClick = {
                        if (!alreadyClaimed && !isSpinning) {
                            isSpinning = true
                            spinDegrees += 720f
                            val prize = DAILY_SPIN_PRIZES.random()
                            scope.launch {
                                delay(1250L)
                                wonPrize = prize
                                isSpinning = false
                                onClaimReward(prize.coins, prize.title)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_spin_claim_button")
                )
            }
        }
    }
}

private data class VibeQuestion(
    val prompt: String,
    val options: List<Pair<String, String>>, // label to vibeKey
)

private val VIBE_QUIZ_QUESTIONS = listOf(
    VibeQuestion(
        prompt = "1. Pick your ideal first date mood:",
        options = listOf(
            "🕯️ Rooftop Candlelight Dinner" to "Candlelight Romantic",
            "🌙 Late-Night Drive & City Lights" to "Midnight Explorer",
            "☕ Cozy Artisan Coffee & Deep Talk" to "Coffee Soulmate",
            "👑 Luxury Lounge & VIP Experience" to "Royal VIP",
        )
    ),
    VibeQuestion(
        prompt = "2. What makes a companion unforgettable?",
        options = listOf(
            "💬 Effortless romantic charm" to "Candlelight Romantic",
            "⚡ Spontaneous fun & laughter" to "Midnight Explorer",
            "🤍 Warm listener & genuine vibe" to "Coffee Soulmate",
            "✨ Glamorous elegance & class" to "Royal VIP",
        )
    ),
    VibeQuestion(
        prompt = "3. Choose your weekend plan:",
        options = listOf(
            "🍽️ Fine Dining & Dessert" to "Candlelight Romantic",
            "🎶 Music, Club & Night Out" to "Midnight Explorer",
            "🎬 Movie & Cafe Hopping" to "Coffee Soulmate",
            "🥂 Private VIP Celebration" to "Royal VIP",
        )
    ),
)

/**
 * Feature 4: 3-Question Instant Love Vibe & Compatibility Quiz Dialog.
 */
@Composable
fun LoveVibeQuizDialog(
    currentVibe: String,
    onSaveVibeAndMatch: (vibeName: String, filterKeyword: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val votes = remember { mutableStateOf(mapOf<String, Int>()) }
    var computedResult by remember { mutableStateOf<String?>(null) }

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
                .padding(horizontal = 20.dp)
                .testTag("love_vibe_quiz_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Love Vibe Quiz",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Quiz")
                    }
                }

                if (computedResult == null) {
                    val q = VIBE_QUIZ_QUESTIONS[stepIndex]
                    Text(
                        text = "Question ${stepIndex + 1} of ${VIBE_QUIZ_QUESTIONS.size}" +
                            if (currentVibe.isNotBlank()) " · Current: $currentVibe" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = q.prompt,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    q.options.forEachIndexed { idx, (label, vibeKey) ->
                        Surface(
                            onClick = {
                                val nextVotes = votes.value.toMutableMap()
                                nextVotes[vibeKey] = (nextVotes[vibeKey] ?: 0) + 1
                                votes.value = nextVotes
                                if (stepIndex + 1 < VIBE_QUIZ_QUESTIONS.size) {
                                    stepIndex++
                                } else {
                                    computedResult = nextVotes.maxByOrNull { it.value }?.key ?: "Candlelight Romantic"
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("vibe_quiz_opt_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                } else {
                    val result = computedResult!!
                    val filterKeyword = when (result) {
                        "Candlelight Romantic" -> "Dinner"
                        "Midnight Explorer" -> "Night"
                        "Royal VIP" -> "Premium"
                        else -> "All"
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Animated3DMascotOrb(mascot = DateMateMascot.LOVE_MATE, size = 64.dp, showOrbitSparkles = true)
                            Text("Your Match Personality", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = result,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Saved to your Profile! We'll surface matching companions right away.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    DateMate3DGlowingButton(
                        text = "Show Matching Companions",
                        style = GlowButtonStyle.ROSE_PINK,
                        leadingIcon = Icons.Rounded.AutoAwesome,
                        onClick = {
                            onSaveVibeAndMatch(result, filterKeyword)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vibe_quiz_apply_button")
                    )
                }
            }
        }
    }
}

/**
 * Feature 7: Date Safety SOS & Trusted Emergency Check-In Toolkit.
 */
@Composable
fun DateSafetyToolkitDialog(
    savedContactName: String,
    savedContactPhone: String,
    companionContextName: String = "",
    companionContextCity: String = "",
    onSaveContact: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var nameInput by remember(savedContactName) { mutableStateOf(savedContactName) }
    var phoneInput by remember(savedContactPhone) { mutableStateOf(savedContactPhone) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, DateMateSuccessGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("date_safety_sos_dialog")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Animated3DMascotOrb(mascot = DateMateMascot.SAFETY_MATE, size = 40.dp, showOrbitSparkles = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Date Safety SOS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Trusted Friend Live Check-In", style = MaterialTheme.typography.labelSmall, color = DateMateSuccessGreen)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Safety Toolkit")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DateMateSuccessGreen.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("DateMate Safety Checklist", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = DateMateSuccessGreen)
                        Text("✓ Always meet first at public cafes, lounges, or restaurants.", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Keep bookings & chat inside DateMate for verified concierge support.", style = MaterialTheme.typography.bodySmall)
                        Text("✓ Send a 1-tap Check-In alert to your trusted friend before meeting.", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Trusted Contact Name") },
                    placeholder = { Text("e.g., Rahul / Best Friend") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Trusted Phone Number") },
                    placeholder = { Text("+91 98XXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSaveContact(nameInput, phoneInput) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("safety_save_contact_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Contact", maxLines = 1)
                    }
                    DateMate3DGlowingButton(
                        text = "Send Check-In",
                        leadingIcon = Icons.Rounded.Share,
                        style = GlowButtonStyle.ROSE_PINK,
                        onClick = {
                            if (nameInput.isNotBlank() || phoneInput.isNotBlank()) {
                                onSaveContact(nameInput, phoneInput)
                            }
                            val targetDetail = buildString {
                                append("🛡️ DateMate Live Safety Check-In\n")
                                if (nameInput.isNotBlank()) append("Hi $nameInput, ")
                                append("I'm heading for a DateMate meetup")
                                if (companionContextName.isNotBlank()) append(" with $companionContextName")
                                if (companionContextCity.isNotBlank()) append(" in $companionContextCity")
                                append(".\nI'll message you once I'm safely back! — Sent via DateMate Safety SOS")
                            }
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, targetDetail)
                            }
                            runCatching {
                                context.startActivity(Intent.createChooser(sendIntent, "Send Live Date Check-In"))
                            }
                        },
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("safety_send_checkin_button")
                    )
                }
            }
        }
    }
}
