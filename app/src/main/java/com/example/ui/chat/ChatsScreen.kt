package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatThread
import com.example.data.model.PresenceStatus
import com.example.data.model.StaffStatus
import com.example.data.model.UserAccount
import com.example.data.repository.UserCrmRepository
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.ui.theme.OnlineEmeraldDot
import com.example.ui.theme.WhatsAppGreen

private enum class ChatsSubTab {
    MY_INQUIRIES,
    STAFF_INBOX,
}

@Composable
fun ChatsScreen(
    clientChats: List<ChatThread>,
    staffInboxChats: List<ChatThread>,
    userAccount: UserAccount?,
    onOpenChat: (chatId: String, senderRole: String) -> Unit,
    onGoToProfile: () -> Unit,
    modifier: Modifier = Modifier,
    peerPresences: Map<String, PresenceStatus> = emptyMap(),
    onDeleteChat: (chatId: String) -> Unit = {},
) {
    val isApprovedStaff = userAccount?.isApprovedStaff == true
    var selectedTab by remember { mutableStateOf(ChatsSubTab.MY_INQUIRIES) }
    var pendingDeleteTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.chats_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.chats_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isApprovedStaff) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "STAFF INBOX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == ChatsSubTab.MY_INQUIRIES,
                    onClick = { selectedTab = ChatsSubTab.MY_INQUIRIES },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("My Inquiries (${clientChats.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("tab_my_inquiries")
                )

                FilterChip(
                    selected = selectedTab == ChatsSubTab.STAFF_INBOX,
                    onClick = { selectedTab = ChatsSubTab.STAFF_INBOX },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isApprovedStaff) Icons.Rounded.Verified else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (isApprovedStaff) "Staff Inbox (${staffInboxChats.size})" else "Staff Inbox",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("tab_staff_inbox")
                )
            }
        }

        when (selectedTab) {
            ChatsSubTab.MY_INQUIRIES -> {
                if (clientChats.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyGalleryState(
                            title = stringResource(R.string.chats_empty_title),
                            subtitle = stringResource(R.string.chats_empty_subtitle),
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("client_chats_list")
                    ) {
                        items(clientChats, key = { it.id }) { thread ->
                            val peerKey = thread.staffOwnerUid.ifBlank { UserCrmRepository.ADMIN_PRESENCE_ID }
                            val isOnline = peerPresences[peerKey]?.isEffectivelyOnline() == true
                            ChatThreadCard(
                                title = thread.profileName,
                                subtitle = thread.lastMessage.ifBlank { "Tap to message" },
                                photoUrl = thread.profilePhotoUrl,
                                priceBadge = thread.profilePrice,
                                unreadCount = thread.unreadForClient,
                                onClick = { onOpenChat(thread.id, "client") },
                                isPeerOnline = isOnline,
                                onLongPress = { pendingDeleteTarget = thread.id to thread.profileName.ifBlank { "this chat" } },
                                modifier = Modifier.testTag("client_chat_row_${thread.id}")
                            )
                        }
                    }
                }
            }

            ChatsSubTab.STAFF_INBOX -> {
                if (!isApprovedStaff) {
                    StaffInboxLockedCard(
                        staffStatus = userAccount?.staffStatus ?: StaffStatus.NONE,
                        onGoToProfile = onGoToProfile,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("staff_inbox_list")
                    ) {
                        item(key = "staff_monitor_notice") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_staff_verified_banner),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .alpha(0.32f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color(0xD80A120E),
                                                        Color(0xAA0D1914),
                                                    )
                                                )
                                            )
                                    )
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shield,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Verified Companion Lounge",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = stringResource(R.string.staff_monitor_notice),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (staffInboxChats.isEmpty()) {
                            item(key = "staff_empty") {
                                EmptyGalleryState(
                                    title = "No Client Messages Yet",
                                    subtitle = "When users message your verified profile, their inquiries will appear right here."
                                )
                            }
                        } else {
                            items(staffInboxChats, key = { it.id }) { thread ->
                                val isOnline = peerPresences[thread.clientUid]?.isEffectivelyOnline() == true
                                ChatThreadCard(
                                    title = thread.clientName.ifBlank { "Client" },
                                    subtitle = thread.lastMessage.ifBlank { "New client inquiry" },
                                    photoUrl = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                                    priceBadge = thread.profilePrice,
                                    unreadCount = thread.unreadForStaff,
                                    onClick = { onOpenChat(thread.id, "staff") },
                                    isPeerOnline = isOnline,
                                    onLongPress = { pendingDeleteTarget = thread.id to thread.clientName.ifBlank { "this chat" } },
                                    modifier = Modifier.testTag("staff_chat_row_${thread.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val deleteTarget = pendingDeleteTarget
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete chat with ${deleteTarget.second}?") },
            text = { Text("This deletes the entire conversation and all messages in this thread.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteTarget = null
                        onDeleteChat(deleteTarget.first)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_delete_chat_dialog_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatThreadCard(
    title: String,
    subtitle: String,
    photoUrl: String,
    priceBadge: String,
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPeerOnline: Boolean = false,
    onLongPress: (() -> Unit)? = null,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                PixoraNetworkImage(
                    imageUrl = photoUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isPeerOnline) OnlineEmeraldDot else WhatsAppGreen)
                        .border(1.5.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!isPeerOnline) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (priceBadge.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        PriceBadgePill(price = priceBadge)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onLongPress != null) {
                IconButton(
                    onClick = onLongPress,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = WhatsAppGreen,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = unreadCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffInboxLockedCard(
    staffStatus: String,
    onGoToProfile: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("staff_inbox_locked_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (staffStatus == StaffStatus.PENDING) {
                                Icons.Rounded.NotificationsActive
                            } else {
                                Icons.Rounded.Lock
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (staffStatus == StaffStatus.PENDING) {
                        "Join DateMate Verification Pending"
                    } else {
                        stringResource(R.string.staff_inbox_locked_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (staffStatus == StaffStatus.PENDING) {
                        "DateMate Admin is verifying your info & chat approval. Once approved, Staff Mode and direct client chat unlock here automatically."
                    } else {
                        stringResource(R.string.staff_inbox_locked_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onGoToProfile,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("staff_locked_go_profile_button")
                ) {
                    Text(
                        text = if (staffStatus == StaffStatus.PENDING) {
                            "View Join DateMate Status"
                        } else {
                            "Join DateMate in Profile"
                        }
                    )
                }
            }
        }
    }
}
