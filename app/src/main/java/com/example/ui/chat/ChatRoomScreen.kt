package com.example.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.ui.components.PixoraNetworkImage
import com.example.ui.components.PriceBadgePill
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ContactLeakFilter

@Composable
fun ChatRoomScreen(
    chat: ChatThread?,
    messages: List<ChatMessage>,
    senderRole: String, // "client", "staff", "admin"
    onSendMessage: (text: String, onError: (String) -> Unit, onSuccess: () -> Unit) -> Unit,
    onStartVoiceCall: () -> Unit = {},
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    if (chat == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

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
    val roleLabel = when (senderRole) {
        "admin" -> "ADMIN CONCIERGE"
        "staff" -> "STAFF MONITORED"
        else -> "PRIVATE CHAT"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
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

                PixoraNetworkImage(
                    imageUrl = headerAvatar,
                    contentDescription = headerTitle,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

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
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (chat.profilePrice.isNotBlank()) {
                    PriceBadgePill(price = chat.profilePrice)
                    Spacer(modifier = Modifier.width(6.dp))
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
                MessageBubble(
                    message = msg,
                    isMine = isMine,
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
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
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

    Row(
        modifier = Modifier.fillMaxWidth(),
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
            }
        }
    }
}
