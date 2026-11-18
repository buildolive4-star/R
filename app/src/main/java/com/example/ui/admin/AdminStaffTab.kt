package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatThread
import com.example.data.model.CompanionPrivateContact
import com.example.data.model.GalleryImage
import com.example.data.model.ProfileTier
import com.example.data.model.UserAccount
import com.example.ui.chat.ChatThreadCard
import com.example.ui.components.EmptyGalleryState
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill

@Composable
fun AdminStaffTab(
    pendingStaffUsers: List<UserAccount>,
    managedProfiles: List<GalleryImage>,
    allChats: List<ChatThread>,
    selectedProfileContact: Pair<String, CompanionPrivateContact>?,
    onApproveStaff: (UserAccount, tier: String) -> Unit,
    onRejectStaff: (userId: String) -> Unit,
    onLoadPrivateContact: (profileId: String) -> Unit,
    onRefreshProfile: (profileId: String) -> Unit,
    onDeleteProfile: (profileId: String) -> Unit,
    onOpenChatAsAdmin: (chatId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inspectedProfile by remember { mutableStateOf<GalleryImage?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_staff_tab")
    ) {
        item(key = "pending_header") {
            Text(
                text = "Pending Staff Requests (${pendingStaffUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (pendingStaffUsers.isEmpty()) {
            item(key = "pending_empty") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No pending staff applications right now. Users can apply after completing 100% of their profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(pendingStaffUsers, key = { it.uid }) { applicant ->
                PendingStaffCard(
                    applicant = applicant,
                    onApproveGrid = { onApproveStaff(applicant, ProfileTier.GRID_9_16) },
                    onApproveVip = { onApproveStaff(applicant, ProfileTier.VIP_16_9) },
                    onReject = { onRejectStaff(applicant.uid) }
                )
            }
        }

        item(key = "active_staff_header") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Verified Staff & Companion Directory (${managedProfiles.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap any profile to inspect full details, private phone/address & client chats.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (managedProfiles.isEmpty()) {
            item(key = "staff_profiles_empty") {
                EmptyGalleryState(
                    title = "No Companion Profiles Published",
                    subtitle = "Create a companion profile in the Create tab or verify a pending staff request above."
                )
            }
        } else {
            items(managedProfiles, key = { it.id }) { profile ->
                val relatedChatCount = allChats.count {
                    it.profileId == profile.id || (profile.ownerUid.isNotBlank() && it.staffOwnerUid == profile.ownerUid)
                }
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onLoadPrivateContact(profile.id)
                            inspectedProfile = profile
                        }
                        .testTag("admin_staff_card_${profile.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixoraNetworkImage(
                            imageUrl = profile.displayThumbnailUrl,
                            contentDescription = profile.title,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${profile.title}, ${profile.age}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = "${if (profile.isVip) "VIP 16:9" else "Grid 9:16"} · ${profile.role} · ${profile.allPhotoUrls.size} photo(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            PriceBadgePill(price = profile.displayPrice)
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BadgedBox(
                                    badge = {
                                        if (relatedChatCount > 0) {
                                            Badge { Text("$relatedChatCount") }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                                        contentDescription = "View client chats",
                                        tint = MaterialTheme.colorScheme.primary,
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

    val activeProfile = inspectedProfile
    if (activeProfile != null) {
        LaunchedEffect(activeProfile.id) {
            onLoadPrivateContact(activeProfile.id)
        }
        val staffChats = remember(allChats, activeProfile.id, activeProfile.ownerUid) {
            allChats.filter {
                it.profileId == activeProfile.id ||
                    (activeProfile.ownerUid.isNotBlank() && it.staffOwnerUid == activeProfile.ownerUid)
            }
        }
        val contact = if (selectedProfileContact?.first == activeProfile.id) selectedProfileContact.second else null
        StaffProfileInspectorSheet(
            profile = activeProfile,
            privateContact = contact,
            staffChats = staffChats,
            onRefresh = { onRefreshProfile(activeProfile.id) },
            onDelete = {
                inspectedProfile = null
                onDeleteProfile(activeProfile.id)
            },
            onOpenChat = { chatId ->
                inspectedProfile = null
                onOpenChatAsAdmin(chatId)
            },
            onDismiss = { inspectedProfile = null }
        )
    }
}

@Composable
private fun PendingStaffCard(
    applicant: UserAccount,
    onApproveGrid: () -> Unit,
    onApproveVip: () -> Unit,
    onReject: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_staff_card_${applicant.uid}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixoraNetworkImage(
                    imageUrl = applicant.photoUrl,
                    contentDescription = applicant.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${applicant.name} (${applicant.age}) · ${applicant.staffRole.ifBlank { "GF" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${applicant.phone} · ${applicant.city}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = applicant.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PriceBadgePill(price = applicant.staffPrice.ifBlank { "₹499" })
            }

            if (applicant.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = applicant.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApproveGrid,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve 9:16", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onApproveVip,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve VIP", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onReject,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffProfileInspectorSheet(
    profile: GalleryImage,
    privateContact: CompanionPrivateContact?,
    staffChats: List<ChatThread>,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showChatsSection by remember { mutableStateOf(false) }

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
                .testTag("staff_inspector_sheet")
        ) {
            // Top bar with prominent Top Chat Icon for inspecting staff-client conversations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${profile.title}, ${profile.age}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${profile.role} · ${profile.displayPrice} · ${if (profile.isVip) "VIP 16:9" else "Grid 9:16"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showChatsSection = !showChatsSection },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(
                            if (showChatsSection) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                        .testTag("staff_inspector_top_chat_icon")
                ) {
                    BadgedBox(
                        badge = {
                            if (staffChats.isNotEmpty()) {
                                Badge { Text("${staffChats.size}") }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = "View Staff Client Chats",
                            tint = if (showChatsSection) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (showChatsSection) {
                Text(
                    text = "Staff Client Discussions (${staffChats.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Monitor messages exchanged with every client and reply directly as Admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (staffChats.isEmpty()) {
                    Text(
                        text = "No client conversations for this profile yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        staffChats.forEach { thread ->
                            ChatThreadCard(
                                title = thread.clientName.ifBlank { "Client" },
                                subtitle = thread.lastMessage,
                                photoUrl = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                                priceBadge = thread.profilePrice,
                                unreadCount = thread.unreadForAdmin,
                                onClick = { onOpenChat(thread.id) },
                                modifier = Modifier.testTag("staff_inspector_chat_${thread.id}")
                            )
                        }
                    }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(profile.allPhotoUrls) { photoUrl ->
                        PixoraNetworkImage(
                            imageUrl = photoUrl,
                            contentDescription = profile.title,
                            modifier = Modifier
                                .size(width = 120.dp, height = 165.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (profile.description.isNotBlank()) {
                    Text(
                        text = profile.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Admin-Only Private PII Box
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Private Contact (Admin-Only)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mobile No.: ${privateContact?.phone?.ifBlank { "Not stored" } ?: "Loading / Local profile"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Address: ${privateContact?.address?.ifBlank { "Not stored" } ?: "-"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (profile.isHidden) {
                        FilledTonalButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh 40s")
                        }
                    }
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Profile")
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
