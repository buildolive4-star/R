package com.example.ui.datemate

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.DateMateCompanionProfile
import com.example.data.model.DateMateChatThread
import com.example.data.model.DateMateMessage
import com.example.ui.theme.DateMateBackground
import com.example.ui.theme.DateMateDarkText
import com.example.ui.theme.DateMatePrimaryPink
import com.example.ui.theme.DateMateSecondaryPink
import com.example.ui.theme.DateMateSecondaryText
import com.example.ui.theme.DateMateSoftBlush
import com.example.ui.theme.DateMateSoftBorder
import com.example.ui.theme.DateMateSuccessGreen
import com.example.ui.theme.DateMateWhite

// ============================================================================
// 10. CHAT LIST & REAL-TIME CHAT ROOM SCREENS
// Theme: White + Blush Pink (#FFF0F5) + Rose Pink (#E91E63 -> #FF6F9C)
// ============================================================================
@Composable
fun DateMateChatListScreen(
    threads: List<DateMateChatThread>,
    companions: List<DateMateCompanionProfile>,
    onOpenThread: (DateMateChatThread) -> Unit,
    onStartChatWithCompanion: (DateMateCompanionProfile) -> Unit,
    onDeleteThread: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateSoftBlushVerticalGradient)
            .testTag("chat_list_screen")
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp)
            ) {
                Text(
                    text = "Conversations",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = DateMateDarkText,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Text(
                    text = "Real-time messaging, voice notes & HD calls",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = DateMateSecondaryText
                    )
                )
            }
        }

        // Active Online Matches Horizontal Strip
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Online Companions",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = DateMatePrimaryPink,
                        fontWeight = FontWeight.Bold
                    )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    companions.forEach { companion ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable { onStartChatWithCompanion(companion) }
                        ) {
                            Box {
                                AsyncImage(
                                    model = companion.primaryPhotoUrl,
                                    contentDescription = companion.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, DateMatePrimaryPink, CircleShape)
                                )
                                if (companion.isOnline) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(DateMateSuccessGreen)
                                            .border(2.dp, DateMateWhite, CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = companion.name.substringBefore(" "),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }

        if (threads.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = DateMateWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .border(1.dp, DateMateSoftBorder, RoundedCornerShape(26.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(28.dp)
                    ) {
                        Animated3DHeartLogo(sizeDp = 74.dp, showDmMonogram = false)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Conversations Yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap any online companion above or swipe right on Discover to start chatting.",
                            style = MaterialTheme.typography.bodySmall.copy(color = DateMateSecondaryText),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(threads, key = { it.id }) { thread ->
                ChatThreadRowCard(
                    thread = thread,
                    onClick = { onOpenThread(thread) },
                    onDelete = { onDeleteThread(thread.id) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatThreadRowCard(
    thread: DateMateChatThread,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DateMateWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DateMateSoftBorder, RoundedCornerShape(24.dp))
            .bouncy3DPress(onClick = onClick)
            .testTag("chat_thread_${thread.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box {
                AsyncImage(
                    model = thread.companionPhotoUrl,
                    contentDescription = thread.companionName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(2.dp, DateMatePrimaryPink, CircleShape)
                )
                if (thread.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(DateMateSuccessGreen)
                            .border(2.dp, DateMateWhite, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = thread.companionName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (thread.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Rounded.Verified,
                                contentDescription = "Verified",
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Text(
                        text = thread.lastUpdatedLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (thread.unreadCount > 0) DateMatePrimaryPink else DateMateSecondaryText,
                            fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (thread.isTyping) "Typing a message…" else thread.lastMessage,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (thread.isTyping || thread.unreadCount > 0) DateMatePrimaryPink else DateMateSecondaryText,
                            fontWeight = if (thread.unreadCount > 0 || thread.isTyping) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(DateMatePrimaryPink)
                        ) {
                            Text(
                                text = "${thread.unreadCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = DateMateWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete conversation",
                                tint = DateMateSecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// REAL-TIME CHAT ROOM SCREEN
// Header: Profile photo, Name, Online status, Audio Call, Video Call, More Options
// Incoming: Soft blush pink (#FFF0F5)
// Outgoing: Rose pink gradient (#E91E63 -> #FF6F9C)
// Features: Image sharing, Voice messages, Emoji picker, Message reactions,
// Floating heart reactions, Typing indicator, Read receipts, Reply, Delete, Block/Report
// ============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateMateChatRoomScreen(
    thread: DateMateChatThread,
    messages: List<DateMateMessage>,
    onBack: () -> Unit,
    onSendMessage: (text: String, imageUrl: String, voiceDurationSec: Int, replyToPreview: String) -> Unit,
    onToggleReaction: (messageId: String, emoji: String) -> Unit,
    onDeleteMessage: (messageId: String) -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onBookDateWithCompanion: () -> Unit,
    onBlockCompanion: () -> Unit,
    onReportCompanion: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var inputText by rememberSaveable { mutableStateOf("") }
    var replyToMessage by remember { mutableStateOf<DateMateMessage?>(null) }
    var showEmojiBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val statusTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onSendMessage(
                    "Shared a special moment with you ✨",
                    uri.toString(),
                    0,
                    replyToMessage?.text.orEmpty()
                )
                replyToMessage = null
            }
        }
    )

    LaunchedEffect(messages.size, thread.isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DateMateBackground)
            .imePadding()
            .testTag("chat_room_screen")
    ) {
        // Chat Header
        Surface(
            color = DateMateWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = (statusTopPadding + 8.dp).coerceAtLeast(12.dp),
                        bottom = 12.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_room_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = DateMateDarkText
                        )
                    }

                    Box {
                        AsyncImage(
                            model = thread.companionPhotoUrl,
                            contentDescription = thread.companionName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DateMatePrimaryPink, CircleShape)
                        )
                        if (thread.isOnline) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(DateMateSuccessGreen)
                                    .border(2.dp, DateMateWhite, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = thread.companionName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = DateMateDarkText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            if (thread.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "Verified",
                                    tint = DateMatePrimaryPink,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                thread.isTyping -> "typing…"
                                thread.isOnline -> "Online • Ready to connect"
                                else -> "Active recently"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (thread.isOnline || thread.isTyping) DateMateSuccessGreen else DateMateSecondaryText,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Audio Call, Video Call, Book Date & More Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onStartAudioCall,
                        modifier = Modifier.testTag("chat_audio_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = "Audio Call",
                            tint = DateMatePrimaryPink
                        )
                    }

                    IconButton(
                        onClick = onStartVideoCall,
                        modifier = Modifier.testTag("chat_video_call_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = "Video Call",
                            tint = DateMatePrimaryPink
                        )
                    }

                    IconButton(onClick = onBookDateWithCompanion) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Book Date",
                            tint = DateMatePrimaryPink
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = DateMateDarkText
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(DateMateWhite)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Book a Date with ${thread.companionName}", color = DateMateDarkText) },
                                onClick = {
                                    showMoreMenu = false
                                    onBookDateWithCompanion()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Profile", color = DateMateDarkText) },
                                onClick = {
                                    showMoreMenu = false
                                    onReportCompanion()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block Profile", color = Color(0xFFE11D48)) },
                                onClick = {
                                    showMoreMenu = false
                                    onBlockCompanion()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                // Romantic safety & encryption header pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = DateMateSoftBlush,
                        modifier = Modifier.border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
                    ) {
                        Text(
                            text = "Tap any message to react with 3D hearts ❤️ • Long-press to reply",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = DateMatePrimaryPink,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(
                    message = message,
                    showReactionBar = activeReactionMessageId == message.id,
                    onToggleReactionBar = {
                        activeReactionMessageId = if (activeReactionMessageId == message.id) null else message.id
                    },
                    onSelectReaction = { emoji ->
                        onToggleReaction(message.id, emoji)
                        activeReactionMessageId = null
                    },
                    onReply = { replyToMessage = message },
                    onDelete = {
                        onDeleteMessage(message.id)
                        activeReactionMessageId = null
                    }
                )
            }

            if (thread.isTyping) {
                item {
                    TypingIndicatorBubble(companionName = thread.companionName)
                }
            }
        }

        // Reply-To Quote Banner
        AnimatedVisibility(visible = replyToMessage != null) {
            replyToMessage?.let { quoted ->
                Surface(
                    color = DateMateSoftBlush,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Reply,
                                contentDescription = null,
                                tint = DateMatePrimaryPink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Replying to ${quoted.senderName}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = DateMatePrimaryPink,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = quoted.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = DateMateDarkText
                                    )
                                )
                            }
                        }
                        IconButton(
                            onClick = { replyToMessage = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel reply",
                                tint = DateMateSecondaryText
                            )
                        }
                    }
                }
            }
        }

        // Emoji Quick Picker Bar
        AnimatedVisibility(visible = showEmojiBar) {
            Surface(
                color = DateMateWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DateMateSoftBorder)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    listOf("❤️", "😍", "🌹", "✨", "🥂", "☕", "😊", "🌸").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { inputText += emoji }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }

        // Bottom Input Composer
        Surface(
            color = DateMateWhite,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DateMateSoftBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 12.dp,
                        top = 10.dp,
                        bottom = (navBottomPadding + 10.dp).coerceAtLeast(14.dp)
                    )
            ) {
                // Emoji Picker Button
                IconButton(
                    onClick = { showEmojiBar = !showEmojiBar },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = "Emoji Picker",
                        tint = if (showEmojiBar) DateMatePrimaryPink else DateMateSecondaryText
                    )
                }

                // Image Attachment Picker
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Share Image",
                        tint = DateMatePrimaryPink
                    )
                }

                // Voice Note Quick Send Button
                IconButton(
                    onClick = {
                        onSendMessage(
                            "Voice message: Looking forward to our upcoming evening!",
                            "",
                            12,
                            replyToMessage?.text.orEmpty()
                        )
                        replyToMessage = null
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("chat_voice_note_btn")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Send Voice Message",
                        tint = DateMatePrimaryPink
                    )
                }

                // Message Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Write a warm message…",
                            style = MaterialTheme.typography.bodyMedium.copy(color = DateMateSecondaryText)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(999.dp),
                    colors = dateMateTextFieldColors(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )

                // Rose-Pink Gradient Send Button
                Surface(
                    shape = CircleShape,
                    color = DateMatePrimaryPink,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .bouncy3DPress(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(
                                        inputText.trim(),
                                        "",
                                        0,
                                        replyToMessage?.text.orEmpty()
                                    )
                                    inputText = ""
                                    replyToMessage = null
                                }
                            }
                        )
                        .testTag("chat_send_btn")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(DateMatePinkGradient)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send Message",
                            tint = DateMateWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: DateMateMessage,
    showReactionBar: Boolean,
    onToggleReactionBar: () -> Unit,
    onSelectReaction: (String) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    val isMe = message.senderIsMe

    Column(
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Floating Reaction Selector Bar when tapped
        AnimatedVisibility(visible = showReactionBar) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateWhite,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    listOf("❤️", "😍", "🌹", "✨", "🥂").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelectReaction(emoji) }
                                .padding(4.dp)
                        )
                    }
                    IconButton(
                        onClick = onReply,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = "Reply",
                            tint = DateMatePrimaryPink,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Message Bubble:
        // Outgoing -> Rose-pink gradient (#E91E63 -> #FF6F9C)
        // Incoming -> Soft blush pink (#FFF0F5)
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isMe) 22.dp else 6.dp,
                        bottomEnd = if (isMe) 6.dp else 22.dp,
                    )
                )
                .background(
                    brush = if (isMe) {
                        DateMatePinkGradient
                    } else {
                        Brush.horizontalGradient(listOf(DateMateSoftBlush, DateMateSoftBlush))
                    }
                )
                .border(
                    width = if (isMe) 0.dp else 1.dp,
                    color = if (isMe) Color.Transparent else DateMateSoftBorder,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isMe) 22.dp else 6.dp,
                        bottomEnd = if (isMe) 6.dp else 22.dp,
                    )
                )
                .combinedClickable(
                    onClick = onToggleReactionBar,
                    onLongClick = onReply,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (message.replyToPreview.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMe) DateMateWhite.copy(alpha = 0.2f) else DateMateWhite,
                    ) {
                        Text(
                            text = "↩ ${message.replyToPreview}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isMe) DateMateWhite else DateMatePrimaryPink
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (message.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.imageUrl,
                        contentDescription = "Shared photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }

                if (message.voiceDurationSec > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Voice Note",
                            tint = if (isMe) DateMateWhite else DateMatePrimaryPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Voice Message • 0:${message.voiceDurationSec.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isMe) DateMateWhite else DateMateDarkText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isMe) DateMateWhite else DateMateDarkText
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = message.timestampLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isMe) DateMateWhite.copy(alpha = 0.82f) else DateMateSecondaryText,
                            fontSize = 10.sp
                        )
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.DoneAll,
                            contentDescription = "Read receipt",
                            tint = DateMateWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Attached Floating Heart Reactions Pill
        if (message.reactions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DateMateWhite,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .offset(y = (-6).dp)
                    .border(1.dp, DateMateSoftBorder, RoundedCornerShape(999.dp))
                    .clickable { onToggleReactionBar() }
            ) {
                Text(
                    text = message.reactions.joinToString(" "),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicatorBubble(
    companionName: String,
) {
    val infinite = rememberInfiniteTransition(label = "typing_dots")
    val alpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DateMateSoftBlush,
        modifier = Modifier.border(1.dp, DateMateSoftBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = DateMatePrimaryPink.copy(alpha = alpha),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "$companionName is typing…",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = DateMatePrimaryPink,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
