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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileTier
import com.example.ui.components.Animated3DMascotOrb
import com.example.ui.components.CupidLikeSuccessOverlay
import com.example.ui.components.DateMate3DGlowingButton
import com.example.ui.components.DateMate3DGlowingIconButton
import com.example.ui.components.DateMateMascot
import com.example.ui.components.GlowButtonStyle
import com.example.ui.components.PixoraBlurredImage
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D080E))
    ) {
        // HorizontalPager is ALWAYS mounted across all uploaded photos so single-finger horizontal swiping works seamlessly!
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
                                .background(Color(0x40120912))
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = Color(0xDC1B111E),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 400.dp)
                                    .testTag("detail_expired_blurred_card_$page")
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                        modifier = Modifier.size(58.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.BlurOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
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
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (unopenedCount > 0) {
                                            "This photo faded into memory. Swipe left/right — $unopenedCount other photo(s) can still be opened!"
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
                                                text = "Go to Photo ${nextIdx + 1} ($unopenedCount Left)",
                                                onClick = { scope.launch { pagerState.animateScrollToPage(nextIdx) } },
                                                style = GlowButtonStyle.ROYAL_GOLD,
                                                leadingIcon = Icons.Rounded.Swipe,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("detail_open_next_photo_button")
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    DateMate3DGlowingButton(
                                        text = "Chat",
                                        onClick = { onChatClick(profile) },
                                        style = GlowButtonStyle.ROSE_PINK,
                                        leadingIcon = Icons.AutoMirrored.Rounded.Chat,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    DateMate3DGlowingButton(
                                        text = stringResource(R.string.back),
                                        onClick = onBack,
                                        style = GlowButtonStyle.GLASS_SOFT,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("detail_expired_back_button")
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
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color(0xDE1D1220),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 400.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
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
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                PriceBadgePill(price = profile.displayPrice)
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (photoUrls.size > 1) {
                                        "Each photo has its own 40s viewing timer. Swipe left or right anytime to open other photos."
                                    } else {
                                        stringResource(R.string.hidden_detail_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6C4D4),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Visible clear photo (either standard OR actively revealed inside 40s window).
                    // IMPORTANT: Only consume pointer events for 2-finger pinch OR when already zoomed (scale > 1.05f),
                    // so single-finger horizontal swipes ALWAYS pass cleanly to HorizontalPager!
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
                            contentScale = ContentScale.Fit,
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

        // Top Floating Toolbar
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
                        Brush.verticalGradient(
                            listOf(Color(0xCC0E090E), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            .background(Color(0xAA1D1220))
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
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.hidden_countdown_badge, currentRemainingSec),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xAA1D1220),
                                border = BorderStroke(1.dp, Color(0x44FFFFFF))
                            ) {
                                Text(
                                    text = "${profile.role} · ${profile.category}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (photoUrls.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xAA1D1220),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF))
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF3E6F1),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (!currentExpired) {
                        DateMate3DGlowingIconButton(
                            icon = if (isBookmarked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                            ),
                            onClick = handleLikeToggle,
                            style = if (isBookmarked) GlowButtonStyle.ROSE_PINK else GlowButtonStyle.GLASS_SOFT,
                            size = 44.dp,
                            iconSize = 20.dp,
                            modifier = Modifier.testTag("detail_bookmark_button")
                        )
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }

        // Bottom Floating Profile Details + Interactive Photo Strip + WhatsApp Chat Action
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
                            listOf(Color.Transparent, Color(0xD90E090E), Color(0xFA0E090E))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color(0xDD1C111E),
                    border = BorderStroke(1.dp, Color(0x33FFC0D5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (photoUrls.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            .size(width = 48.dp, height = 62.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (selected) 2.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.primary else Color(0x44FFFFFF),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                scope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                            .testTag("detail_thumb_$index")
                                    ) {
                                        when {
                                            thumbExpired -> {
                                                PixoraBlurredImage(
                                                    imageUrl = thumbUrl,
                                                    contentDescription = "Blurred photo ${index + 1}",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0x55120814)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.BlurOn,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            thumbLocked -> {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(Color(0xFF28162C)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Lock,
                                                        contentDescription = "Locked photo ${index + 1}",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            else -> {
                                                PixoraNetworkImage(
                                                    imageUrl = thumbUrl,
                                                    contentDescription = "Photo ${index + 1}",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xAA0D080E),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (profile.tier == ProfileTier.VIP_16_9) {
                                    Animated3DMascotOrb(
                                        mascot = DateMateMascot.ROYAL_MATE,
                                        size = 34.dp,
                                        showOrbitSparkles = false,
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            PriceBadgePill(price = profile.displayPrice)
                        }

                        // Trust & Safety Shield + Voice Bio Intro Bar
                        var isPlayingVoiceBio by remember(profile.id) { mutableStateOf(false) }
                        LaunchedEffect(isPlayingVoiceBio) {
                            if (isPlayingVoiceBio) {
                                kotlinx.coroutines.delay(5000L)
                                isPlayingVoiceBio = false
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x2822C55E),
                                border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ID & Voice Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBBF7D0)
                                    )
                                }
                            }

                            Surface(
                                onClick = { isPlayingVoiceBio = !isPlayingVoiceBio },
                                shape = RoundedCornerShape(50),
                                color = if (isPlayingVoiceBio) Color(0xFFE91E63) else Color(0x33FF6F9C),
                                border = BorderStroke(1.dp, Color(0xFFFF6F9C).copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingVoiceBio) Icons.Rounded.Stop else Icons.Rounded.GraphicEq,
                                        contentDescription = "Voice Intro",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isPlayingVoiceBio) "Playing Voice Bio (0:05)…" else "Listen Voice Bio",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (profile.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF3E4F0),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (profile.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(profile.tags) { _, tag ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x33FFFFFF),
                                        border = BorderStroke(0.8.dp, Color(0x55FF6F9C))
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (photoUrls.size > 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (profile.isHidden && unopenedCount > 0) {
                                    "Swipe or tap thumbnails · $unopenedCount photo(s) ready to reveal"
                                } else {
                                    stringResource(R.string.swipe_hint)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFBBA4B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateMate3DGlowingButton(
                                text = if (isBookmarked) "Liked" else "Like",
                                onClick = handleLikeToggle,
                                style = if (isBookmarked) GlowButtonStyle.ROSE_PINK else GlowButtonStyle.GLASS_SOFT,
                                leadingIcon = if (isBookmarked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                compact = true,
                                cornerRadius = 16.dp,
                                modifier = Modifier.testTag("detail_bottom_like_button")
                            )

                            DateMate3DGlowingButton(
                                text = "Chat",
                                onClick = { onChatClick(profile) },
                                style = GlowButtonStyle.ROSE_PINK,
                                leadingIcon = Icons.AutoMirrored.Rounded.Chat,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_whatsapp_chat_button")
                            )
                        }
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
}
