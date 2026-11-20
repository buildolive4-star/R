package com.example.ui.call

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ActiveCallUi
import com.example.data.model.CallSession
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

private data class QuickCallGift(
    val id: String,
    val emoji: String,
    val name: String,
    val coinCost: Int,
)

private val QUICK_CALL_GIFTS = listOf(
    QuickCallGift("rose", "🌹", "Rose", 10),
    QuickCallGift("diamond", "💎", "Diamond", 50),
    QuickCallGift("crown", "👑", "Crown", 120),
    QuickCallGift("car", "🏎️", "Luxury Car", 250),
)

@Composable
fun AudioCallOverlay(
    callUi: ActiveCallUi,
    currentUid: String,
    onAccept: (CallSession) -> Unit,
    onDecline: (CallSession) -> Unit,
    onHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    localVideoTrack: VideoTrack? = null,
    remoteVideoTrack: VideoTrack? = null,
    eglBaseContext: EglBase.Context? = null,
    dateCoinsBalance: Int = 50,
    onToggleCamera: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onSendGift: (name: String, emoji: String, cost: Int) -> Unit = { _, _, _ -> },
) {
    if (callUi is ActiveCallUi.Idle) return

    val session = when (callUi) {
        is ActiveCallUi.OutgoingRinging -> callUi.session
        is ActiveCallUi.IncomingRinging -> callUi.session
        is ActiveCallUi.Connecting -> callUi.session
        is ActiveCallUi.Connected -> callUi.session
        is ActiveCallUi.Idle -> return
    }

    val isVideoCall = session.isVideoCall
    val isCaller = session.callerUid == currentUid
    val peerDisplayName = if (isCaller) {
        session.calleeName.ifBlank { "Companion" }
    } else {
        session.callerName.ifBlank { "JustUs" }
    }
    val subtitleText = session.profileName.takeIf { it.isNotBlank() }?.let { "Profile: $it" }
        ?: if (isVideoCall) "Private HD Video" else "Private Voice Channel"

    var elapsedSeconds by remember(callUi) { mutableLongStateOf(0L) }
    var showGiftStrip by remember(session.id) { mutableStateOf(false) }
    var lastSentGiftBanner by remember(session.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(lastSentGiftBanner) {
        if (lastSentGiftBanner != null) {
            delay(2400L)
            lastSentGiftBanner = null
        }
    }

    if (callUi is ActiveCallUi.Connected) {
        LaunchedEffect(callUi.connectedAtEpochMs) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - callUi.connectedAtEpochMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        }
    }

    val billedMinutes = if (elapsedSeconds <= 0L) 1L else ((elapsedSeconds + 59L) / 60L)
    val estimatedCostInr = if (session.ratePerMinInr > 0) (billedMinutes * session.ratePerMinInr).toInt() else 0

    val timerText = remember(elapsedSeconds) {
        val mins = elapsedSeconds / 60L
        val secs = elapsedSeconds % 60L
        String.format("%02d:%02d", mins, secs)
    }

    val statusText = when (callUi) {
        is ActiveCallUi.OutgoingRinging -> if (isVideoCall) "Ringing Video Call…" else "Ringing… Waiting for answer"
        is ActiveCallUi.IncomingRinging -> if (isVideoCall) "Incoming HD Video Call…" else "Incoming Voice Call…"
        is ActiveCallUi.Connecting -> if (isVideoCall) "Connecting HD Video…" else "Connecting WebRTC audio…"
        is ActiveCallUi.Connected -> if (isVideoCall) "$timerText · Live HD Video" else "$timerText · Encrypted Audio"
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

    val connectedState = callUi as? ActiveCallUi.Connected
    val isCameraOff = connectedState?.isCameraOff == true
    val isFrontCamera = connectedState?.isFrontCamera != false
    val showRemoteVideoSurface = isVideoCall && remoteVideoTrack != null && eglBaseContext != null
    val showLocalVideoPip = isVideoCall && eglBaseContext != null &&
        (callUi is ActiveCallUi.Connected || callUi is ActiveCallUi.Connecting || callUi is ActiveCallUi.OutgoingRinging)

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
                .testTag(if (isVideoCall) "video_call_overlay" else "audio_call_overlay")
        ) {
            // Layer 1: Remote Full-Screen HD Video OR Atmospheric Backdrop
            if (showRemoteVideoSurface) {
                WebRtcVideoSurface(
                    videoTrack = remoteVideoTrack,
                    eglBaseContext = eglBaseContext,
                    mirror = false,
                    zOrderMediaOverlay = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("remote_video_surface")
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99080610),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color(0xCC080610),
                                )
                            )
                        )
                )
            } else {
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
            }

            // Layer 2: Floating Local Camera PiP for Video Calls (Top-End)
            if (showLocalVideoPip) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 68.dp, end = 16.dp)
                        .size(width = 108.dp, height = 154.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF171324))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .testTag("local_video_pip")
                ) {
                    if (!isCameraOff && localVideoTrack != null) {
                        WebRtcVideoSurface(
                            videoTrack = localVideoTrack,
                            eglBaseContext = eglBaseContext,
                            mirror = isFrontCamera,
                            zOrderMediaOverlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideocamOff,
                                contentDescription = "Camera Off",
                                tint = Color.White.copy(alpha = 0.72f),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Cam Off",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }

            // Layer 3: Foreground HUD (Top Bar, Center Avatar when no remote video, Gift Toast & Controls)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top HUD: Channel Pill + Connected Peer / Timer / Live Cost Pill
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = if (isVideoCall) Color(0xFFFF6F9C) else Color(0xFFF6C478),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isVideoCall) "PRIVATE HD VIDEO" else "PRIVATE VOICE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        if (session.ratePerMinInr > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xCC241508),
                                border = BorderStroke(1.dp, Color(0xFFFFD166).copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("call_rate_cost_pill")
                            ) {
                                Text(
                                    text = if (callUi is ActiveCallUi.Connected) {
                                        "₹${session.ratePerMinInr}/min · Est. ₹$estimatedCostInr"
                                    } else {
                                        "₹${session.ratePerMinInr}/min"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFE082),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Compact header banner when remote HD video is live
                    if (showRemoteVideoSurface) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.Black.copy(alpha = 0.48f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = peerDisplayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "• $timerText",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WhatsAppGreen
                                )
                            }
                        }
                    }
                }

                // Center Avatar & Ringing Status (hidden once remote HD video takes full screen)
                if (!showRemoteVideoSurface) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            if (callUi is ActiveCallUi.IncomingRinging || callUi is ActiveCallUi.OutgoingRinging) {
                                val ringColor = if (isVideoCall) Color(0xFFE91E63) else WhatsAppGreen
                                Box(
                                    modifier = Modifier
                                        .size(164.dp)
                                        .scale(outerRingScale)
                                        .clip(CircleShape)
                                        .background(ringColor.copy(alpha = outerRingAlpha))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(144.dp)
                                        .scale(ringScale)
                                        .clip(CircleShape)
                                        .background(ringColor.copy(alpha = ringAlpha))
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
                            color = Color.Black.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (callUi is ActiveCallUi.Connected) WhatsAppGreen else Color(0xFFF6C478),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Bottom Section: Sent Gift Banner + Tango Quick Gift Strip + Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedVisibility(
                        visible = lastSentGiftBanner != null,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xDDE91E63),
                            border = BorderStroke(1.dp, Color(0xFFFFD166))
                        ) {
                            Text(
                                text = lastSentGiftBanner.orEmpty(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Tango-style in-call Quick Virtual Gift Strip
                    AnimatedVisibility(
                        visible = showGiftStrip && callUi is ActiveCallUi.Connected,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xDD171022),
                            border = BorderStroke(1.dp, Color(0xFFFFD166).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("call_gift_tray")
                        ) {
                            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Send Live Gift",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "🪙 $dateCoinsBalance Coins",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD166)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(QUICK_CALL_GIFTS, key = { it.id }) { gift ->
                                        Surface(
                                            onClick = {
                                                onSendGift(gift.name, gift.emoji, gift.coinCost)
                                                lastSentGiftBanner = "${gift.emoji} Sent ${gift.name} (${gift.coinCost} Coins)!"
                                                showGiftStrip = false
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                            modifier = Modifier.testTag("call_gift_${gift.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = gift.emoji,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                Text(
                                                    text = gift.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${gift.coinCost}c",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFFFD166)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
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
                                    icon = if (isVideoCall) Icons.Rounded.Videocam else Icons.Rounded.Call,
                                    label = if (isVideoCall) "Accept Video" else "Accept",
                                    containerColor = WhatsAppGreen,
                                    onClick = { onAccept(callUi.session) },
                                    tag = "call_accept_button"
                                )
                            }
                        }

                        else -> {
                            val muted = connectedState?.isMuted == true
                            val speaker = connectedState?.isSpeakerOn == true
                            if (isVideoCall) {
                                Surface(
                                    shape = RoundedCornerShape(28.dp),
                                    color = Color(0xBF120D1C),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CallActionCircle(
                                            icon = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                            label = if (muted) "Unmute" else "Mute",
                                            containerColor = if (muted) Color.White else Color.White.copy(alpha = 0.16f),
                                            iconTint = if (muted) Color.Black else Color.White,
                                            onClick = onToggleMute,
                                            tag = "call_mute_button",
                                            circleSize = 52.dp
                                        )
                                        CallActionCircle(
                                            icon = if (isCameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                                            label = if (isCameraOff) "Cam Off" else "Camera",
                                            containerColor = if (isCameraOff) Color.White else Color.White.copy(alpha = 0.16f),
                                            iconTint = if (isCameraOff) Color.Black else Color.White,
                                            onClick = onToggleCamera,
                                            tag = "call_camera_button",
                                            circleSize = 52.dp
                                        )
                                        CallActionCircle(
                                            icon = Icons.Rounded.Cameraswitch,
                                            label = "Flip",
                                            containerColor = Color.White.copy(alpha = 0.16f),
                                            onClick = onSwitchCamera,
                                            tag = "call_flip_camera_button",
                                            circleSize = 52.dp
                                        )
                                        CallActionCircle(
                                            icon = Icons.Rounded.CardGiftcard,
                                            label = "Gift",
                                            containerColor = if (showGiftStrip) Color(0xFFFFD166) else Color(0xFFE91E63).copy(alpha = 0.85f),
                                            iconTint = if (showGiftStrip) Color.Black else Color.White,
                                            onClick = { showGiftStrip = !showGiftStrip },
                                            tag = "call_gift_button",
                                            circleSize = 52.dp
                                        )
                                        CallActionCircle(
                                            icon = Icons.Rounded.CallEnd,
                                            label = "End",
                                            containerColor = Color(0xFFE53935),
                                            onClick = onHangUp,
                                            tag = "call_end_button",
                                            circleSize = 54.dp
                                        )
                                    }
                                }
                            } else {
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
                                    if (callUi is ActiveCallUi.Connected) {
                                        CallActionCircle(
                                            icon = Icons.Rounded.CardGiftcard,
                                            label = "Gift",
                                            containerColor = if (showGiftStrip) Color(0xFFFFD166) else Color(0xFFE91E63).copy(alpha = 0.85f),
                                            iconTint = if (showGiftStrip) Color.Black else Color.White,
                                            onClick = { showGiftStrip = !showGiftStrip },
                                            tag = "call_gift_button"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebRtcVideoSurface(
    videoTrack: VideoTrack?,
    eglBaseContext: EglBase.Context?,
    mirror: Boolean,
    zOrderMediaOverlay: Boolean,
    modifier: Modifier = Modifier,
) {
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(rendererRef, videoTrack, mirror) {
        val renderer = rendererRef
        if (renderer != null) {
            runCatching { renderer.setMirror(mirror) }
            if (videoTrack != null) {
                runCatching { videoTrack.addSink(renderer) }
            }
        }
        onDispose {
            if (renderer != null && videoTrack != null) {
                runCatching { videoTrack.removeSink(renderer) }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val renderer = rendererRef
            rendererRef = null
            runCatching { renderer?.release() }
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                setZOrderMediaOverlay(zOrderMediaOverlay)
                setMirror(mirror)
                if (eglBaseContext != null) {
                    runCatching { init(eglBaseContext, null) }
                }
                rendererRef = this
            }
        },
        update = { view ->
            runCatching { view.setMirror(mirror) }
        },
        modifier = modifier
    )
}

@Composable
private fun CallActionCircle(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconTint: Color = Color.White,
    onClick: () -> Unit,
    tag: String,
    circleSize: Dp = 64.dp,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = containerColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(circleSize)
                .minimumInteractiveComponentSize()
                .testTag(tag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(if (circleSize <= 54.dp) 23.dp else 27.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1
        )
    }
}
