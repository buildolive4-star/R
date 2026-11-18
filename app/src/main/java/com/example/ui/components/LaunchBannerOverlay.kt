package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.background
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LaunchBannerConfig
import kotlinx.coroutines.delay

@Composable
fun LaunchBannerOverlay(
    banner: LaunchBannerConfig?,
    onSkip: () -> Unit,
) {
    val effectiveImageUrl = banner?.imageUrl?.takeIf { banner.isReadyToDisplay }.orEmpty()
    val effectiveTitle = banner?.title?.takeIf { it.isNotBlank() } ?: "DATEMATE LUXURY EDITION"

    var secondsRemaining by remember(effectiveImageUrl, banner?.updatedAt) { mutableIntStateOf(3) }
    val entranceProgress = remember(effectiveImageUrl, banner?.updatedAt) { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_3d_glass")
    val heartPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_heart_pulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_3d_float"
    )
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "welcome_3d_tilt"
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
                            Color(0xFFFFFAFC),
                            Color(0xFFFFF0F5),
                            Color(0xFFFFE4EE),
                            Color(0xFFFFFAFC),
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("launch_banner_overlay")
        ) {
            // 3D Floating Ambient Glass Orbs & Glossy Particles Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE91E63).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.16f),
                        radius = size.minDimension * 0.55f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6F9C).copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.45f),
                        radius = size.minDimension * 0.5f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFD6A5A5).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.55f, size.height * 0.82f),
                        radius = size.minDimension * 0.48f
                    )
                )
            }

            // Floating 3D Glossy Decorative Hearts around the screen
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Color(0xFFFF6F9C).copy(alpha = 0.45f)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 12.dp, y = (74 + floatOffset * 0.7f).dp)
                    .size(44.dp)
                    .graphicsLayer {
                        rotationZ = -14f + tiltAngle
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.4f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-14).dp, y = (110 - floatOffset * 0.8f).dp)
                    .size(52.dp)
                    .graphicsLayer {
                        rotationZ = 16f - tiltAngle
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF6F9C),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Glassmorphic Spotlight Header Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.35f)),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = effectiveTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF25212A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Surface(
                        onClick = onSkip,
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.28f)),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
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
                                    color = Color(0xFFE91E63)
                                )
                            }
                        }
                    }
                }

                // Center 3D Glassmorphic Welcome Showcase Card
                val t = entranceProgress.value
                Card(
                    shape = RoundedCornerShape(34.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.82f)
                    ),
                    border = BorderStroke(
                        2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color.White,
                                Color(0xFFFF6F9C).copy(alpha = 0.65f),
                                Color(0xFFE91E63).copy(alpha = 0.75f),
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 14.dp)
                        .graphicsLayer {
                            cameraDistance = 18f * density
                            rotationX = (1f - t) * 16f + (tiltAngle * 0.35f)
                            rotationY = tiltAngle * 0.6f
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
                                                Color(0xFF25212A).copy(alpha = 0.82f)
                                            )
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "DateMate ❤️",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "A Better Way to Connect · Verified Companionship",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Default 3D Glassmorphic Welcome Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.95f),
                                            Color(0xFFFFF0F5).copy(alpha = 0.88f),
                                            Color.White.copy(alpha = 0.95f),
                                        )
                                    )
                                )
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 3D Glowing Heart Emblem with Floating Ring
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(126.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(122.dp)
                                        .graphicsLayer {
                                            scaleX = heartPulse
                                            scaleY = heartPulse
                                        }
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFFE91E63).copy(alpha = 0.28f),
                                                    Color(0xFFFF6F9C).copy(alpha = 0.10f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(
                                        2.5.dp,
                                        Brush.linearGradient(
                                            listOf(Color(0xFFE91E63), Color(0xFFFF6F9C))
                                        )
                                    ),
                                    shadowElevation = 16.dp,
                                    modifier = Modifier
                                        .size(92.dp)
                                        .graphicsLayer {
                                            scaleX = 0.96f + (heartPulse - 0.94f) * 0.5f
                                            scaleY = 0.96f + (heartPulse - 0.94f) * 0.5f
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(0xFFE91E63),
                                                        Color(0xFFFF6F9C)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.28f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3D GLASS LUXURY EXPERIENCE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE91E63),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Welcome to DateMate",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF25212A),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "A Better Way to Connect ❤️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Discover verified companions, plan real coffee & dining dates, enjoy instant private chat & HD voice calls in a frosted 3D glass sanctuary.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF77717D),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // 3 Frosted Glass Highlight Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassHighlightBadge(
                                    title = "100% Verified",
                                    subtitle = "ID & Voice",
                                    modifier = Modifier.weight(1f)
                                )
                                GlassHighlightBadge(
                                    title = "Instant Chat",
                                    subtitle = "& Voice Call",
                                    modifier = Modifier.weight(1f)
                                )
                                GlassHighlightBadge(
                                    title = "Date Planner",
                                    subtitle = "10+ Vibes",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Animated Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0xFFE91E63).copy(alpha = 0.14f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressScale)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFE91E63),
                                                    Color(0xFFFF6F9C),
                                                )
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                // Bottom Action Button (Always clickable so user can enter immediately or after 3s)
                Button(
                    onClick = onSkip,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE91E63),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("launch_banner_skip_button")
                ) {
                    Text(
                        text = if (skipEnabled) "Enter DateMate ❤️" else "Enter DateMate (${secondsRemaining}s)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.24f),
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
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.22f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFE91E63),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF77717D),
                maxLines = 1
            )
        }
    }
}
