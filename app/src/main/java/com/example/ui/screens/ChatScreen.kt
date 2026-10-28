package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BlueMezApp
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.TransferStatus
import com.example.ui.components.EditFriendNameDialog
import com.example.ui.components.UserAvatar
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.StatusDisconnected
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMediaViewer: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BlueMezApp
    val messages by viewModel.messages.collectAsState()
    val friend by viewModel.friend.collectAsState()
    val isConnected by viewModel.isConnectedToPeer.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRemoveContactDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isImeVisible = WindowInsets.isImeVisible

    // WhatsApp-style: Automatically scroll to bottom when new messages arrive or keyboard opens
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val isVideo = mimeType.startsWith("video/")
            val defaultName = if (isVideo) "video_${System.currentTimeMillis()}.mp4" else "image_${System.currentTimeMillis()}.jpg"
            viewModel.sendMedia(it, defaultName, mimeType)
        }
    }

    // Edit Custom Name Dialog
    if (showEditNameDialog) {
        EditFriendNameDialog(
            initialName = friend?.customName ?: "",
            onDismiss = { showEditNameDialog = false },
            onConfirm = { newName ->
                viewModel.updateFriendCustomName(newName)
                showEditNameDialog = false
            }
        )
    }

    // Clear Conversation Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation?") },
            text = {
                Text("This will remove all message history for this chat on your phone. Your contact details will remain saved. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearConversation()
                        showClearDialog = false
                        Toast.makeText(context, "Conversation cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Messages")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Conversation Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation?") },
            text = {
                Text("This will delete the conversation and its message records from your chats list. Saved attachments will remain in your local app storage.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(deleteMedia = false)
                        showDeleteDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Conversation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Contact Confirmation Dialog
    if (showRemoveContactDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveContactDialog = false },
            title = { Text("Remove Contact?") },
            text = {
                Text("This will remove ${friend?.customName ?: "this user"} from your contacts list. Your conversation history will be preserved.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeContact(keepHistory = true)
                        showRemoveContactDialog = false
                        Toast.makeText(context, "Contact removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Contact")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    val displayName = friend?.customName ?: friend?.originalDisplayName ?: "Peer ${viewModel.peerId.take(6)}"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showEditNameDialog = true }
                    ) {
                        UserAvatar(name = displayName, size = 38)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit name",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isConnected) StatusConnected else StatusDisconnected)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPeerTyping) "typing..." else if (isConnected) "Connected (Bluetooth)" else "Offline (Queued)",
                                    fontSize = 11.sp,
                                    color = if (isPeerTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.testTag("chat_overflow_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Contact Name") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                showEditNameDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Messages") },
                            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                showClearDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Conversation") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                showDeleteDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Contact") },
                            leadingIcon = { Icon(Icons.Default.PersonRemove, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                showRemoveContactDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    MessageBubble(
                        message = msg,
                        app = app,
                        onRetry = { viewModel.retryMessage(msg.messageId) },
                        onDelete = { viewModel.deleteMessage(msg.messageId) },
                        onOpenMedia = { onNavigateToMediaViewer(it) }
                    )
                }
            }

            // Peer typing indicator pill
            AnimatedVisibility(visible = isPeerTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${friend?.customName ?: "Peer"} is typing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }

            // WhatsApp-style anchored Composer Bottom Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Integrated input capsule combining attachment button and text field
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 2.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("attach_media_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = "Attach File",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            TextField(
                                value = inputText,
                                onValueChange = { viewModel.onInputTextChanged(it) },
                                placeholder = {
                                    Text(
                                        "Message (Bluetooth)...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                maxLines = 5,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    cursorColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: MessageEntity,
    app: BlueMezApp,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpenMedia: (String) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOutgoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(min = 60.dp, max = 280.dp)
        ) {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOutgoing) 16.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 16.dp
                ),
                modifier = Modifier
                    .testTag("message_bubble_${message.messageId}")
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOutgoing) 16.dp else 4.dp,
                            bottomEnd = if (isOutgoing) 4.dp else 16.dp
                        )
                    )
                    .combinedClickable(
                        onClick = { message.attachmentId?.let { onOpenMedia(it) } },
                        onLongClick = { showMenu = true }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Attachment Preview if present
                    message.attachmentId?.let { attachId ->
                        val attachmentFlow = remember(attachId) { app.chatRepository.getAttachmentFlow(attachId) }
                        val attachment by attachmentFlow.collectAsState(initial = null)

                        attachment?.let { attach ->
                            AttachmentBubbleContent(
                                attachment = attach,
                                isOutgoing = isOutgoing,
                                onOpen = { onOpenMedia(attach.attachmentId) },
                                onCancel = {
                                    app.applicationScope.launch {
                                        app.transferManager.cancelTransfer(attach.attachmentId)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Text Content (Supports Unicode, Malayalam, Emojis)
                    if (message.text.isNotBlank() && message.attachmentId == null) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Timestamp and Delivery Status Indicator - wraps content neatly
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = dateFormat.format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            DeliveryStatusIcon(status = message.status, onRetry = onRetry)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy Text") },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("BlueMez Message", message.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete Message") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AttachmentBubbleContent(
    attachment: AttachmentEntity,
    isOutgoing: Boolean,
    onOpen: () -> Unit,
    onCancel: () -> Unit
) {
    val isTransferring = attachment.transferStatus == TransferStatus.TRANSFERRING
    val isCompleted = attachment.transferStatus == TransferStatus.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        if (attachment.isImage && isCompleted && attachment.localPath != null) {
            AsyncImage(
                model = File(attachment.localPath),
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpen() }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (attachment.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.fileName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(attachment.fileSize),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isTransferring) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (isTransferring) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { attachment.progressPercent },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${(attachment.progressPercent * 100).toInt()}% transferring...",
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun DeliveryStatusIcon(status: String, onRetry: () -> Unit) {
    when (status) {
        MessageStatus.PENDING -> {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Pending (Offline)",
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        MessageStatus.SENDING -> {
            CircularProgressIndicator(
                modifier = Modifier.size(11.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MessageStatus.READ -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Read",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        MessageStatus.FAILED -> {
            IconButton(
                onClick = onRetry,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024f * 1024f))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024f)
        else -> "$bytes B"
    }
}
