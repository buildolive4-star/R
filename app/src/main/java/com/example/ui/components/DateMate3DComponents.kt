package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * DateMate's 6 cohesive Pixar-inspired 3D brand mascots mapped cleanly to app screens.
 */
enum class DateMateMascot(
    @DrawableRes val resId: Int,
    val title: String,
    val primaryGlow: Color,
    val secondaryGlow: Color,
) {
    LOVE_MATE(
        resId = R.drawable.img_mascot_lovemate_1795072439985,
        title = "LoveMate",
        primaryGlow = Color(0xFFFF2E7E),
        secondaryGlow = Color(0xFFFFB2CE),
    ),
    WELCOME_GIRL(
        resId = R.drawable.img_mascot_welcome_girl_1795072439185,
        title = "Welcome Assistant",
        primaryGlow = Color(0xFFE91E63),
        secondaryGlow = Color(0xFFFFC1D9),
    ),
    CUPID(
        resId = R.drawable.img_mascot_cupid_1795072441406,
        title = "Cupid",
        primaryGlow = Color(0xFFFF4081),
        secondaryGlow = Color(0xFFFFD54F),
    ),
    CHAT_MATE(
        resId = R.drawable.img_mascot_chatmate_1795072438521,
        title = "ChatMate",
        primaryGlow = Color(0xFFFF2E7E),
        secondaryGlow = Color(0xFFFF80AB),
    ),
    ROYAL_MATE(
        resId = R.drawable.img_mascot_royalmate_1795072440703,
        title = "RoyalMate",
        primaryGlow = Color(0xFFFFB300),
        secondaryGlow = Color(0xFFFF4081),
    ),
    SAFETY_MATE(
        resId = R.drawable.img_mascot_safetymate_1795072439294,
        title = "SafetyMate",
        primaryGlow = Color(0xFF00C853),
        secondaryGlow = Color(0xFFFF80AB),
    ),
}

enum class GlowButtonStyle(
    val topStart: Color,
    val topEnd: Color,
    val depthLip: Color,
    val haloGlow: Color,
    val contentColor: Color,
    val borderTint: Color,
) {
    ROSE_PINK(
        topStart = Color(0xFFFF2E7E),
        topEnd = Color(0xFFFF72A8),
        depthLip = Color(0xFFA60B4B),
        haloGlow = Color(0xFFFF2E7E),
        contentColor = Color.White,
        borderTint = Color(0xCCFFFFFF),
    ),
    EMERALD_CHAT(
        topStart = Color(0xFF1ED760),
        topEnd = Color(0xFF00C853),
        depthLip = Color(0xFF0B7A3B),
        haloGlow = Color(0xFF1ED760),
        contentColor = Color.White,
        borderTint = Color(0xCCFFFFFF),
    ),
    ROYAL_GOLD(
        topStart = Color(0xFFFFB300),
        topEnd = Color(0xFFFF5E8E),
        depthLip = Color(0xFFAD5200),
        haloGlow = Color(0xFFFFB300),
        contentColor = Color.White,
        borderTint = Color(0xDDFFF3C4),
    ),
    GLASS_SOFT(
        topStart = Color(0xFFFFF2F7),
        topEnd = Color(0xFFFFDDE9),
        depthLip = Color(0xFFE6A2BE),
        haloGlow = Color(0xFFFF6F9C),
        contentColor = Color(0xFFC2185B),
        borderTint = Color(0xFFFF9EC3),
    ),
    DANGER_RED(
        topStart = Color(0xFFFF3B5C),
        topEnd = Color(0xFFFF6E7F),
        depthLip = Color(0xFFA10F2A),
        haloGlow = Color(0xFFFF3B5C),
        contentColor = Color.White,
        borderTint = Color(0xCCFFFFFF),
    ),
}

/**
 * Interactive 3D Animated Mascot Medallion:
 * - Continuous 3D floating levitation, gentle perspective tilt, breathing scale,
 *   orbiting glowing sparkles, pulsing neon halo, and playful bounce on tap.
 */
@Composable
fun Animated3DMascotOrb(
    mascot: DateMateMascot,
    size: Dp = 84.dp,
    modifier: Modifier = Modifier,
    showOrbitSparkles: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    var tapCount by remember { mutableIntStateOf(0) }
    var isBouncing by remember { mutableStateOf(false) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            isBouncing = true
            delay(220L)
            isBouncing = false
        }
    }

    val tapScale by animateFloatAsState(
        targetValue = if (isBouncing) 1.16f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mascot_tap_scale"
    )

    val infinite = rememberInfiniteTransition(label = "mascot_3d_${mascot.name}")
    val bobDp by infinite.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_bob"
    )
    val tiltY by infinite.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_tilt_y"
    )
    val tiltZ by infinite.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_tilt_z"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_glow"
    )
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_orbit"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                tapCount++
                onClick?.invoke()
            }
    ) {
        // Pulsing neon halo + orbiting 3D glowing sparkles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val haloRadius = this.size.minDimension * 0.52f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        mascot.primaryGlow.copy(alpha = 0.42f * glowPulse),
                        mascot.secondaryGlow.copy(alpha = 0.22f * glowPulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = center
            )

            if (showOrbitSparkles) {
                val orbitR = this.size.minDimension * 0.44f
                for (i in 0 until 4) {
                    val rad = (orbitAngle + i * 90f) * (PI / 180.0)
                    val sx = center.x + (cos(rad) * orbitR).toFloat()
                    val sy = center.y + (sin(rad) * orbitR * 0.72f).toFloat()
                    val sparkleColor = if (i % 2 == 0) mascot.primaryGlow else mascot.secondaryGlow
                    drawCircle(
                        color = sparkleColor.copy(alpha = 0.75f),
                        radius = (if (i % 2 == 0) 3.2f else 2.2f) * density,
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // 3D Floating Glossy Mascot Medallion
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    cameraDistance = 12f * density
                    translationY = bobDp * density
                    rotationY = tiltY
                    rotationZ = tiltZ
                    scaleX = tapScale
                    scaleY = tapScale
                }
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = mascot.primaryGlow.copy(alpha = 0.5f),
                    spotColor = mascot.primaryGlow.copy(alpha = 0.65f),
                )
                .clip(CircleShape)
                .background(Color(0xFFFFF2F7))
                .border(
                    width = 2.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White,
                            mascot.primaryGlow,
                            mascot.secondaryGlow,
                            Color.White,
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = mascot.resId),
                contentDescription = mascot.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle glossy specular rim on top curve
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(size * 0.36f)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Tactile 3D Extruded Glowing Button with:
 * - Pulsing neon outer halo glow
 * - Physical 3D bottom lip that compresses on press with spring animation
 * - Vibrant gradient face + glossy top 3D rim highlight
 * - Animated light shimmer sweep across the face
 */
@Composable
fun DateMate3DGlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlowButtonStyle = GlowButtonStyle.ROSE_PINK,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    compact: Boolean = false,
    cornerRadius: Dp = if (compact) 999.dp else 18.dp,
    contentPadding: PaddingValues = if (compact) {
        PaddingValues(horizontal = 13.dp, vertical = 7.dp)
    } else {
        PaddingValues(horizontal = 18.dp, vertical = 12.dp)
    },
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val depthOffset by animateDpAsState(
        targetValue = if (isPressed || !enabled) 1.dp else if (compact) 3.dp else 4.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btn_3d_depth"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_3d_scale"
    )

    val infinite = rememberInfiniteTransition(label = "btn_3d_glow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_halo_alpha"
    )
    val shimmerProgress by infinite.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "btn_shimmer"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val activeAlpha = if (enabled) 1f else 0.55f

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = activeAlpha
            }
            .drawBehind {
                if (enabled) {
                    val spread = if (compact) 6.dp.toPx() else 10.dp.toPx()
                    val r = cornerRadius.toPx().coerceAtMost(size.height / 2f) + spread
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                style.haloGlow.copy(alpha = glowAlpha * 0.55f),
                                style.haloGlow.copy(alpha = glowAlpha * 0.18f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height / 2f + 2.dp.toPx()),
                            radius = (size.width.coerceAtLeast(size.height) * 0.72f) + spread
                        ),
                        topLeft = Offset(-spread * 0.55f, -spread * 0.35f),
                        size = Size(size.width + spread * 1.1f, size.height + spread * 0.95f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
            .padding(bottom = if (compact) 3.dp else 4.5.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 3D Extruded Bottom Depth Lip
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = depthOffset)
                .clip(shape)
                .background(style.depthLip)
        )

        // Elevated Glossy Top Face
        Box(
            modifier = Modifier
                .then(if (compact) Modifier else Modifier.fillMaxWidth())
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(style.topStart, style.topEnd)
                    )
                )
                .drawBehind {
                    // Animated glossy shimmer sweep
                    if (enabled && !isLoading) {
                        val sweepWidth = size.width * 0.38f
                        val startX = shimmerProgress * size.width - sweepWidth
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.26f),
                                    Color.Transparent
                                ),
                                start = Offset(startX, 0f),
                                end = Offset(startX + sweepWidth, size.height)
                            )
                        )
                    }
                    // Top 3D Glass Specular Rim
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.34f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
                .border(
                    width = 1.15.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            style.borderTint,
                            style.borderTint.copy(alpha = 0.22f)
                        )
                    ),
                    shape = shape
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = style.contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(if (compact) 14.dp else 18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = style.contentColor,
                        modifier = Modifier.size(if (compact) 15.dp else 19.dp)
                    )
                    Spacer(modifier = Modifier.width(if (compact) 5.dp else 8.dp))
                }
                Text(
                    text = text,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = style.contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Circular 3D Extruded Glowing Icon Button (for Push Bell, Heart/Like, Send, Call, Back).
 */
@Composable
fun DateMate3DGlowingIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlowButtonStyle = GlowButtonStyle.ROSE_PINK,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val depthOffset by animateDpAsState(
        targetValue = if (isPressed || !enabled) 1.dp else 3.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "icon_btn_depth"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_btn_scale"
    )
    val infinite = rememberInfiniteTransition(label = "icon_btn_glow")
    val glowPulse by infinite.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_btn_halo"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                if (enabled) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                style.haloGlow.copy(alpha = glowPulse * 0.55f),
                                Color.Transparent
                            ),
                            radius = this.size.minDimension * 0.72f
                        )
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = depthOffset)
                .clip(CircleShape)
                .background(style.depthLip)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(style.topStart, style.topEnd))
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(style.borderTint, style.borderTint.copy(alpha = 0.25f))
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = style.contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * Ambient 3D Animated Aurora & Sparkle Backdrop for every page.
 */
@Composable
fun Animated3DPageBackdrop(
    modifier: Modifier = Modifier,
    primaryOrbColor: Color = Color(0xFFFF4081),
    secondaryOrbColor: Color = Color(0xFFFFB74D),
) {
    val infinite = rememberInfiniteTransition(label = "page_3d_backdrop")
    val drift1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_drift_1"
    )
    val drift2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_drift_2"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_twinkle"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-right drifting rose aura orb
        val orb1 = Offset(
            x = w * (0.82f + 0.08f * cos(drift1)),
            y = h * (0.14f + 0.05f * sin(drift1))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryOrbColor.copy(alpha = 0.16f), Color.Transparent),
                center = orb1,
                radius = w * 0.56f
            ),
            radius = w * 0.56f,
            center = orb1
        )

        // Mid-left drifting warm gold-peach aura orb
        val orb2 = Offset(
            x = w * (0.14f + 0.09f * sin(drift2)),
            y = h * (0.48f + 0.06f * cos(drift2))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryOrbColor.copy(alpha = 0.14f), Color.Transparent),
                center = orb2,
                radius = w * 0.52f
            ),
            radius = w * 0.52f,
            center = orb2
        )

        // Bottom-right drifting soft pink orb
        val orb3 = Offset(
            x = w * (0.76f + 0.07f * sin(drift1 + 1.2f)),
            y = h * (0.84f + 0.04f * cos(drift2))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryOrbColor.copy(alpha = 0.12f), Color.Transparent),
                center = orb3,
                radius = w * 0.48f
            ),
            radius = w * 0.48f,
            center = orb3
        )

        // Subtle floating glowing sparkle particles
        val seeds = listOf(
            0.16f to 0.18f,
            0.86f to 0.28f,
            0.25f to 0.62f,
            0.78f to 0.56f,
            0.48f to 0.80f,
            0.12f to 0.88f,
        )
        seeds.forEachIndexed { idx, (fx, fy) ->
            val phase = drift1 + idx * 1.1f
            val px = w * fx + cos(phase) * 18f
            val py = h * fy + sin(phase) * 24f
            val c = if (idx % 2 == 0) primaryOrbColor else secondaryOrbColor
            drawCircle(
                color = c.copy(alpha = twinkle * 0.35f),
                radius = if (idx % 2 == 0) 4.5f else 3.2f,
                center = Offset(px, py)
            )
        }
    }
}

/**
 * Cupid Like / Match Success Animated 3D Overlay:
 * Pops in briefly with the animated 3D Cupid mascot when a user Likes a profile.
 */
@Composable
fun CupidLikeSuccessOverlay(
    likedProfileName: String?,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(likedProfileName) {
        if (!likedProfileName.isNullOrBlank()) {
            delay(1650L)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = !likedProfileName.isNullOrBlank(),
        enter = fadeIn(tween(200)) + scaleIn(
            initialScale = 0.72f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(tween(260)) + scaleOut(targetScale = 0.85f),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xF5FFFFFF),
                shadowElevation = 24.dp,
                border = BorderStroke(
                    2.dp,
                    Brush.linearGradient(
                        listOf(Color(0xFFFF2E7E), Color(0xFFFFD54F), Color(0xFFFF80AB))
                    )
                ),
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFFFF2E7E).copy(alpha = 0.45f),
                        spotColor = Color(0xFFFF2E7E).copy(alpha = 0.55f),
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Animated3DMascotOrb(
                        mascot = DateMateMascot.CUPID,
                        size = 86.dp,
                        showOrbitSparkles = true,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cupid Spark Sent! 💘",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD81B60),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${likedProfileName.orEmpty()} saved to your Favorites",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF5D4958),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
