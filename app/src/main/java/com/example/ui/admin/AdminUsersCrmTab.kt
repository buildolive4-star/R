package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminUserFilter
import com.example.data.model.ChatThread
import com.example.data.model.LeadTag
import com.example.data.model.ModerationStatus
import com.example.data.model.UserAccount
import com.example.ui.chat.ChatThreadCard
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.theme.BlockedAccountColor
import com.example.ui.theme.ColdLeadColor
import com.example.ui.theme.HotLeadColor
import com.example.ui.theme.OnlineEmeraldDot
import com.example.ui.theme.SuspendedAccountColor
import com.example.ui.theme.WarmLeadColor
import com.example.ui.theme.WhatsAppGreen

@Composable
fun AdminUsersCrmTab(
    pagedUsers: List<UserAccount>,
    totalFilteredCount: Int,
    hasMoreUsers: Boolean,
    selectedFilter: AdminUserFilter,
    allChats: List<ChatThread>,
    onSelectFilter: (AdminUserFilter) -> Unit,
    onLoadNextPage: () -> Unit,
    onSetLeadTag: (userId: String, leadTag: String) -> Unit,
    onSetModeration: (userId: String, status: String) -> Unit,
    onOpenChatAsAdmin: (chatId: String) -> Unit,
    onCallUser: (UserAccount, ChatThread?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var inspectedUser by remember { mutableStateOf<UserAccount?>(null) }
    val gridState = rememberLazyGridState()

    val reachedBottom by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }

    LaunchedEffect(reachedBottom, hasMoreUsers) {
        if (reachedBottom && hasMoreUsers) {
            onLoadNextPage()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminUserFilter.entries, key = { it.name }) { filter ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(text = filter.label, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("crm_filter_${filter.name}")
                )
            }
        }

        Text(
            text = "Showing ${pagedUsers.size} of $totalFilteredCount users · 5×6 Grid",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (pagedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyGalleryState(
                    title = "No Matching Users",
                    subtitle = "Switch filter above or wait for users to sign in and send an inquiry."
                )
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("admin_crm_5col_grid")
            ) {
                items(pagedUsers, key = { it.uid }) { user ->
                    UserCrmMiniTile(
                        user = user,
                        onClick = { inspectedUser = user }
                    )
                }

                if (hasMoreUsers) {
                    item(span = { GridItemSpan(5) }, key = "crm_load_more") {
                        OutlinedButton(
                            onClick = onLoadNextPage,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("crm_load_more_button")
                        ) {
                            Text("Load Next 30 Users")
                        }
                    }
                }
            }
        }
    }

    val activeUser = inspectedUser?.let { selected ->
        pagedUsers.firstOrNull { it.uid == selected.uid } ?: selected
    }
    if (activeUser != null) {
        val userInquiries = remember(allChats, activeUser.uid) {
            allChats.filter { it.clientUid == activeUser.uid }
        }
        UserCrmInspectorSheet(
            user = activeUser,
            userChats = userInquiries,
            onSetLeadTag = { tag -> onSetLeadTag(activeUser.uid, tag) },
            onSetModeration = { status -> onSetModeration(activeUser.uid, status) },
            onCallUser = {
                inspectedUser = null
                onCallUser(activeUser, userInquiries.firstOrNull())
            },
            onOpenChat = { chatId ->
                inspectedUser = null
                onOpenChatAsAdmin(chatId)
            },
            onDismiss = { inspectedUser = null }
        )
    }
}

@Composable
private fun UserCrmMiniTile(
    user: UserAccount,
    onClick: () -> Unit,
) {
    val tileTint = when {
        user.moderationStatus == ModerationStatus.BLOCKED -> BlockedAccountColor
        user.moderationStatus == ModerationStatus.SUSPENDED -> SuspendedAccountColor
        user.leadTag == LeadTag.HOT -> HotLeadColor
        user.leadTag == LeadTag.WARM -> WarmLeadColor
        user.leadTag == LeadTag.COLD -> ColdLeadColor
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val containerBg = when {
        user.moderationStatus == ModerationStatus.BLOCKED -> BlockedAccountColor.copy(alpha = 0.24f)
        user.moderationStatus == ModerationStatus.SUSPENDED -> SuspendedAccountColor.copy(alpha = 0.22f)
        user.leadTag == LeadTag.HOT -> HotLeadColor.copy(alpha = 0.16f)
        user.leadTag == LeadTag.WARM -> WarmLeadColor.copy(alpha = 0.16f)
        user.leadTag == LeadTag.COLD -> ColdLeadColor.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerBg,
        border = BorderStroke(1.2.dp, tileTint.copy(alpha = 0.78f)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("crm_user_tile_${user.uid}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(36.dp)) {
                if (user.photoUrl.isNotBlank()) {
                    PixoraNetworkImage(
                        imageUrl = user.photoUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.trim().firstOrNull()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (user.isRecentlyOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(OnlineEmeraldDot)
                    )
                }

                if (user.hasInquiryNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = user.name.ifBlank { "User" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            val miniTag = when {
                user.moderationStatus != ModerationStatus.ACTIVE -> user.moderationStatus.take(4).uppercase()
                user.leadTag != LeadTag.NONE -> user.leadTag.uppercase()
                else -> "${user.completionPercent}%"
            }
            Text(
                text = miniTag,
                style = MaterialTheme.typography.labelSmall,
                color = if (tileTint == MaterialTheme.colorScheme.outlineVariant) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    tileTint
                },
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun UserCrmInspectorSheet(
    user: UserAccount,
    userChats: List<ChatThread>,
    onSetLeadTag: (String) -> Unit,
    onSetModeration: (String) -> Unit,
    onCallUser: () -> Unit,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("crm_user_inspector_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    PixoraNetworkImage(
                        imageUrl = user.photoUrl,
                        contentDescription = user.name,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${user.name.ifBlank { "Member" }} (${user.age})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.isRecentlyOnline) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(OnlineEmeraldDot)
                                )
                            }
                        }
                        Text(
                            text = user.email.ifBlank { user.uid },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Private PII Card (Admin only)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Phone: ${user.phone.ifBlank { "Not provided" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Address: ${user.address.ifBlank { "Not provided" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "City: ${user.city.ifBlank { "-" }} · Profile ${user.completionPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (user.bio.isNotBlank()) {
                        Text(
                            text = "Bio: ${user.bio}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onCallUser,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("crm_voice_call_button")
            ) {
                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("WebRTC Voice Call ${user.name.ifBlank { "Member" }}", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Lead Color Classification",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LeadChip("Hot Lead", LeadTag.HOT, HotLeadColor, user.leadTag == LeadTag.HOT) {
                    onSetLeadTag(LeadTag.HOT)
                }
                LeadChip("Warm Lead", LeadTag.WARM, WarmLeadColor, user.leadTag == LeadTag.WARM) {
                    onSetLeadTag(LeadTag.WARM)
                }
                LeadChip("Cold Lead", LeadTag.COLD, ColdLeadColor, user.leadTag == LeadTag.COLD) {
                    onSetLeadTag(LeadTag.COLD)
                }
                LeadChip("None", LeadTag.NONE, MaterialTheme.colorScheme.outline, user.leadTag == LeadTag.NONE) {
                    onSetLeadTag(LeadTag.NONE)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Account Moderation",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onSetModeration(ModerationStatus.ACTIVE) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (user.moderationStatus == ModerationStatus.ACTIVE) {
                            OnlineEmeraldDot.copy(alpha = 0.24f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Active", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { onSetModeration(ModerationStatus.SUSPENDED) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (user.moderationStatus == ModerationStatus.SUSPENDED) {
                            SuspendedAccountColor.copy(alpha = 0.35f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PauseCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suspend", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = { onSetModeration(ModerationStatus.BLOCKED) },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (user.moderationStatus == ModerationStatus.BLOCKED) {
                            BlockedAccountColor.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block", maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Inquired Profiles & Replies (${userChats.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (userChats.isEmpty()) {
                Text(
                    text = "No profile inquiries from this user yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    userChats.forEach { thread ->
                        ChatThreadCard(
                            title = thread.profileName,
                            subtitle = thread.lastMessage,
                            photoUrl = thread.profilePhotoUrl,
                            priceBadge = thread.profilePrice,
                            unreadCount = thread.unreadForAdmin,
                            onClick = { onOpenChat(thread.id) },
                            modifier = Modifier.testTag("crm_user_chat_${thread.id}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}

@Composable
private fun LeadChip(
    label: String,
    tag: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White,
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("lead_chip_$tag")
    )
}
