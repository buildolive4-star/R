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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
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

private val SMART_ICEBREAKERS = listOf(
    "☕ Coffee & Conversation today?",
    "🎶 Up for live acoustic music?",
    "🌅 Evening walk & street food?",
    "🎨 Art gallery visit this weekend?",
    "✨ Hey! Loved your profile vibe",
)

private val DATE_PLANNER_OPTIONS = listOf(
    "☕ Specialty Coffee & Book Cafe · Today 5:00 PM",
    "🍽️ Rooftop Fine Dining & Sunset · Tonight 8:00 PM",
    "🎨 Art Gallery & Heritage Walk · Saturday 4:00 PM",
    "🎶 Live Indie Music & Mocktails · Friday 7:30 PM",
    "🎬 Cinema Premiere & Dessert · Sunday 6:00 PM",
    "🎧 Virtual Coffee & Voice Hangout · In 30 Mins",
)

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
    var showDatePlannerDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Pinned Companion Profile Top Header
                Surface(
                    color = if (selectedMessageIds.isNotEmpty()) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp,
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
                                        .size(42.dp)
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
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (senderRole == "staff") {
                                    stringResource(R.string.staff_monitor_notice)
                                } else {
                                    "Admin Mode: Direct live reply active for ${chat.profileName}."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Bottom Bar right above navigation bar / keyboard
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
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

                    // Quick Companionship Actions: Date Planner, 3D Virtual Rose Gift & Smart Icebreakers
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "plan_date_btn") {
                            Surface(
                                onClick = { showDatePlannerDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Plan Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE91E63)
                                    )
                                }
                            }
                        }

                        item(key = "send_rose_btn") {
                            Surface(
                                onClick = {
                                    onSendMessage(
                                        "🌹 Sent a 3D Velvet Rose to ${chat.profileName.ifBlank { "you" }}!",
                                        { err -> leakWarning = err },
                                        {}
                                    )
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = Color(0xFFFFF0F5),
                                border = BorderStroke(1.dp, Color(0xFFFF6F9C).copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🌹 Send Rose",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE91E63)
                                    )
                                }
                            }
                        }

                        items(SMART_ICEBREAKERS, key = { it }) { icebreaker ->
                            Surface(
                                onClick = { draft = icebreaker },
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = icebreaker,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Composer Row (Docked directly above footer)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 2.dp),
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
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("chat_send_button")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFE91E63), Color(0xFFFF6F9C))
                                    )
                                )
                            ) {
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
            }
        }
    ) { innerPadding ->
        // Scrollable Message List strictly between pinned topBar and pinned bottomBar
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
    }

    if (showDatePlannerDialog) {
        AlertDialog(
            onDismissRequest = { showDatePlannerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFE91E63)
                )
            },
            title = {
                Text(
                    text = "Plan a DateMate Hangout",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Select an experience to invite ${chat.profileName.ifBlank { "your companion" }}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DATE_PLANNER_OPTIONS.forEach { option ->
                        Surface(
                            onClick = {
                                showDatePlannerDialog = false
                                onSendMessage(
                                    "📅 DateMate Hangout Invite: $option",
                                    { err -> leakWarning = err },
                                    {}
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF0F5),
                            border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF25212A),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDatePlannerDialog = false }) {
                    Text("Close")
                }
            }
        )
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
        message.senderRole == "admin" -> Color(0xFFFFE4EE)
        isMine -> Color(0xFFE91E63)
        else -> Color(0xFFFFF0F5)
    }
    val textColor = when {
        message.senderRole == "admin" -> Color(0xFF25212A)
        isMine -> Color.White
        else -> Color(0xFF25212A)
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
            .clip(RoundedCornerShape(14.dp))
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
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMine) 20.dp else 5.dp,
                bottomEnd = if (isMine) 5.dp else 20.dp,
            ),
            color = containerColor,
            shadowElevation = if (isMine) 4.dp else 1.dp,
            border = if (isSelected) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
            } else if (!isMine) {
                BorderStroke(1.dp, Color(0xFFF6DCE6))
            } else null,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (isMine && message.senderRole != "admin") {
                            Modifier.background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color(0xFFE91E63), Color(0xFFFF6F9C))
                                )
                            )
                        } else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
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
