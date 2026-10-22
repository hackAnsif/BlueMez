package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object MessageStatus {
    const val PENDING = "PENDING"
    const val SENDING = "SENDING"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
    const val FAILED = "FAILED"
}

@Entity(
    tableName = "messages",
    indices = [
        Index("conversationId"),
        Index("timestamp")
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val senderPeerId: String,
    val recipientPeerId: String,
    val text: String,
    val timestamp: Long,
    val status: String = MessageStatus.PENDING,
    val attachmentId: String? = null
) {
    val isOutgoing: Boolean
        get() = senderPeerId == "self"
}
