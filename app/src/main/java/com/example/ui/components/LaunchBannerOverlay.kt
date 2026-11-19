package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LaunchBannerConfig
import com.example.util.Local3DAssetResolver
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LaunchBannerOverlay(
    banner: LaunchBannerConfig?,
    onSkip: () -> Unit,
) {
    val effectiveImageUrl = banner?.imageUrl?.takeIf { banner.isReadyToDisplay }.orEmpty()
    val effectiveTitle = banner?.title?.takeIf { it.isNotBlank() } ?: "DATEMATE 3D GLOW EDITION"

    var secondsRemaining by remember(effectiveImageUrl, banner?.updatedAt) { mutableIntStateOf(3) }
    val entranceProgress = remember(effectiveImageUrl, banner?.updatedAt) { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_3d_glow")
    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_heart_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -11f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_3d_float"
    )
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_3d_tilt"
    )
    val glowSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "welcome_glow_orbit"
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_aura_alpha"
    )

    LaunchedEffect(effectiveImageUrl, banner?.updatedAt) {
        entranceProgress.snapTo(0f)
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        )
    }

    LaunchedEffect(effectiveImageUrl, banner?.updatedAt) {
        secondsRemaining = 3
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
    }

    val skipEnabled = secondsRemaining <= 0
    val density = LocalDensity.current.density
    val progressScale by animateFloatAsState(
        targetValue = (3 - secondsRemaining).coerceIn(0, 3) / 3f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "launch_progress"
    )

    Dialog(
        onDismissRequest = onSkip,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1A0B1E),
                            Color(0xFF2D0F35),
                            Color(0xFF3E123F),
                            Color(0xFF1B0A22),
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("launch_banner_overlay")
        ) {
            // Animated 3D Neon Aurora Orbs & Glowing Energy Rings Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val rad = glowSweep * (PI.toFloat() / 180f)
                val orbitX1 = size.width * (0.5f + 0.34f * cos(rad))
                val orbitY1 = size.height * (0.24f + 0.08f * sin(rad))
                val orbitX2 = size.width * (0.5f - 0.36f * cos(rad * 0.8f))
                val orbitY2 = size.height * (0.76f + 0.08f * cos(rad))

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF2A85).copy(alpha = 0.42f * auraAlpha),
                            Color(0xFFE91E63).copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(orbitX1, orbitY1),
                        radius = size.minDimension * 0.62f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB74D).copy(alpha = 0.28f * auraAlpha),
                            Color(0xFFFF6F9C).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(orbitX2, orbitY2),
                        radius = size.minDimension * 0.56f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.30f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.minDimension * 0.72f
                    )
                )
                // Glowing neon orbital ring
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF4081).copy(alpha = 0.45f),
                            Color(0xFFFFD54F).copy(alpha = 0.25f),
                            Color(0xFFCE93D8).copy(alpha = 0.35f),
                            Color(0xFFFF4081).copy(alpha = 0.45f),
                        )
                    ),
                    radius = size.minDimension * 0.44f,
                    center = Offset(size.width * 0.5f, size.height * 0.46f),
                    style = Stroke(width = 2.5f)
                )
            }

            // Floating 3D Glowing Glass Hearts
            Surface(
                shape = CircleShape,
                color = Color(0x33FF4081),
                border = BorderStroke(1.2.dp, Color(0xFFFF80AB).copy(alpha = 0.75f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = (70 + floatOffset * 0.8f).dp)
                    .size(46.dp)
                    .graphicsLayer {
                        rotationZ = -14f + tiltAngle
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF80AB),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color(0x33FFB74D),
                border = BorderStroke(1.2.dp, Color(0xFFFFD54F).copy(alpha = 0.75f)),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = (104 - floatOffset * 0.9f).dp)
                    .size(52.dp)
                    .graphicsLayer {
                        rotationZ = 16f - tiltAngle
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Glowing Glass Header Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0x40FFFFFF),
                        border = BorderStroke(1.dp, Color(0xFFFF80AB).copy(alpha = 0.65f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = effectiveTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Surface(
                        onClick = onSkip,
                        shape = RoundedCornerShape(50),
                        color = Color(0x38FFFFFF),
                        border = BorderStroke(1.dp, Color(0xFFFF80AB).copy(alpha = 0.55f)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFF80AB),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            AnimatedContent(
                                targetState = secondsRemaining,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "countdown_text"
                            ) { sec ->
                                Text(
                                    text = if (sec > 0) "${sec}s · Skip" else "Skip",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Center 3D Glowing Frosted Glass Showcase Card
                val t = entranceProgress.value
                Card(
                    shape = RoundedCornerShape(36.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xCC261030)
                    ),
                    border = BorderStroke(
                        2.2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFFF4081),
                                Color(0xFFFFD54F).copy(alpha = 0.85f),
                                Color(0xFFFF80AB),
                                Color(0xFFE91E63),
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 12.dp)
                        .shadow(
                            elevation = 28.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = Color(0xFFFF2A85).copy(alpha = 0.55f),
                            spotColor = Color(0xFFFF4081).copy(alpha = 0.65f)
                        )
                        .graphicsLayer {
                            cameraDistance = 18f * density
                            rotationX = (1f - t) * 18f + (tiltAngle * 0.45f)
                            rotationY = tiltAngle * 0.7f
                            translationY = floatOffset
                            val scale = 0.90f + (0.10f * t)
                            scaleX = scale
                            scaleY = scale
                            alpha = t.coerceIn(0f, 1f)
                        }
                ) {
                    if (effectiveImageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 13f)
                        ) {
                            PixoraNetworkImage(
                                imageUrl = effectiveImageUrl,
                                contentDescription = effectiveTitle,
                                contentScale = ContentScale.Crop,
                                showSpinnerOnLoad = true,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color(0xFF1D0A24).copy(alpha = 0.88f)
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "DateMate 3D ✨",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "A Better Way to Connect · Verified Companionship",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Default 3D Glowing Glass Welcome Experience with 3D Character Medallions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xE6331340),
                                            Color(0xE6220B2B),
                                            Color(0xE62F1038),
                                        )
                                    )
                                )
                                .padding(horizontal = 22.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 3D Glowing Heart Halo Emblem
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(108.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(106.dp)
                                        .graphicsLayer {
                                            scaleX = heartPulse
                                            scaleY = heartPulse
                                        }
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFFF2A85).copy(alpha = 0.55f),
                                                    Color(0xFFFFD54F).copy(alpha = 0.20f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFE91E63),
                                    border = BorderStroke(
                                        2.5.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFD54F), Color(0xFFFF80AB), Color.White)
                                        )
                                    ),
                                    shadowElevation = 18.dp,
                                    modifier = Modifier
                                        .size(78.dp)
                                        .graphicsLayer {
                                            scaleX = 0.96f + (heartPulse - 0.94f) * 0.45f
                                            scaleY = 0.96f + (heartPulse - 0.94f) * 0.45f
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFFFF1E78),
                                                        Color(0xFFFF6F9C),
                                                        Color(0xFFFF9800)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Glowing 3D Character Avatar Showcase Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy((-10).dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Local3DAssetResolver.ALL_3D_AVATAR_RES_IDS.forEachIndexed { idx, resId ->
                                    val isCenter = idx == 2
                                    val avatarSize = if (isCenter) 60.dp else 48.dp
                                    Box(
                                        modifier = Modifier
                                            .size(avatarSize)
                                            .shadow(
                                                elevation = if (isCenter) 14.dp else 8.dp,
                                                shape = CircleShape,
                                                ambientColor = Color(0xFFFF4081),
                                                spotColor = Color(0xFFFFD54F)
                                            )
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isCenter) 2.5.dp else 1.8.dp,
                                                brush = Brush.linearGradient(
                                                    listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color.White)
                                                ),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Image(
                                            painter = painterResource(id = resId),
                                            contentDescription = "DateMate 3D Companion",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x33FF4081),
                                border = BorderStroke(1.dp, Color(0xFFFF80AB).copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD54F),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3D GLOW LUXURY EXPERIENCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFD54F),
                                        letterSpacing = 0.9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Welcome to DateMate",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Meet Nearby Companions in 3D Glow ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF80AB),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Discover verified nearby GF & BF companions, smart budget filters, instant push alerts with sound & vibration, and HD voice calls.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 Glowing Glass Highlight Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassHighlightBadge(
                                    title = "Nearby First",
                                    subtitle = "Auto GPS",
                                    modifier = Modifier.weight(1f)
                                )
                                GlassHighlightBadge(
                                    title = "Instant Push",
                                    subtitle = "Sound + Vibe",
                                    modifier = Modifier.weight(1f)
                                )
                                GlassHighlightBadge(
                                    title = "3D Profiles",
                                    subtitle = "GF & BF",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Glowing Neon Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressScale)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFFF2A85),
                                                    Color(0xFFFF80AB),
                                                    Color(0xFFFFD54F),
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Bottom Glowing Animated CTA Button
                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF2A85),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 14.dp),
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(50),
                            ambientColor = Color(0xFFFF2A85),
                            spotColor = Color(0xFFFF80AB)
                        )
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color.White, Color(0xFFFF80AB))),
                            RoundedCornerShape(50)
                        )
                        .minimumInteractiveComponentSize()
                        .testTag("launch_banner_skip_button")
                ) {
                    Text(
                        text = if (skipEnabled) "Enter DateMate ✨" else "Enter DateMate (${secondsRemaining}s)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassHighlightBadge(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x26FFFFFF),
        border = BorderStroke(1.dp, Color(0xFFFF80AB).copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}
