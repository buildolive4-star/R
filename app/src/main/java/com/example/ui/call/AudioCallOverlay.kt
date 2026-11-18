package com.example.ui.call

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ActiveCallUi
import com.example.data.model.CallSession
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay

@Composable
fun AudioCallOverlay(
    callUi: ActiveCallUi,
    currentUid: String,
    onAccept: (CallSession) -> Unit,
    onDecline: (CallSession) -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
) {
    if (callUi is ActiveCallUi.Idle) return

    val session = when (callUi) {
        is ActiveCallUi.OutgoingRinging -> callUi.session
        is ActiveCallUi.IncomingRinging -> callUi.session
        is ActiveCallUi.Connecting -> callUi.session
        is ActiveCallUi.Connected -> callUi.session
        is ActiveCallUi.Idle -> return
    }

    val isCaller = session.callerUid == currentUid
    val peerDisplayName = if (isCaller) {
        session.calleeName.ifBlank { "Companion" }
    } else {
        session.callerName.ifBlank { "JustUs" }
    }
    val subtitleText = session.profileName.takeIf { it.isNotBlank() }?.let { "Profile: $it" }
        ?: "Private Voice Channel"

    var elapsedSeconds by remember(callUi) { mutableLongStateOf(0L) }
    if (callUi is ActiveCallUi.Connected) {
        LaunchedEffect(callUi.connectedAtEpochMs) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - callUi.connectedAtEpochMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    val statusText = when (callUi) {
        is ActiveCallUi.OutgoingRinging -> "Ringing… Waiting for answer"
        is ActiveCallUi.IncomingRinging -> "Incoming Voice Call…"
        is ActiveCallUi.Connecting -> "Connecting WebRTC audio…"
        is ActiveCallUi.Connected -> {
            val mins = elapsedSeconds / 60L
            val secs = elapsedSeconds % 60L
            String.format("%02d:%02d · Encrypted Audio", mins, secs)
        }
        is ActiveCallUi.Idle -> ""
    }

    val pulseTransition = rememberInfiniteTransition(label = "call_ring_pulse")
    val ringScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.32f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_scale"
    )
    val ringAlpha by pulseTransition.animateFloat(
        initialValue = 0.52f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring_alpha"
    )
    val outerRingScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.56f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "outer_ring_scale"
    )
    val outerRingAlpha by pulseTransition.animateFloat(
        initialValue = 0.36f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "outer_ring_alpha"
    )
    val emblemTiltY by pulseTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emblem_tilt_y"
    )
    val density = LocalDensity.current.density

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0912))
                .testTag("audio_call_overlay")
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_call_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.65f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0A0912),
                                Color(0x88141024),
                                Color(0xEE0A0912),
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFF6C478),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JUSTUS PRIVATE VOICE · WEBRTC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        if (callUi is ActiveCallUi.IncomingRinging || callUi is ActiveCallUi.OutgoingRinging) {
                            Box(
                                modifier = Modifier
                                    .size(164.dp)
                                    .scale(outerRingScale)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = outerRingAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(144.dp)
                                    .scale(ringScale)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = ringAlpha))
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            border = BorderStroke(2.dp, Color(0xFFF6C478).copy(alpha = 0.85f)),
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .size(112.dp)
                                .graphicsLayer {
                                    cameraDistance = 12f * density
                                    rotationY = emblemTiltY
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = peerDisplayName.trim().firstOrNull()?.uppercase() ?: "J",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = peerDisplayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (callUi is ActiveCallUi.Connected) WhatsAppGreen else Color(0xFFF6C478),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                when (callUi) {
                    is ActiveCallUi.IncomingRinging -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallActionCircle(
                                icon = Icons.Rounded.CallEnd,
                                label = "Decline",
                                containerColor = Color(0xFFE53935),
                                onClick = { onDecline(callUi.session) },
                                tag = "call_decline_button"
                            )
                            CallActionCircle(
                                icon = Icons.Rounded.Call,
                                label = "Accept",
                                containerColor = WhatsAppGreen,
                                onClick = { onAccept(callUi.session) },
                                tag = "call_accept_button"
                            )
                        }
                    }

                    else -> {
                        val muted = (callUi as? ActiveCallUi.Connected)?.isMuted == true
                        val speaker = (callUi as? ActiveCallUi.Connected)?.isSpeakerOn == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CallActionCircle(
                                icon = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                label = if (muted) "Unmute" else "Mute",
                                containerColor = if (muted) Color.White else Color.White.copy(alpha = 0.16f),
                                iconTint = if (muted) Color.Black else Color.White,
                                onClick = onToggleMute,
                                tag = "call_mute_button"
                            )
                            CallActionCircle(
                                icon = Icons.Rounded.CallEnd,
                                label = "End Call",
                                containerColor = Color(0xFFE53935),
                                onClick = onHangUp,
                                tag = "call_end_button"
                            )
                            CallActionCircle(
                                icon = Icons.Rounded.VolumeUp,
                                label = "Speaker",
                                containerColor = if (speaker) Color.White else Color.White.copy(alpha = 0.16f),
                                iconTint = if (speaker) Color.Black else Color.White,
                                onClick = onToggleSpeaker,
                                tag = "call_speaker_button"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallActionCircle(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    tag: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(66.dp)
                .minimumInteractiveComponentSize()
                .testTag(tag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
