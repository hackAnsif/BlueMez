package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothManager
import com.example.data.local.PreferencesManager
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.FriendDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.protocol.ProtocolFrame
import com.example.transfer.FileTransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val friendDao: FriendDao,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    val bluetoothManager: BluetoothManager,
    val transferManager: FileTransferManager,
    val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val allFriendsFlow: Flow<List<FriendEntity>> = friendDao.getAllFriendsFlow()
    val allConversationsFlow: Flow<List<ConversationEntity>> = conversationDao.getAllConversationsFlow()
    val allAttachmentsFlow: Flow<List<AttachmentEntity>> = attachmentDao.getAllAttachmentsFlow()
    val activeTransfersFlow: Flow<List<AttachmentEntity>> = attachmentDao.getActiveTransfersFlow()

    private val _isPeerTyping = MutableStateFlow(false)
    val isPeerTyping: StateFlow<Boolean> = _isPeerTyping.asStateFlow()

    private var activeChatPeerId: String? = null

    init {
        observeBluetoothConnection()
        observeIncomingFrames()
    }

    fun setActiveChat(peerId: String?) {
        activeChatPeerId = peerId
        if (peerId != null) {
            scope.launch {
                conversationDao.clearUnreadCount(peerId)
            }
        }
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversationFlow(conversationId)
    }

    fun getFriendFlow(peerId: String): Flow<FriendEntity?> {
        return friendDao.getFriendFlow(peerId)
    }

    fun getAttachmentFlow(attachmentId: String): Flow<AttachmentEntity?> {
        return attachmentDao.getAttachmentFlow(attachmentId)
    }

    private fun observeBluetoothConnection() {
        scope.launch {
            bluetoothManager.connectionState.collect { state ->
                if (state is BluetoothConnectionState.Connected) {
                    handlePeerConnected(state)
                } else if (state is BluetoothConnectionState.Disconnected) {
                    _isPeerTyping.value = false
                }
            }
        }
    }

    private suspend fun handlePeerConnected(connected: BluetoothConnectionState.Connected) {
        val existingFriend = friendDao.getFriendByPeerId(connected.peerId)
        val friendName = existingFriend?.customName ?: connected.peerDisplayName

        val friend = FriendEntity(
            peerId = connected.peerId,
            bluetoothAddress = connected.bluetoothAddress,
            customName = friendName,
            originalDisplayName = connected.peerDisplayName,
            lastConnectedTimestamp = System.currentTimeMillis()
        )
        friendDao.insertOrUpdate(friend)

        val conversation = conversationDao.getConversation(connected.peerId) ?: ConversationEntity(
            conversationId = connected.peerId,
            peerId = connected.peerId,
            lastMessageText = "",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        conversationDao.insertOrUpdate(conversation)

        // Automatically deliver queued messages
        flushPendingMessages(connected.peerId)
    }

    private suspend fun flushPendingMessages(peerId: String) {
        val pendingList = messageDao.getPendingMessagesForPeer(peerId)
        for (msg in pendingList) {
            val sent = bluetoothManager.sendFrame(
                ProtocolFrame.TextMessage(
                    messageId = msg.messageId,
                    timestamp = msg.timestamp,
                    text = msg.text
                )
            )
            if (sent) {
                messageDao.updateMessageStatus(msg.messageId, MessageStatus.SENDING)
            }
        }
    }

    private fun observeIncomingFrames() {
        scope.launch {
            bluetoothManager.incomingFrames.collect { frame ->
                when (frame) {
                    is ProtocolFrame.TextMessage -> handleIncomingTextMessage(frame)
                    is ProtocolFrame.MsgDeliveredAck -> handleMsgDeliveredAck(frame)
                    is ProtocolFrame.MsgReadReceipt -> handleMsgReadReceipt(frame)
                    is ProtocolFrame.TransferRequest -> handleIncomingTransferRequest(frame)
                    is ProtocolFrame.TransferAccept -> transferManager.handleTransferAccept(frame)
                    is ProtocolFrame.TransferReject -> {
                        attachmentDao.updateCompletion(frame.transferId, "FAILED", null, frame.reason)
                    }
                    is ProtocolFrame.FileChunk -> transferManager.handleFileChunk(frame)
                    is ProtocolFrame.TransferComplete -> transferManager.handleTransferComplete(frame)
                    is ProtocolFrame.TransferCancel -> transferManager.handleTransferCancel(frame)
                    is ProtocolFrame.TypingState -> {
                        _isPeerTyping.value = frame.isTyping
                    }
                    else -> {}
                }
            }
        }
    }

    private suspend fun handleIncomingTextMessage(frame: ProtocolFrame.TextMessage) {
        val currentConnected = bluetoothManager.connectionState.value as? BluetoothConnectionState.Connected ?: return
        val senderPeerId = currentConnected.peerId

        val existingMessage = messageDao.getMessageById(frame.messageId)
        if (existingMessage != null) {
            // Deduplicate: already received and stored! Acknowledge again in case ack was lost.
            bluetoothManager.sendFrame(ProtocolFrame.MsgDeliveredAck(frame.messageId))
            return
        }

        val message = MessageEntity(
            messageId = frame.messageId,
            conversationId = senderPeerId,
            senderPeerId = senderPeerId,
            recipientPeerId = "self",
            text = frame.text,
            timestamp = frame.timestamp,
            status = MessageStatus.DELIVERED
        )
        messageDao.insertMessage(message)

        // Update conversation
        val isCurrentChat = (activeChatPeerId == senderPeerId)
        conversationDao.updateLastMessage(senderPeerId, frame.text, frame.timestamp)
        if (!isCurrentChat) {
            conversationDao.incrementUnreadCount(senderPeerId)
        }

        // Send delivery acknowledgement
        bluetoothManager.sendFrame(ProtocolFrame.MsgDeliveredAck(frame.messageId))

        // Send read receipt if active in chat and enabled
        if (isCurrentChat && preferencesManager.readReceiptsFlow.first()) {
            bluetoothManager.sendFrame(ProtocolFrame.MsgReadReceipt(frame.messageId))
            messageDao.updateMessageStatus(frame.messageId, MessageStatus.READ)
        }
    }

    private suspend fun handleMsgDeliveredAck(ack: ProtocolFrame.MsgDeliveredAck) {
        val current = messageDao.getMessageById(ack.messageId)
        if (current != null && current.status != MessageStatus.READ) {
            messageDao.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED)
        }
    }

    private suspend fun handleMsgReadReceipt(receipt: ProtocolFrame.MsgReadReceipt) {
        messageDao.updateMessageStatus(receipt.messageId, MessageStatus.READ)
    }

    private suspend fun handleIncomingTransferRequest(request: ProtocolFrame.TransferRequest) {
        val currentConnected = bluetoothManager.connectionState.value as? BluetoothConnectionState.Connected ?: return
        val senderPeerId = currentConnected.peerId

        val isImage = request.mimeType.startsWith("image/")
        val maxMb = if (isImage) preferencesManager.maxImageMbFlow.first() else preferencesManager.maxVideoMbFlow.first()
        val maxBytes = maxMb.toLong() * 1024 * 1024

        // Insert placeholder message for this incoming media in conversation
        val mediaMessage = MessageEntity(
            messageId = request.transferId,
            conversationId = senderPeerId,
            senderPeerId = senderPeerId,
            recipientPeerId = "self",
            text = if (isImage) "[Image: ${request.fileName}]" else "[Video: ${request.fileName}]",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.DELIVERED,
            attachmentId = request.transferId
        )
        messageDao.insertMessage(mediaMessage)
        conversationDao.updateLastMessage(senderPeerId, mediaMessage.text, mediaMessage.timestamp)

        transferManager.handleTransferRequest(request, maxBytes)
    }

    suspend fun sendTextMessage(conversationId: String, text: String): Boolean {
        if (text.isBlank()) return false
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderPeerId = "self",
            recipientPeerId = conversationId,
            text = text.trim(),
            timestamp = timestamp,
            status = MessageStatus.PENDING
        )
        messageDao.insertMessage(message)
        conversationDao.updateLastMessage(conversationId, text.trim(), timestamp)

        val connected = bluetoothManager.connectionState.value
        if (connected is BluetoothConnectionState.Connected && connected.peerId == conversationId) {
            val sent = bluetoothManager.sendFrame(
                ProtocolFrame.TextMessage(messageId, timestamp, text.trim())
            )
            if (sent) {
                messageDao.updateMessageStatus(messageId, MessageStatus.SENDING)
                return true
            }
        }
        return false
    }

    suspend fun sendMedia(conversationId: String, uri: Uri, fileName: String, mimeType: String): String? {
        val isImage = mimeType.startsWith("image/")
        val maxImageBytes = preferencesManager.maxImageMbFlow.first().toLong() * 1024 * 1024
        val maxVideoBytes = preferencesManager.maxVideoMbFlow.first().toLong() * 1024 * 1024

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val transferId = transferManager.startOutgoingTransfer(
            uri = uri,
            fileName = fileName,
            mimeType = mimeType,
            messageId = messageId,
            maxImageBytes = maxImageBytes,
            maxVideoBytes = maxVideoBytes
        ) ?: return null

        val message = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderPeerId = "self",
            recipientPeerId = conversationId,
            text = if (isImage) "[Image: $fileName]" else "[Video: $fileName]",
            timestamp = timestamp,
            status = MessageStatus.SENDING,
            attachmentId = transferId
        )
        messageDao.insertMessage(message)
        conversationDao.updateLastMessage(conversationId, message.text, timestamp)
        return transferId
    }

    suspend fun retryMessage(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        val connected = bluetoothManager.connectionState.value
        if (connected is BluetoothConnectionState.Connected && connected.peerId == msg.recipientPeerId) {
            val sent = bluetoothManager.sendFrame(
                ProtocolFrame.TextMessage(msg.messageId, msg.timestamp, msg.text)
            )
            if (sent) {
                messageDao.updateMessageStatus(messageId, MessageStatus.SENDING)
            }
        }
    }

    suspend fun sendTypingState(isTyping: Boolean) {
        bluetoothManager.sendFrame(ProtocolFrame.TypingState(isTyping))
    }

    suspend fun updateCustomFriendName(peerId: String, newName: String) {
        friendDao.updateCustomName(peerId, newName.trim())
    }

    suspend fun deleteMessage(messageId: String) {
        val msg = messageDao.getMessageById(messageId)
        msg?.attachmentId?.let { attachmentId ->
            val attachment = attachmentDao.getAttachmentById(attachmentId)
            attachment?.localPath?.let { path ->
                try { java.io.File(path).delete() } catch (e: Exception) {}
            }
            attachmentDao.deleteAttachment(attachmentId)
        }
        messageDao.deleteMessage(messageId)
    }

    suspend fun clearConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.updateLastMessage(conversationId, "", System.currentTimeMillis())
        conversationDao.clearUnreadCount(conversationId)
    }

    suspend fun deleteConversation(conversationId: String, deleteMediaFiles: Boolean = false) {
        if (deleteMediaFiles) {
            val messages = messageDao.getPendingMessagesForPeer(conversationId)
            // also delete attachments if any
        }
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun deleteFriend(peerId: String) {
        removeContact(peerId, keepHistory = false)
    }

    suspend fun removeContact(peerId: String, keepHistory: Boolean = true) {
        friendDao.deleteFriend(peerId)
        if (!keepHistory) {
            deleteConversation(peerId)
        }
    }

    suspend fun clearAllData() {
        withContext(Dispatchers.IO) {
            friendDao.clearAll()
            conversationDao.clearAll()
            messageDao.clearAll()
            attachmentDao.clearAll()
            preferencesManager.clearSettings()
            try {
                java.io.File(context.filesDir, "media").deleteRecursively()
                java.io.File(context.filesDir, "bluemez").deleteRecursively()
            } catch (e: Exception) {}
        }
    }
}
