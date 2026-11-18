package com.example.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bookmark
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.components.PixoraBlurredImage
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.delay

@Composable
fun ImageDetailScreen(
    profile: GalleryImage?,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (GalleryImage) -> Unit,
    onRevealHiddenImage: (GalleryImage) -> Unit,
    onChatClick: (GalleryImage) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    if (profile == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val photoUrls = remember(profile.id, profile.photoUrls, profile.imageUrl) {
        profile.allPhotoUrls.ifEmpty { listOf(profile.displayThumbnailUrl) }
    }
    val pagerState = rememberPagerState(pageCount = { photoUrls.size.coerceAtLeast(1) })
    val isBookmarked = bookmarkedIds.contains(profile.id)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }
    var remainingSec by remember(profile.id, profile.revealExpiresAtEpochMs, profile.isExpired) {
        mutableIntStateOf(profile.remainingSeconds())
    }

    LaunchedEffect(
        profile.id,
        profile.isHidden,
        profile.revealExpiresAtEpochMs,
        profile.isExpired,
        profile.refreshEpochMs,
    ) {
        if (profile.isExpired) {
            remainingSec = 0
        } else if (profile.isRevealStarted) {
            while (true) {
                val secs = profile.remainingSeconds()
                remainingSec = secs
                if (secs <= 0) break
                delay(250L)
            }
        } else {
            remainingSec = 40
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    val isCurrentExpired = profile.isDeletedBlurred || (profile.isRevealStarted && remainingSec <= 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D080E))
    ) {
        when {
            isCurrentExpired -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    PixoraBlurredImage(
                        imageUrl = photoUrls.first(),
                        contentDescription = profile.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x38120912))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color(0xDC1B111E),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .testTag("detail_expired_blurred_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.BlurOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = stringResource(R.string.expired_detail_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.expired_detail_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD8C6D5),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = { onChatClick(profile) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "${stringResource(R.string.chat_now)} with ${profile.title}", color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_expired_back_button")
                                ) {
                                    Text(text = stringResource(R.string.back), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            profile.isHidden && !profile.isRevealStarted -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF150C17), Color(0xFF231325), Color(0xFF0E090E))
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = Color(0xDD1C111E),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
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
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            PriceBadgePill(price = profile.displayPrice)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onRevealHiddenImage(profile) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_reveal_hidden_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.reveal_photo_button))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.hidden_detail_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6C4D4),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            else -> {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = scale <= 1.05f,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val photoUrl = photoUrls[page]
                    val isCurrentPage = page == pagerState.currentPage

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(photoUrl) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    if (isCurrentPage) {
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        offset = if (newScale > 1.05f) {
                                            Offset(
                                                x = (offset.x + pan.x).coerceIn(-700f, 700f),
                                                y = (offset.y + pan.y).coerceIn(-900f, 900f)
                                            )
                                        } else {
                                            Offset.Zero
                                        }
                                    }
                                }
                            }
                            .pointerInput(photoUrl) {
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
                            contentDescription = profile.title,
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
            visible = showChrome || isCurrentExpired,
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
                        if (profile.isRevealStarted && !isCurrentExpired) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
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
                                        text = stringResource(R.string.hidden_countdown_badge, remainingSec),
                                        style = MaterialTheme.typography.labelMedium,
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

                        if (photoUrls.size > 1 && !isCurrentExpired) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xAA1D1220)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${photoUrls.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE0CEDD),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (!profile.isHidden && !isCurrentExpired) {
                        IconButton(
                            onClick = { onToggleBookmark(profile) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(Color(0xAA1D1220))
                                .testTag("detail_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(
                                    if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                                ),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }

        // Bottom Floating Profile Details + WhatsApp Chat Action (NO Phone/Address exposed!)
        AnimatedVisibility(
            visible = showChrome && !isCurrentExpired,
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = Color(0xDD1C111E),
                    border = BorderStroke(1.dp, Color(0x2EFFC0D5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            PriceBadgePill(price = profile.displayPrice)
                        }

                        if (profile.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = profile.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF3E4F0),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (photoUrls.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.swipe_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA791A4)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { onChatClick(profile) },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("detail_whatsapp_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chat on JustUs · ${profile.displayPrice}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
