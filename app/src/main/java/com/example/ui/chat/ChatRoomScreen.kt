package com.example.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.MessageTickState
import com.example.data.model.PresenceStatus
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.ui.theme.OnlineEmeraldDot
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ContactLeakFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WhatsAppBlueTick = Color(0xFF34B7F1)

@Composable
fun ChatRoomScreen(
    chat: ChatThread?,
    messages: List<ChatMessage>,
    senderRole: String, // "client", "staff", "admin"
    onSendMessage: (text: String, onError: (String) -> Unit, onSuccess: () -> Unit) -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onBack: () -> Unit,
    peerPresence: PresenceStatus? = null,
    onDeleteMessages: (messageIds: List<String>, remainingMessages: List<ChatMessage>) -> Unit = { _, _ -> },
    onDeleteChat: () -> Unit = {},
) {
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteMessagesDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (selectedMessageIds.isNotEmpty()) {
            selectedMessageIds = emptySet()
        } else {
            onBack()
        }
    }

    if (chat == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    var draft by remember { mutableStateOf("") }
    var leakWarning by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val headerTitle = when (senderRole) {
        "client" -> chat.profileName.ifBlank { "Companion" }
        else -> "${chat.clientName.ifBlank { "Client" }} ↔ ${chat.profileName}"
    }
    val headerAvatar = if (senderRole == "client") chat.profilePhotoUrl else chat.clientPhotoUrl.ifBlank { chat.profilePhotoUrl }
    val isPeerOnline = peerPresence?.isEffectivelyOnline() == true
    val roleLabel = when (senderRole) {
        "admin" -> "ADMIN CONCIERGE"
        "staff" -> "STAFF MONITORED"
        else -> "PRIVATE CHAT"
    }
    val presenceLabel = peerPresence?.formatPresenceLabel()
    val subtitleText = when {
        isPeerOnline -> "● Online"
        !presenceLabel.isNullOrBlank() && presenceLabel != "Offline" -> presenceLabel
        else -> roleLabel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Header (switches into WhatsApp selection mode when messages are long-pressed)
        Surface(
            color = if (selectedMessageIds.isNotEmpty()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (selectedMessageIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .testTag("chat_selection_action_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedMessageIds = emptySet() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear selection")
                    }
                    Text(
                        text = "${selectedMessageIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val selectedText = messages
                                .filter { selectedMessageIds.contains(it.id) }
                                .joinToString("\n") { it.text }
                            if (selectedText.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(selectedText))
                            }
                            selectedMessageIds = emptySet()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy selected messages")
                    }
                    IconButton(
                        onClick = { showDeleteMessagesDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("delete_selected_messages_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete selected messages",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_room_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }

                    Box {
                        PixoraNetworkImage(
                            imageUrl = headerAvatar,
                            contentDescription = headerTitle,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                        if (isPeerOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(OnlineEmeraldDot)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isPeerOnline) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isPeerOnline) WhatsAppGreen else MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("chat_peer_presence_label")
                        )
                    }

                    if (chat.profilePrice.isNotBlank()) {
                        PriceBadgePill(price = chat.profilePrice)
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = { showDeleteChatDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_room_delete_thread_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = onStartVoiceCall,
                        shape = CircleShape,
                        color = WhatsAppGreen.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(44.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("chat_room_call_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Voice Call",
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Staff Admin Monitoring & Anti-Leak Banner
        if (senderRole == "staff" || senderRole == "admin") {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (senderRole == "staff") {
                            stringResource(R.string.staff_monitor_notice)
                        } else {
                            "Admin Mode: You can view and reply directly in this ${chat.profileName} inquiry thread."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list")
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = when (senderRole) {
                    "client" -> msg.senderRole == "client"
                    "staff" -> msg.senderRole == "staff"
                    else -> msg.senderRole == "admin" || msg.senderRole == "staff"
                }
                val isSelected = selectedMessageIds.contains(msg.id)
                val tickState = if (isMine) {
                    chat.tickStateForOutgoingMessage(
                        message = msg,
                        viewerRole = senderRole,
                        peerOnline = isPeerOnline,
                    )
                } else {
                    null
                }
                MessageBubble(
                    message = msg,
                    isMine = isMine,
                    isSelected = isSelected,
                    tickState = tickState,
                    onClick = {
                        if (selectedMessageIds.isNotEmpty()) {
                            selectedMessageIds = if (isSelected) {
                                selectedMessageIds - msg.id
                            } else {
                                selectedMessageIds + msg.id
                            }
                        }
                    },
                    onLongPress = {
                        selectedMessageIds = if (isSelected) {
                            selectedMessageIds - msg.id
                        } else {
                            selectedMessageIds + msg.id
                        }
                    }
                )
            }
        }

        // Inline Anti-Contact-Leak Error Warning
        AnimatedVisibility(visible = !leakWarning.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("staff_contact_leak_warning")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = leakWarning.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Composer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    if (senderRole == "staff" && it.isNotBlank()) {
                        val check = ContactLeakFilter.validateForStaff(it)
                        leakWarning = if (!check.isAllowed) check.reason else null
                    } else {
                        leakWarning = null
                    }
                },
                placeholder = {
                    Text(
                        if (senderRole == "staff") {
                            "Reply safely (no phone/IG/email)…"
                        } else {
                            "Write a message…"
                        }
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_composer_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                onClick = {
                    val text = draft.trim()
                    if (text.isEmpty()) return@Surface
                    if (senderRole == "staff") {
                        val check = ContactLeakFilter.validateForStaff(text)
                        if (!check.isAllowed) {
                            leakWarning = check.reason
                            return@Surface
                        }
                    }
                    onSendMessage(
                        text,
                        { err -> leakWarning = err },
                        {
                            draft = ""
                            leakWarning = null
                        }
                    )
                },
                shape = CircleShape,
                color = WhatsAppGreen,
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("chat_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    if (showDeleteMessagesDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMessagesDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    if (selectedMessageIds.size == 1) "Delete message?"
                    else "Delete ${selectedMessageIds.size} messages?"
                )
            },
            text = { Text("Selected messages will be permanently removed from this conversation.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedMessageIds.toList()
                        val remaining = messages.filterNot { selectedMessageIds.contains(it.id) }
                        selectedMessageIds = emptySet()
                        showDeleteMessagesDialog = false
                        onDeleteMessages(toDelete, remaining)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_delete_messages_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMessagesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete this chat?") },
            text = { Text("This will delete the entire chat history with $headerTitle.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteChatDialog = false
                        onDeleteChat()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    modifier = Modifier.testTag("confirm_delete_room_chat_button")
                ) {
                    Text("Delete Chat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    isSelected: Boolean,
    tickState: MessageTickState?,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val containerColor = when {
        message.senderRole == "admin" -> MaterialTheme.colorScheme.secondaryContainer
        isMine -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        message.senderRole == "admin" -> MaterialTheme.colorScheme.onSecondaryContainer
        isMine -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val tagText = when (message.senderRole) {
        "admin" -> "${message.senderName.ifBlank { "Admin" }} · Concierge"
        "staff" -> "${message.senderName.ifBlank { "Companion" }} · Staff"
        else -> message.senderName.ifBlank { "Client" }
    }
    val formattedTime = remember(message.createdAt) {
        val ms = message.createdAt?.toDate()?.time
        if (ms != null) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ms)) else ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            )
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp,
            ),
            color = containerColor,
            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary) else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor.copy(alpha = 0.78f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (formattedTime.isNotBlank()) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.68f)
                        )
                    }
                    if (tickState != null) {
                        val tickIcon = when (tickState) {
                            MessageTickState.SENT -> Icons.Rounded.Done
                            MessageTickState.DELIVERED,
                            MessageTickState.READ -> Icons.Rounded.DoneAll
                        }
                        val tickTint = when (tickState) {
                            MessageTickState.READ -> WhatsAppBlueTick
                            MessageTickState.SENT,
                            MessageTickState.DELIVERED -> textColor.copy(alpha = 0.72f)
                        }
                        Icon(
                            imageVector = tickIcon,
                            contentDescription = tickState.name,
                            tint = tickTint,
                            modifier = Modifier
                                .size(15.dp)
                                .testTag("message_tick_${message.id}_${tickState.name}")
                        )
                    }
                }
            }
        }
    }
}
