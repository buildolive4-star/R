package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.GalleryImage
import com.example.ui.theme.WhatsAppDarkGreen
import com.example.ui.theme.WhatsAppGreen
import com.example.util.Local3DAssetResolver
import kotlinx.coroutines.delay

private fun resolveMatchPercentage(profile: GalleryImage): String {
    val explicit = profile.tags.firstOrNull { it.contains("% Match", ignoreCase = true) }
    if (explicit != null) return explicit
    val score = 92 + (profile.id.hashCode().absoluteValue % 8)
    return "$score% Match"
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha)
        )
    )
}

/**
 * Top 16:9 VIP Staff Showcase Carousel — auto-cycles every 3 seconds.
 */
@Composable
fun VipShowcaseCarousel(
    profiles: List<GalleryImage>,
    bookmarkedIds: Set<String>,
    onProfileClick: (GalleryImage) -> Unit,
    onChatClick: (GalleryImage) -> Unit,
    onToggleBookmark: (GalleryImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (profiles.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { profiles.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(profiles.size, isDragged) {
        if (profiles.size > 1 && !isDragged) {
            while (true) {
                delay(3000L)
                val nextPage = (pagerState.currentPage + 1) % profiles.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val density = LocalDensity.current.density

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vip_16_9_carousel")
        ) { page ->
            val profile = profiles[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1f)
            VipShowcaseCard(
                profile = profile,
                isBookmarked = bookmarkedIds.contains(profile.id),
                onClick = { onProfileClick(profile) },
                onChatClick = { onChatClick(profile) },
                onToggleBookmark = { onToggleBookmark(profile) },
                modifier = Modifier.graphicsLayer {
                    cameraDistance = 14f * density
                    rotationY = (pageOffset * -10f).coerceIn(-12f, 12f)
                    val s = lerp(0.93f, 1f, 1f - absOffset)
                    scaleX = s
                    scaleY = s
                    alpha = lerp(0.76f, 1f, 1f - absOffset)
                }
            )
        }

        if (profiles.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(profiles.size) { idx ->
                    val selected = pagerState.currentPage == idx
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 22.dp else 6.dp,
                        label = "vip_dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun VipShowcaseCard(
    profile: GalleryImage,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAllBlurredExpired = profile.isDeletedBlurred
    val unopenedCount = profile.unopenedPhotoCount()
    val hasActiveOrUnopened = profile.isHidden && !isAllBlurredExpired
    val matchLabel = remember(profile.id, profile.tags) { resolveMatchPercentage(profile) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vip_card_${profile.id}")
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            when {
                isAllBlurredExpired -> {
                    PixoraBlurredImage(
                        imageUrl = profile.displayThumbnailUrl,
                        contentDescription = profile.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x28120914)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xBB1E1220),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BlurOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.expired_blurred_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                hasActiveOrUnopened -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PixoraBlurredImage(
                            imageUrl = profile.displayThumbnailUrl,
                            contentDescription = profile.title,
                            blurRadiusDp = 12,
                            blurPasses = 2,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x55180B1C),
                                            Color(0x6B120816),
                                            Color(0x99120814),
                                        )
                                    )
                                )
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xCC1E1222),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = if (profile.photoCount > 1 && unopenedCount < profile.photoCount) {
                                        "OPEN NEXT PHOTO ($unopenedCount LEFT)"
                                    } else {
                                        stringResource(R.string.hidden_card_badge)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    PixoraNetworkImage(
                        imageUrl = profile.displayThumbnailUrl,
                        contentDescription = profile.title,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Soft bottom gradient so VIP details & Chat button pop crisply
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x36100810),
                                0.5f to Color.Transparent,
                                1.0f to Color(0xEA120814),
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC1B101D),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "VIP · ${profile.role}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xD9E91E63),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = matchLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }

                if (!isAllBlurredExpired) {
                    Surface(
                        onClick = onToggleBookmark,
                        shape = CircleShape,
                        color = if (isBookmarked) Color(0xFFE91E63) else Color(0xB31B101D),
                        border = BorderStroke(
                            1.dp,
                            if (isBookmarked) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f)
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(38.dp)
                            .testTag("vip_like_${profile.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${profile.title}, ${profile.age}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = Color(0xFFFF6F9C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriceBadgePill(price = profile.displayPrice)
                        if (profile.category.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x881F1224),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = profile.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                WhatsAppChatPillButton(
                    onClick = onChatClick,
                    compact = false,
                    modifier = Modifier.testTag("vip_chat_btn_${profile.id}")
                )
            }
        }
    }
}

@Composable
fun WhatsAppChatPillButton(
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    DateMate3DGlowingButton(
        text = "Chat",
        onClick = onClick,
        style = GlowButtonStyle.ROSE_PINK,
        leadingIcon = Icons.AutoMirrored.Rounded.Chat,
        compact = compact,
        cornerRadius = 999.dp,
        contentPadding = PaddingValues(
            horizontal = if (compact) 11.dp else 15.dp,
            vertical = if (compact) 6.dp else 9.dp
        ),
        modifier = modifier
    )
}

@Composable
fun PriceBadgePill(
    price: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = price,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

/**
 * 2-by-2 Grid 9:16 Companion Card.
 * Includes Like/Heart button for Favorites, Compatibility Match %, Verified Badge, and 'Chat' button.
 */
@Composable
fun GalleryGridCard(
    image: GalleryImage,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    isNearby: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isAllBlurredExpired = image.isDeletedBlurred
    val unopenedCount = image.unopenedPhotoCount()
    val hasActiveOrUnopened = image.isHidden && !isAllBlurredExpired
    val matchLabel = remember(image.id, image.tags) { resolveMatchPercentage(image) }
    val density = LocalDensity.current.density
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_press_scale"
    )
    val pressTiltX by animateFloatAsState(
        targetValue = if (isPressed) 4.5f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_press_tilt"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isNearby) 1.8.dp else 1.2.dp,
            brush = Brush.linearGradient(
                colors = if (isNearby) {
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.primary,
                    )
                } else if (isAllBlurredExpired) {
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
                } else {
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.surface,
                    )
                }
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 3.dp else 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isNearby) 14.dp else 9.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isNearby) 0.32f else 0.18f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isNearby) 0.38f else 0.22f)
            )
            .graphicsLayer {
                cameraDistance = 12f * density
                scaleX = pressScale
                scaleY = pressScale
                rotationX = pressTiltX
            }
            .testTag("gallery_card_${image.id}")
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
            ) {
                when {
                    isAllBlurredExpired -> {
                        PixoraBlurredImage(
                            imageUrl = image.displayThumbnailUrl,
                            contentDescription = image.title,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x22140A14)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xBA1E1220),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BlurOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.expired_blurred_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFDF6FA),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    hasActiveOrUnopened -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PixoraBlurredImage(
                                imageUrl = image.displayThumbnailUrl,
                                contentDescription = image.title,
                                blurRadiusDp = 12,
                                blurPasses = 2,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0x40160A18),
                                                Color(0x66140A16),
                                                Color(0x88120814),
                                            )
                                        )
                                    )
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xCC211424),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = if (image.photoCount > 1 && unopenedCount < image.photoCount) {
                                            "$unopenedCount/${image.photoCount} OPEN NEXT"
                                        } else if (image.photoCount > 1) {
                                            "BLUR · ${image.photoCount} PICS"
                                        } else {
                                            stringResource(R.string.hidden_card_badge)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        PixoraNetworkImage(
                            imageUrl = image.displayThumbnailUrl,
                            contentDescription = image.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Top-left Compatibility Match % & Multi-photo count badge
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xD9E91E63),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = matchLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    if (image.allPhotoUrls.size > 1 && !image.isHidden && !isAllBlurredExpired) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0x9E1C101E)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${image.allPhotoUrls.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Top-right Like / Heart Button (adds/removes from Favorites tab)
                if (!isAllBlurredExpired) {
                    Surface(
                        onClick = onToggleBookmark,
                        shape = CircleShape,
                        color = if (isBookmarked) Color(0xFFE91E63) else Color(0xB31C101E),
                        border = BorderStroke(
                            1.dp,
                            if (isBookmarked) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f)
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .minimumInteractiveComponentSize()
                            .size(36.dp)
                            .testTag("card_bookmark_${image.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(
                                    if (isBookmarked) R.string.bookmark_remove else R.string.bookmark_add
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Bottom-left Nearby / City / Category tag over photo
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isNearby && !isAllBlurredExpired) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xE600BFA5),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = "Near You",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Near You",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    } else if (image.city.isNotBlank() && !isAllBlurredExpired) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xB81C101E),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFF80AB),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = image.city,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else if (image.category.isNotBlank() && !isAllBlurredExpired) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xB31C101E),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = image.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Footer: Name, Age, Verified Badge, Price + 'Chat' Pill Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${image.title}, ${image.age}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = image.role,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PriceBadgePill(
                        price = image.displayPrice,
                        modifier = Modifier.testTag("card_price_${image.id}")
                    )
                    WhatsAppChatPillButton(
                        onClick = onChatClick,
                        compact = true,
                        modifier = Modifier.testTag("card_chat_btn_${image.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun PixoraNetworkImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showSpinnerOnLoad: Boolean = false,
) {
    val context = LocalContext.current
    val resolvedModel = remember(imageUrl) {
        Local3DAssetResolver.resolveCoilModel(context, imageUrl)
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedModel)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            if (showSpinnerOnLoad) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF130B14)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(34.dp)
                    )
                }
            } else {
                ShimmerBox(modifier = Modifier.fillMaxSize())
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BrokenImage,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    )
}

/**
 * Soft predictable blur after 40s expiry — intentionally lighter so the person
 * in the photo remains gently recognizable while preserving privacy.
 */
@Composable
fun PixoraBlurredImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurRadiusDp: Int = 8,
    blurPasses: Int = 1,
) {
    val context = LocalContext.current
    val blurTransform = remember(blurPasses) {
        LowResBlurTransformation(maxGridDim = 84, blurPasses = blurPasses, outputScale = 4)
    }
    val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modifier.blur(blurRadiusDp.dp)
    } else {
        modifier
    }

    val resolvedModel = remember(imageUrl) {
        Local3DAssetResolver.resolveCoilModel(context, imageUrl)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedModel)
            .allowHardware(false)
            .transformations(blurTransform)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = blurModifier,
        loading = {
            ShimmerBox(modifier = Modifier.fillMaxSize())
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF281726), Color(0xFF1A0F1A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BlurOn,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    )
}

@Composable
fun EmptyGalleryState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    mascot: DateMateMascot = DateMateMascot.LOVE_MATE,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Animated3DMascotOrb(
            mascot = mascot,
            size = 112.dp,
            showOrbitSparkles = true,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            DateMate3DGlowingButton(
                text = actionLabel,
                onClick = onAction,
                style = GlowButtonStyle.ROSE_PINK,
                compact = true,
                modifier = Modifier.widthIn(min = 160.dp)
            )
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(14.dp))
                DateMate3DGlowingButton(
                    text = stringResource(R.string.error_retry),
                    onClick = onRetry,
                    style = GlowButtonStyle.EMERALD_CHAT,
                    compact = true,
                )
            }
        }
    }
}
