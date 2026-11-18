package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GalleryImage
import com.example.ui.theme.WhatsAppGreen

sealed interface InAppBannerEvent {
    data class ProfileArrival(val image: GalleryImage) : InAppBannerEvent
    data class ChatMessageArrival(
        val chatId: String,
        val senderRoleForOpen: String, // "client", "staff", "admin"
        val title: String,
        val preview: String,
        val avatarUrl: String,
    ) : InAppBannerEvent
}

@Composable
fun InAppNotificationBanner(
    event: InAppBannerEvent?,
    onOpenProfile: (GalleryImage) -> Unit,
    onOpenChat: (chatId: String, senderRole: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var displayedEvent by remember { mutableStateOf<InAppBannerEvent?>(null) }
    if (event != null) {
        displayedEvent = event
    }
    val active = displayedEvent

    AnimatedVisibility(
        visible = event != null,
        enter = slideInVertically(initialOffsetY = { -it - 40 }),
        exit = slideOutVertically(targetOffsetY = { -it - 40 }),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (active != null) {
            val isChat = active is InAppBannerEvent.ChatMessageArrival
            val borderColor = if (isChat) {
                WhatsAppGreen.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("in_app_notification_banner")
                    .clickable {
                        when (active) {
                            is InAppBannerEvent.ProfileArrival -> onOpenProfile(active.image)
                            is InAppBannerEvent.ChatMessageArrival -> onOpenChat(active.chatId, active.senderRoleForOpen)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (active) {
                        is InAppBannerEvent.ProfileArrival -> {
                            val activeImage = active.image
                            if (activeImage.isHidden) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                PixoraNetworkImage(
                                    imageUrl = activeImage.displayThumbnailUrl,
                                    contentDescription = activeImage.title,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeImage.isHidden) {
                                        stringResource(R.string.hidden_photo_banner_title)
                                    } else {
                                        stringResource(R.string.new_photo_banner_title, activeImage.category)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${activeImage.title} · ${activeImage.displayPrice}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { onOpenProfile(activeImage) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(text = stringResource(R.string.view_photo))
                            }
                        }

                        is InAppBannerEvent.ChatMessageArrival -> {
                            if (active.avatarUrl.isNotBlank()) {
                                PixoraNetworkImage(
                                    imageUrl = active.avatarUrl,
                                    contentDescription = active.title,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppGreen.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = null,
                                        tint = WhatsAppGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = active.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = active.preview,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = { onOpenChat(active.chatId, active.senderRoleForOpen) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(text = "Reply", color = WhatsAppGreen)
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
