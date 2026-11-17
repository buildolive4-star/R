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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GalleryImage
import com.example.ui.components.PixoraBlurredImage
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.util.ImageActions
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageDetailScreen(
    images: List<GalleryImage>,
    initialImageId: String,
    bookmarkedIds: Set<String>,
    onToggleBookmark: (GalleryImage) -> Unit,
    onRevealHiddenImage: (GalleryImage) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val initialStillExists = remember(images, initialImageId) {
        images.any { it.id == initialImageId }
    }
    if (images.isEmpty() || !initialStillExists) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val initialIndex = remember(images, initialImageId) {
        images.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    val currentImage = images[pagerState.currentPage.coerceIn(0, images.lastIndex)]
    val isBookmarked = bookmarkedIds.contains(currentImage.id)

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var remainingSec by remember(currentImage.id, currentImage.revealExpiresAtEpochMs, currentImage.isExpired) {
        mutableIntStateOf(currentImage.remainingSeconds())
    }

    LaunchedEffect(
        currentImage.id,
        currentImage.isHidden,
        currentImage.revealExpiresAtEpochMs,
        currentImage.isExpired,
        currentImage.refreshEpochMs,
    ) {
        if (currentImage.isExpired) {
            remainingSec = 0
        } else if (currentImage.isRevealStarted) {
            while (true) {
                val secs = currentImage.remainingSeconds()
                remainingSec = secs
                if (secs <= 0) break
                delay(250L)
            }
        } else {
            remainingSec = 20
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    val formattedDate = remember(currentImage.createdAt, currentImage.updatedAt) {
        val ts = currentImage.createdAt ?: currentImage.updatedAt ?: Timestamp.now()
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(ts.toDate())
    }

    val copiedMessage = stringResource(R.string.link_copied)
    val savedMessage = stringResource(R.string.photo_saved)
    val saveFailedMessage = stringResource(R.string.photo_save_failed)
    val isCurrentExpired = currentImage.isDeletedBlurred || (currentImage.isRevealStarted && remainingSec <= 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E090E))
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageImage = images[page]
            val isCurrentPage = page == pagerState.currentPage
            val isPageExpired = pageImage.isDeletedBlurred ||
                (isCurrentPage && pageImage.isRevealStarted && remainingSec <= 0)

            when {
                isPageExpired -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        PixoraBlurredImage(
                            imageUrl = pageImage.displayThumbnailUrl,
                            contentDescription = "${pageImage.title} (blurred memory)",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x77120912))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = Color(0xDE1B111E),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                ),
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
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.BlurOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.expired_detail_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.expired_detail_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFD8C6D5),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    OutlinedButton(
                                        onClick = onBack,
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        ),
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

                pageImage.isHidden && !pageImage.isRevealStarted -> {
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
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.hidden_detail_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                PriceBadgePill(price = pageImage.displayPrice)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.hidden_detail_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD6C4D4),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { onRevealHiddenImage(pageImage) },
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
                            }
                        }
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pageImage.id) {
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
                            .pointerInput(pageImage.id) {
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
                            imageUrl = pageImage.imageUrl,
                            contentDescription = pageImage.title,
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
                        if (isCurrentExpired) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xCC281426),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("detail_expired_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BlurOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = stringResource(R.string.expired_blurred_badge),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        } else if (currentImage.isRevealStarted) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, Color(0x66FFFFFF)),
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
                                    text = currentImage.category,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (images.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xAA1D1220)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${images.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFE0CEDD),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (!currentImage.isHidden && !isCurrentExpired) {
                        IconButton(
                            onClick = { onToggleBookmark(currentImage) },
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA1D1220)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = stringResource(R.string.screen_protection_active),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Floating Metadata & Actions (hidden once expired to keep blurred lock card clean)
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
                            Text(
                                text = currentImage.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            PriceBadgePill(price = currentImage.displayPrice)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${stringResource(R.string.uploaded_by_prefix, currentImage.uploaderName)} · $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC6B3C3)
                        )

                        if (currentImage.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentImage.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF3E4F0),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (currentImage.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                currentImage.tags.take(6).forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x24FF7AA2)
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF5D8E4),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (images.size > 1) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.swipe_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA791A4)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (currentImage.isHidden) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x22FFFFFF),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentImage.isRevealStarted) {
                                            stringResource(R.string.hidden_protected_countdown_notice, remainingSec)
                                        } else {
                                            stringResource(R.string.hidden_protected_notice)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFF3E4F0)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (!isSaving) {
                                            isSaving = true
                                            scope.launch {
                                                val result = ImageActions.saveImageToGallery(context, currentImage)
                                                isSaving = false
                                                onShowMessage(
                                                    if (result.isSuccess) savedMessage else saveFailedMessage
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isSaving,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_save_button")
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.save_to_device),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        ImageActions.shareImage(
                                            context,
                                            currentImage.title,
                                            currentImage.imageUrl
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_share_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = stringResource(R.string.share_image),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(currentImage.imageUrl))
                                        onShowMessage(copiedMessage)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("detail_copy_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = stringResource(R.string.copy_link),
                                        modifier = Modifier.size(18.dp)
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
