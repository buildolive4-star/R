package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.LaunchBannerConfig
import kotlinx.coroutines.delay

@Composable
fun LaunchBannerOverlay(
    banner: LaunchBannerConfig,
    onSkip: () -> Unit,
) {
    if (!banner.isReadyToDisplay) return

    var secondsRemaining by remember(banner.imageUrl, banner.updatedAt) { mutableIntStateOf(3) }
    val entranceProgress = remember(banner.imageUrl, banner.updatedAt) { Animatable(0f) }

    LaunchedEffect(banner.imageUrl, banner.updatedAt) {
        entranceProgress.snapTo(0f)
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        )
    }

    LaunchedEffect(banner.imageUrl, banner.updatedAt) {
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
        onDismissRequest = { if (skipEnabled) onSkip() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = skipEnabled,
            dismissOnClickOutside = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C060F),
                            Color(0xFF170B1C),
                            Color(0xFF0A050D),
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .testTag("launch_banner_overlay")
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Spotlight Header Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = banner.title.ifBlank { "JUSTUS SPOTLIGHT" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = if (skipEnabled) MaterialTheme.colorScheme.primary else Color(0xFFF6C478),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            AnimatedContent(
                                targetState = secondsRemaining,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "countdown_text"
                            ) { sec ->
                                Text(
                                    text = if (sec > 0) "${sec}s" else "Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // 3D Animated Showcase Image Card
                val t = entranceProgress.value
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0F1F)),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                            )
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 18.dp)
                        .graphicsLayer {
                            cameraDistance = 16f * density
                            rotationX = (1f - t) * 18f
                            val scale = 0.88f + (0.12f * t)
                            scaleX = scale
                            scaleY = scale
                            alpha = t.coerceIn(0f, 1f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 14f)
                    ) {
                        PixoraNetworkImage(
                            imageUrl = banner.imageUrl,
                            contentDescription = banner.title.ifBlank { "JustUs Announcement" },
                            contentScale = ContentScale.Crop,
                            showSpinnerOnLoad = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressScale)
                                    .height(6.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary,
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }

                // Bottom Skip Button (Unlocks after 3 seconds)
                Button(
                    onClick = onSkip,
                    enabled = skipEnabled,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.14f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f),
                    ),
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .height(54.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("launch_banner_skip_button")
                ) {
                    if (skipEnabled) {
                        Text(
                            text = "Skip to Home",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Opening Home in ${secondsRemaining}s…",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
