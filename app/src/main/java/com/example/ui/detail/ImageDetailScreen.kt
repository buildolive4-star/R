package com.example.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppShareConfig
import com.example.data.model.GalleryImage
import com.example.ui.components.CupidLikeSuccessOverlay
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMate3DGlowingIconButton
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.LuxuryDateInviteCardDialog
import com.example.ui.components.PixoraBlurredImage
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.ui.theme.OnlineEmeraldDot
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImageDetailScreen(
    profile: GalleryImage?,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (GalleryImage) -> Unit,
    onRevealHiddenImage: (GalleryImage, Int) -> Unit,
    onChatClick: (GalleryImage) -> Unit,
    onVoiceCallClick: (GalleryImage) -> Unit = {},
    onVideoCallClick: (GalleryImage) -> Unit = {},
    isCurrentUserVideoAllowed: Boolean = true,
    onBookClick: (GalleryImage) -> Unit = {},
    defaultAdvanceInr: Int = 299,
    appShareConfig: AppShareConfig = AppShareConfig(),
    emergencyContactName: String = "",
    emergencyContactPhone: String = "",
    onSaveEmergencyContact: (name: String, phone: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (profile == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val scope = rememberCoroutineScope()
    val photoUrls = remember(profile.id, profile.photoUrls, profile.imageUrl) {
        profile.allPhotoUrls.ifEmpty { listOf(profile.displayThumbnailUrl) }
    }
    val initialOpenPage = remember(profile.id, profile.refreshEpochMs) {
        profile.nextOpenablePhotoIndex().coerceIn(0, (photoUrls.size - 1).coerceAtLeast(0))
    }
    val pagerState = rememberPagerState(
        initialPage = initialOpenPage,
        pageCount = { photoUrls.size.coerceAtLeast(1) }
    )
    val isBookmarked = bookmarkedIds.contains(profile.id)
    var cupidCelebrationName by remember { mutableStateOf<String?>(null) }
    val handleLikeToggle: () -> Unit = {
        val wasBookmarked = bookmarkedIds.contains(profile.id)
        onToggleBookmark(profile)
        if (!wasBookmarked) {
            cupidCelebrationName = profile.title
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }
    var bioExpanded by remember(profile.id) { mutableStateOf(false) }
    var showInviteCardDialog by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(profile.id, profile.photoRevealStates, profile.refreshEpochMs) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(250L)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    val currentPage = pagerState.currentPage
    val currentExpired = profile.isPhotoExpired(currentPage, nowMs)
    val currentRevealStarted = profile.isPhotoRevealStarted(currentPage, nowMs)
    val currentRemainingSec = profile.remainingSecondsForPhoto(currentPage, nowMs)
    val unopenedCount = profile.unopenedPhotoCount(nowMs)
    val videoAllowedHere = profile.videoCallEnabled && isCurrentUserVideoAllowed
    val advanceInr = remember(profile.id, profile.advanceBookingAmount, defaultAdvanceInr) {
        profile.resolvedAdvanceAmountInr(defaultAdvanceInr)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A070D))
    ) {
        // Full-bleed swipeable gallery with 40s VIP reveal & pinch zoom
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            modifier = Modifier
                .fillMaxSize()
                .testTag("detail_photo_pager")
        ) { page ->
            val photoUrl = photoUrls[page]
            val isCurrentPage = page == pagerState.currentPage
            val pageExpired = profile.isPhotoExpired(page, nowMs)
            val pageRevealStarted = profile.isPhotoRevealStarted(page, nowMs)
            val pageLockedUnopened = profile.isHidden && !pageRevealStarted && !pageExpired

            when {
                pageExpired -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(photoUrl, page) {
                                detectTapGestures(onTap = { showChrome = !showChrome })
                            }
                    ) {
                        PixoraBlurredImage(
                            imageUrl = photoUrl,
                            contentDescription = "${profile.title} photo ${page + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x45120912))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = Color(0xE018101C),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 380.dp)
                                    .testTag("detail_expired_blurred_card_$page")
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.BlurOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (photoUrls.size > 1) {
                                            "Photo ${page + 1} · ${stringResource(R.string.expired_detail_title)}"
                                        } else {
                                            stringResource(R.string.expired_detail_title)
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (unopenedCount > 0) {
                                            "Swipe to view $unopenedCount remaining photo(s)."
                                        } else {
                                            stringResource(R.string.expired_detail_subtitle)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFD8C6D5),
                                        textAlign = TextAlign.Center
                                    )
                                    if (unopenedCount > 0) {
                                        val nextIdx = profile.unopenedPhotoIndices(nowMs).firstOrNull()
                                        if (nextIdx != null) {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            DateMate3DGlowingButton(
                                                text = "Open Photo ${nextIdx + 1}",
                                                onClick = { scope.launch { pagerState.animateScrollToPage(nextIdx) } },
                                                style = GlowButtonStyle.ROYAL_GOLD,
                                                leadingIcon = Icons.Rounded.Swipe,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("detail_open_next_photo_button")
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    DateMate3DGlowingButton(
                                        text = "Chat Now",
                                        onClick = { onChatClick(profile) },
                                        style = GlowButtonStyle.ROSE_PINK,
                                        leadingIcon = Icons.AutoMirrored.Rounded.Chat,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                pageLockedUnopened -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF150C17), Color(0xFF271429), Color(0xFF0E090E))
                                )
                            )
                            .pointerInput(photoUrl, page) {
                                detectTapGestures(onTap = { showChrome = !showChrome })
                            }
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = Color(0xE01A1020),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 380.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "${profile.title}, ${profile.age}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                if (photoUrls.size > 1) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Private Photo ${page + 1} of ${photoUrls.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFB2CE)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                DateMate3DGlowingButton(
                                    text = if (photoUrls.size > 1) {
                                        "Reveal Photo ${page + 1} (40s)"
                                    } else {
                                        "${stringResource(R.string.reveal_photo_button)} (40s)"
                                    },
                                    onClick = { onRevealHiddenImage(profile, page) },
                                    style = GlowButtonStyle.ROYAL_GOLD,
                                    leadingIcon = Icons.Rounded.Visibility,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("detail_reveal_hidden_button_$page")
                                )
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(photoUrl, page) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    do {
                                        val event = awaitPointerEvent()
                                        val pressedCount = event.changes.count { it.pressed }
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        if (isCurrentPage && (pressedCount >= 2 || scale > 1.05f)) {
                                            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                            scale = newScale
                                            offset = if (newScale > 1.05f) {
                                                Offset(
                                                    x = (offset.x + panChange.x).coerceIn(-750f, 750f),
                                                    y = (offset.y + panChange.y).coerceIn(-950f, 950f)
                                                )
                                            } else {
                                                Offset.Zero
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    } while (event.changes.any { it.pressed })
                                }
                            }
                            .pointerInput(photoUrl, page) {
                                detectTapGestures(
                                    onTap = { showChrome = !showChrome },
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.2f
                                        }
                                    }
                                )
                            }
                    ) {
                        PixoraNetworkImage(
                            imageUrl = photoUrl,
                            contentDescription = "${profile.title} photo ${page + 1}",
                            contentScale = ContentScale.Crop,
                            showSpinnerOnLoad = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isCurrentPage) {
                                        Modifier.graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        }

        // Minimal Floating Top Bar: Back | Countdown or Photo Count | Share + Favorite
        AnimatedVisibility(
            visible = showChrome || currentExpired,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xB80A070D), Color.Transparent))
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(Color(0x99160E1A))
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentRevealStarted && !currentExpired) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                modifier = Modifier.testTag("detail_countdown_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.hidden_countdown_badge, currentRemainingSec),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else if (photoUrls.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x99160E1A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (!currentExpired) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showInviteCardDialog = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x99160E1A))
                                    .testTag("detail_share_card_pill")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            DateMate3DGlowingIconButton(
                                icon = if (isBookmarked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                                ),
                                onClick = handleLikeToggle,
                                style = if (isBookmarked) GlowButtonStyle.ROSE_PINK else GlowButtonStyle.GLASS_SOFT,
                                size = 42.dp,
                                iconSize = 19.dp,
                                modifier = Modifier.testTag("detail_bookmark_button")
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(44.dp))
                    }
                }
            }
        }

        // Clean, Simple Bottom Glass Sheet (OnlyFans + Tango Private Creator Layout)
        AnimatedVisibility(
            visible = showChrome && !currentExpired,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC0A070D), Color(0xFA0A070D))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xDF170F1C),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        // Compact photo strip when multiple photos exist
                        if (photoUrls.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("detail_thumbnail_strip")
                            ) {
                                itemsIndexed(photoUrls) { index, thumbUrl ->
                                    val selected = index == pagerState.currentPage
                                    val thumbExpired = profile.isPhotoExpired(index, nowMs)
                                    val thumbRevealed = profile.isPhotoRevealStarted(index, nowMs)
                                    val thumbLocked = profile.isHidden && !thumbExpired && !thumbRevealed
                                    Box(
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 52.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(9.dp)
                                            )
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                            .testTag("detail_thumb_$index")
                                    ) {
                                        when {
                                            thumbExpired -> PixoraBlurredImage(
                                                imageUrl = thumbUrl,
                                                contentDescription = "Photo ${index + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            thumbLocked -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF28162C)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = "Locked",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            else -> PixoraNetworkImage(
                                                imageUrl = thumbUrl,
                                                contentDescription = "Photo ${index + 1}",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Row 1: Name, Age, Verified, Online dot + Date Price
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${profile.title}, ${profile.age}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                if (profile.staffOnline) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(OnlineEmeraldDot)
                                            .testTag("detail_online_badge")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            PriceBadgePill(price = profile.displayPrice)
                        }

                        // Row 2: City · Role + Clean expandable 2-line bio
                        val metaLine = buildString {
                            if (profile.city.isNotBlank()) append(profile.city)
                            if (profile.role.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(profile.role)
                            }
                        }
                        if (metaLine.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8BB5),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = metaLine,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD9C6D8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (profile.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFEADCE8),
                                maxLines = if (bioExpanded) 6 else 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { bioExpanded = !bioExpanded }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 3: Clean 3-Action Private Connect Bar (Chat / Audio / Video with ₹/min)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrivateActionPill(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                title = "Chat",
                                rateText = if (profile.chatPricePerMin > 0) "₹${profile.chatPricePerMin}/m" else "Private",
                                accentColor = Color(0xFFE91E63),
                                enabled = true,
                                onClick = { onChatClick(profile) },
                                tag = "detail_whatsapp_chat_button",
                                modifier = Modifier.weight(1f)
                            )
                            PrivateActionPill(
                                icon = Icons.Rounded.Call,
                                title = "Audio",
                                rateText = if (profile.voiceCallPricePerMin > 0) "₹${profile.voiceCallPricePerMin}/m" else "Voice",
                                accentColor = WhatsAppGreen,
                                enabled = true,
                                onClick = { onVoiceCallClick(profile) },
                                tag = "detail_voice_call_button",
                                modifier = Modifier.weight(1f)
                            )
                            PrivateActionPill(
                                icon = if (videoAllowedHere) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                                title = "Video",
                                rateText = when {
                                    !videoAllowedHere -> "Off"
                                    profile.videoCallPricePerMin > 0 -> "₹${profile.videoCallPricePerMin}/m"
                                    else -> "HD Live"
                                },
                                accentColor = if (videoAllowedHere) Color(0xFF9C27B0) else Color(0xFF6B6275),
                                enabled = videoAllowedHere,
                                onClick = { onVideoCallClick(profile) },
                                tag = "detail_video_call_button",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 4: Single Clean Bold CTA — Book Date
                        DateMate3DGlowingButton(
                            text = "Book Date · Advance ₹$advanceInr",
                            onClick = { onBookClick(profile) },
                            style = GlowButtonStyle.ROYAL_GOLD,
                            leadingIcon = Icons.Rounded.CalendarMonth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("detail_book_now_button")
                        )
                    }
                }
            }
        }

        // Animated 3D Cupid Like Success Celebration Overlay
        CupidLikeSuccessOverlay(
            likedProfileName = cupidCelebrationName,
            onFinished = { cupidCelebrationName = null },
            modifier = Modifier.align(Alignment.Center)
        )
    }

    if (showInviteCardDialog) {
        LuxuryDateInviteCardDialog(
            profile = profile,
            appShareConfig = appShareConfig,
            onDismiss = { showInviteCardDialog = false },
        )
    }
}

@Composable
private fun PrivateActionPill(
    icon: ImageVector,
    title: String,
    rateText: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = if (enabled) 0.20f else 0.12f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (enabled) 0.65f else 0.30f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.50f),
                maxLines = 1
            )
            Text(
                text = rateText,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color(0xFFFFE082) else Color.White.copy(alpha = 0.42f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
