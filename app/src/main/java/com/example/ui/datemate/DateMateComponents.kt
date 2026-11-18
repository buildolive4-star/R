package com.example.ui.datemate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.DateMateTab
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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

val DateMatePinkGradient = Brush.horizontalGradient(
    colors = listOf(DateMatePrimaryPink, DateMateSecondaryPink)
)

val DateMateSoftBlushVerticalGradient = Brush.verticalGradient(
    colors = listOf(DateMateWhite, DateMateBackground, DateMateSoftBlush.copy(alpha = 0.55f))
)

fun Modifier.bouncy3DPress(
    onClick: () -> Unit,
    enabled: Boolean = true,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale3d"
    )
    val rotationX by animateFloatAsState(
        targetValue = if (isPressed && enabled) 3.5f else 0f,
        animationSpec = tween(120),
        label = "rotX3d"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.rotationX = rotationX
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

@Composable
fun DateMateGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = DateMateSoftBlush,
            contentColor = DateMateWhite,
            disabledContentColor = DateMateSecondaryText,
        ),
        modifier = taggedModifier
            .heightIn(min = 52.dp)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(999.dp),
                ambientColor = DateMatePrimaryPink.copy(alpha = 0.35f),
                spotColor = DateMatePrimaryPink.copy(alpha = 0.45f),
            )
            .background(
                brush = if (enabled) DateMatePinkGradient else Brush.horizontalGradient(
                    listOf(DateMateSoftBlush, DateMateSoftBlush)
                ),
                shape = RoundedCornerShape(999.dp),
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) DateMateWhite else DateMateSecondaryText
                )
            )
        }
    }
}

@Composable
fun Floating3DHeartsBackground(
    modifier: Modifier = Modifier,
    heartCount: Int = 9,
) {
    val infinite = rememberInfiniteTransition(label = "floating_3d_hearts")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "float_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (i in 0 until heartCount) {
            val seed = (i + 1) * 0.137f
            val baseX = ((seed * 3.7f) % 1f) * w
            val phase = (progress + seed) % 1f
            val y = h * (1.05f - phase * 1.15f)
            val sway = sin((phase * 6.28f) + i) * 22f
            val heartSize = (18f + (i % 4) * 8f)
            val alpha = (sin(phase * Math.PI).toFloat() * 0.24f).coerceIn(0.03f, 0.24f)

            draw3DHeartShape(
                center = Offset(baseX + sway, y),
                radius = heartSize,
                primaryColor = DateMatePrimaryPink.copy(alpha = alpha),
                highlightColor = DateMateSecondaryPink.copy(alpha = alpha * 1.25f)
            )
        }
    }
}

fun DrawScope.draw3DHeartShape(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    highlightColor: Color,
) {
    val path = Path().apply {
        val x = center.x
        val y = center.y
        moveTo(x, y + radius * 0.65f)
        cubicTo(
            x - radius * 1.15f, y - radius * 0.05f,
            x - radius * 0.65f, y - radius * 1.05f,
            x, y - radius * 0.35f
        )
        cubicTo(
            x + radius * 0.65f, y - radius * 1.05f,
            x + radius * 1.15f, y - radius * 0.05f,
            x, y + radius * 0.65f
        )
        close()
    }
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(highlightColor, primaryColor),
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f),
            radius = radius * 1.35f,
        ),
        style = Fill
    )
}

@Composable
fun Animated3DHeartLogo(
    sizeDp: Dp = 116.dp,
    showDmMonogram: Boolean = true,
) {
    val infinite = rememberInfiniteTransition(label = "heart_3d_pulse")
    val pulseScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(sizeDp * 1.35f)
    ) {
        // Outer 3D soft pink aura
        Box(
            modifier = Modifier
                .size(sizeDp * 1.25f)
                .graphicsLayer {
                    scaleX = pulseScale * 1.05f
                    scaleY = pulseScale * 1.05f
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DateMateSecondaryPink.copy(alpha = glowAlpha),
                            DateMateSoftBlush.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Elevated 3D porcelain & rose-pink heart emblem
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = DateMateWhite,
            shadowElevation = 18.dp,
            modifier = Modifier
                .size(sizeDp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(DateMateWhite, DateMateSecondaryPink.copy(alpha = 0.45f))
                    ),
                    shape = RoundedCornerShape(34.dp)
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(DateMateWhite, DateMateSoftBlush)
                        )
                    )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "DateMate 3D Heart",
                    tint = DateMatePrimaryPink,
                    modifier = Modifier.size(sizeDp * 0.68f)
                )
                if (showDmMonogram) {
                    Text(
                        text = "DM",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = DateMateWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (sizeDp.value * 0.23f).sp,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun Animated3DCrownBadge(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 54.dp,
) {
    val infinite = rememberInfiniteTransition(label = "crown_3d")
    val floatY by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_float"
    )
    Surface(
        shape = CircleShape,
        color = DateMateWhite,
        shadowElevation = 10.dp,
        modifier = modifier
            .size(sizeDp)
            .offset(y = floatY.dp)
            .border(1.5.dp, DateMateRoseGold.copy(alpha = 0.6f), CircleShape)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF6E5),
                            DateMateSoftBlush,
                        )
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "3D Crown",
                tint = DateMatePrimaryPink,
                modifier = Modifier.size(sizeDp * 0.56f)
            )
        }
    }
}

@Composable
fun HeartExplosionOverlay(
    triggerTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(triggerTimestamp) {
        if (triggerTimestamp > 0L) {
            visible = true
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
            delay(100)
            visible = false
        }
    }

    if (visible) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val p = progress.value
            val particleCount = 14
            for (i in 0 until particleCount) {
                val angle = (i * (360f / particleCount)) * (Math.PI / 180f)
                val distance = p * 280f * (0.7f + (i % 3) * 0.22f)
                val x = center.x + cos(angle).toFloat() * distance
                val y = center.y + sin(angle).toFloat() * distance - (p * 45f)
                val radius = (1f - p * 0.65f) * (24f + (i % 3) * 8f)
                val alpha = (1f - p).coerceIn(0f, 1f)
                draw3DHeartShape(
                    center = Offset(x, y),
                    radius = radius,
                    primaryColor = DateMatePrimaryPink.copy(alpha = alpha),
                    highlightColor = DateMateSecondaryPink.copy(alpha = alpha),
                )
            }
        }
    }
}

@Composable
fun VerifiedOnlinePill(
    isVerified: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        if (isVerified) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateWhite.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Verified Profile",
                        tint = DateMatePrimaryPink,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        if (isOnline) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateWhite.copy(alpha = 0.92f),
                shadowElevation = 2.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DateMateSuccessGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Online",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// ============================================================================
// Floating 5-Tab Bottom Navigation Bar
// Sections: 1. Home, 2. Discover, 3. Bookings, 4. Chat, 5. Profile
// ============================================================================
@Composable
fun DateMateFloatingBottomBar(
    selectedTab: DateMateTab,
    onSelectTab: (DateMateTab) -> Unit,
    unreadChatCount: Int,
    pendingBookingCount: Int,
    modifier: Modifier = Modifier,
) {
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = (navBarBottomPadding + 10.dp).coerceAtLeast(14.dp)
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
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                DateMateTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = stringResource(tab.labelRes)
                    val icon = when (tab) {
                        DateMateTab.HOME -> if (isSelected) Icons.Rounded.Home else Icons.Outlined.Home
                        DateMateTab.DISCOVER -> if (isSelected) Icons.Rounded.Explore else Icons.Outlined.Explore
                        DateMateTab.BOOKINGS -> if (isSelected) Icons.Rounded.CalendarMonth else Icons.Outlined.CalendarMonth
                        DateMateTab.CHAT -> if (isSelected) Icons.AutoMirrored.Rounded.Chat else Icons.AutoMirrored.Outlined.Chat
                        DateMateTab.PROFILE -> if (isSelected) Icons.Rounded.Person else Icons.Outlined.Person
                    }
                    val badgeCount = when (tab) {
                        DateMateTab.CHAT -> unreadChatCount
                        DateMateTab.BOOKINGS -> pendingBookingCount
                        else -> 0
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (isSelected) DateMateSoftBlush else Color.Transparent
                            )
                            .clickable { onSelectTab(tab) }
                            .testTag(tab.testTag)
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) DateMatePrimaryPink else DateMateSecondaryText,
                                    modifier = Modifier.size(23.dp)
                                )
                                if (badgeCount > 0) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 7.dp, y = (-4).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(DateMatePrimaryPink)
                                    ) {
                                        Text(
                                            text = badgeCount.coerceAtMost(9).toString(),
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
                                    color = if (isSelected) DateMatePrimaryPink else DateMateSecondaryText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(width = if (isSelected) 16.dp else 0.dp, height = 3.dp)
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

@Composable
fun MatchCelebrationDialog(
    companion: DateMateCompanionProfile?,
    onDismiss: () -> Unit,
    onStartChat: (DateMateCompanionProfile) -> Unit,
    onBookDate: (DateMateCompanionProfile) -> Unit,
) {
    if (companion == null) return
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = DateMateWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, DateMateSoftBorder, RoundedCornerShape(30.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DateMateSoftBlushVerticalGradient)
                    .padding(24.dp)
            ) {
                Animated3DHeartLogo(sizeDp = 76.dp, showDmMonogram = false)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "It's a DateMate Match!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "You and ${companion.name} both connected. Start a conversation or plan a memorable date!",
                    style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = companion.primaryPhotoUrl,
                    contentDescription = companion.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(3.dp, DateMatePrimaryPink, CircleShape)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${companion.name}, ${companion.age}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = companion.city,
                    style = MaterialTheme.typography.bodySmall.copy(color = DateMateSecondaryText)
                )
                Spacer(modifier = Modifier.height(20.dp))
                DateMateGradientButton(
                    text = "Chat Now",
                    onClick = { onStartChat(companion) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "match_dialog_chat_btn"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = DateMateSoftBlush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onBookDate(companion) }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Book a Date",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep Exploring",
                    style = MaterialTheme.typography.labelMedium.copy(color = DateMateSecondaryText),
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun CompanionCallOverlay(
    call: ActiveCompanionCall?,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit,
) {
    AnimatedVisibility(
        visible = call != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
    ) {
        if (call != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DateMateWhite.copy(alpha = 0.98f),
                                DateMateSoftBlush.copy(alpha = 0.98f),
                                Color(0xFFFFE4EE).copy(alpha = 0.99f)
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Floating3DHeartsBackground(heartCount = 7)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateWhite,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Encrypted",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DateMate Private ${if (call.callType == ActiveCallType.VIDEO) "Video" else "Audio"} Call",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = DateMatePrimaryPink,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = call.companionPhotoUrl,
                            contentDescription = call.companionName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(148.dp)
                                .clip(CircleShape)
                                .border(4.dp, DateMatePrimaryPink, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = call.companionName,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = DateMateDarkText,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = call.statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = DateMatePrimaryPink,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Spacer(modifier = Modifier.height(48.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (call.isMuted) DateMatePrimaryPink else DateMateWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(58.dp)
                        ) {
                            IconButton(onClick = onToggleMute) {
                                Icon(
                                    imageVector = if (call.isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                    contentDescription = "Toggle Mute",
                                    tint = if (call.isMuted) DateMateWhite else DateMateDarkText
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE11D48),
                            shadowElevation = 12.dp,
                            modifier = Modifier.size(68.dp)
                        ) {
                            IconButton(
                                onClick = onEndCall,
                                modifier = Modifier.testTag("end_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = "End Call",
                                    tint = DateMateWhite,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (call.isSpeakerOn) DateMatePrimaryPink else DateMateWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(58.dp)
                        ) {
                            IconButton(onClick = onToggleSpeaker) {
                                Icon(
                                    imageVector = Icons.Rounded.VolumeUp,
                                    contentDescription = "Toggle Speaker",
                                    tint = if (call.isSpeakerOn) DateMateWhite else DateMateDarkText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
