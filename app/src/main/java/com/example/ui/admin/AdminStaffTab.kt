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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
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

private val EDIT_PRICE_PRESETS = listOf("₹299", "₹499", "₹799", "₹1,499", "₹2,999")
private val EDIT_CATEGORIES = listOf("Companion", "Date Night", "Dinner", "Travel", "VIP", "Verified Staff")

@Composable
fun AdminStaffTab(
    pendingStaffUsers: List<UserAccount>,
    managedProfiles: List<GalleryImage>,
    allChats: List<ChatThread>,
    selectedProfileContact: Pair<String, CompanionPrivateContact>?,
    onApproveStaff: (UserAccount, tier: String) -> Unit,
    onRejectStaff: (userId: String) -> Unit,
    onLoadPrivateContact: (profileId: String) -> Unit,
    onUpdateProfile: (
        profileId: String,
        name: String,
        ageText: String,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        phone: String?,
        address: String?,
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    onRefreshProfile: (profileId: String) -> Unit,
    onDeleteProfile: (profileId: String) -> Unit,
    onOpenChatAsAdmin: (chatId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var inspectedProfileId by remember { mutableStateOf<String?>(null) }

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
                val sanitizedId = profile.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
                val relatedChats = allChats.filter {
                    it.profileId == profile.id ||
                        it.profileId == sanitizedId ||
                        it.profileName.equals(profile.title, ignoreCase = true) ||
                        (profile.ownerUid.isNotBlank() && it.staffOwnerUid == profile.ownerUid)
                }
                val relatedChatCount = relatedChats.size
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onLoadPrivateContact(profile.id)
                            inspectedProfileId = profile.id
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
                                text = "${if (profile.isVip) "VIP 16:9" else "Grid 9:16"} · ${profile.role} · Direct Chat Approved ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PriceBadgePill(price = profile.displayPrice)
                                if (relatedChatCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "$relatedChatCount Chat(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = {
                                if (relatedChats.size == 1) {
                                    onOpenChatAsAdmin(relatedChats.first().id)
                                } else {
                                    onLoadPrivateContact(profile.id)
                                    inspectedProfileId = profile.id
                                }
                            },
                            shape = CircleShape,
                            color = if (relatedChatCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(42.dp)
                                .testTag("admin_staff_chat_btn_${profile.id}")
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
                                        contentDescription = "Open client chats",
                                        tint = if (relatedChatCount > 0) Color.White else MaterialTheme.colorScheme.primary,
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

    val activeProfile = inspectedProfileId?.let { id -> managedProfiles.firstOrNull { it.id == id } }
    if (activeProfile != null) {
        LaunchedEffect(activeProfile.id) {
            onLoadPrivateContact(activeProfile.id)
        }
        val staffChats = remember(allChats, activeProfile.id, activeProfile.title, activeProfile.ownerUid) {
            val sanitizedId = activeProfile.id.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
            allChats.filter {
                it.profileId == activeProfile.id ||
                    it.profileId == sanitizedId ||
                    it.profileName.equals(activeProfile.title, ignoreCase = true) ||
                    (activeProfile.ownerUid.isNotBlank() && it.staffOwnerUid == activeProfile.ownerUid)
            }
        }
        val contact = if (selectedProfileContact?.first == activeProfile.id) selectedProfileContact.second else null
        StaffProfileInspectorSheet(
            profile = activeProfile,
            privateContact = contact,
            staffChats = staffChats,
            allChats = allChats,
            onSaveEdits = { name, ageText, price, role, category, tier, bio, hiddenTimed, phone, address ->
                onUpdateProfile(
                    activeProfile.id,
                    name,
                    ageText,
                    price,
                    role,
                    category,
                    tier,
                    bio,
                    hiddenTimed,
                    phone,
                    address,
                )
            },
            onRefresh = { onRefreshProfile(activeProfile.id) },
            onDelete = {
                inspectedProfileId = null
                onDeleteProfile(activeProfile.id)
            },
            onOpenChat = { chatId ->
                inspectedProfileId = null
                onOpenChatAsAdmin(chatId)
            },
            onDismiss = { inspectedProfileId = null }
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

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Approving verifies user info, activates Staff Mode, and enables Direct Staff Chat.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))
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
                    Text("Verify & Chat (9:16)", maxLines = 1)
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
                    Text("Verify VIP", maxLines = 1)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun StaffProfileInspectorSheet(
    profile: GalleryImage,
    privateContact: CompanionPrivateContact?,
    staffChats: List<ChatThread>,
    allChats: List<ChatThread>,
    onSaveEdits: (
        name: String,
        ageText: String,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        phone: String?,
        address: String?,
    ) -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onOpenChat: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showChatsSection by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }

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
                        text = "${profile.role} · ${profile.displayPrice} · ${if (profile.isVip) "VIP 16:9" else "Grid 9:16"} · Direct Chat Enabled",
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

            // Always show Client Chats right at the top when showChatsSection is active (default true!)
            if (showChatsSection) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Client Chat Conversations (${staffChats.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap any client conversation below to open the chat room and reply directly as Admin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        if (staffChats.isEmpty()) {
                            Text(
                                text = "No client conversations specifically linked to ${profile.title} yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (allChats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Recent Client Messages Across DateMate (${allChats.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allChats.take(5).forEach { thread ->
                                        ChatThreadCard(
                                            title = "${thread.clientName.ifBlank { "Client" }} → ${thread.profileName}",
                                            subtitle = thread.lastMessage,
                                            photoUrl = thread.clientPhotoUrl.ifBlank { thread.profilePhotoUrl },
                                            priceBadge = thread.profilePrice,
                                            unreadCount = thread.unreadForAdmin,
                                            onClick = { onOpenChat(thread.id) },
                                            modifier = Modifier.testTag("staff_inspector_any_chat_${thread.id}")
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                staffChats.forEach { thread ->
                                    ChatThreadCard(
                                        title = "${thread.clientName.ifBlank { "Client" }} → ${thread.profileName.ifBlank { profile.title }}",
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
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_edit_price_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Price & Info", maxLines = 1)
                    }
                    if (profile.isHidden) {
                        FilledTonalButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("40s", maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove Profile")
                }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }

    if (showEditDialog) {
        EditProfilePriceDialog(
            profile = profile,
            privateContact = privateContact,
            onSave = { name, ageText, price, role, category, tier, bio, hiddenTimed, phone, address ->
                showEditDialog = false
                onSaveEdits(name, ageText, price, role, category, tier, bio, hiddenTimed, phone, address)
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfilePriceDialog(
    profile: GalleryImage,
    privateContact: CompanionPrivateContact?,
    onSave: (
        name: String,
        ageText: String,
        price: String,
        role: String,
        category: String,
        tier: String,
        bio: String,
        hiddenTimed: Boolean,
        phone: String?,
        address: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(profile.id) { mutableStateOf(profile.title) }
    var ageText by remember(profile.id) { mutableStateOf(profile.age.toString()) }
    var price by remember(profile.id) { mutableStateOf(profile.price) }
    var role by remember(profile.id) { mutableStateOf(profile.role.ifBlank { "GF" }) }
    var tier by remember(profile.id) { mutableStateOf(profile.tier.ifBlank { ProfileTier.GRID_9_16 }) }
    var category by remember(profile.id) { mutableStateOf(profile.category.ifBlank { "Companion" }) }
    var bio by remember(profile.id) { mutableStateOf(profile.description) }
    var hiddenTimed by remember(profile.id) { mutableStateOf(profile.isHidden) }
    var phone by remember(profile.id, privateContact) { mutableStateOf(privateContact?.phone.orEmpty()) }
    var address by remember(profile.id, privateContact) { mutableStateOf(privateContact?.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Price & Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    singleLine = true,
                    label = { Text("Manual Custom Price (e.g. ₹999)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_price_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EDIT_PRICE_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = price == preset,
                            onClick = { price = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        label = { Text("Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(2) },
                        singleLine = true,
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.75f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("GF", "BF").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tier == ProfileTier.GRID_9_16,
                        onClick = { tier = ProfileTier.GRID_9_16 },
                        label = { Text("Grid 9:16") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = tier == ProfileTier.VIP_16_9,
                        onClick = { tier = ProfileTier.VIP_16_9 },
                        label = { Text("VIP 16:9") },
                        modifier = Modifier.weight(1f)
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EDIT_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    minLines = 2,
                    maxLines = 4,
                    label = { Text("Bio / Vibes") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    singleLine = true,
                    label = { Text("Mobile No. (Private)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    singleLine = true,
                    label = { Text("Address (Private)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("40-Sec Hidden Mode", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = hiddenTimed, onCheckedChange = { hiddenTimed = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        ageText,
                        price,
                        role,
                        category,
                        tier,
                        bio,
                        hiddenTimed,
                        phone.takeIf { it.isNotBlank() || privateContact != null },
                        address.takeIf { it.isNotBlank() || privateContact != null },
                    )
                },
                modifier = Modifier.testTag("save_profile_edits_button")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
