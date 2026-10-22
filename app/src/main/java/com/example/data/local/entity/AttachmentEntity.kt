package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object TransferStatus {
    const val PENDING = "PENDING"
    const val TRANSFERRING = "TRANSFERRING"
    const val COMPLETED = "COMPLETED"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

@Entity(
    tableName = "attachments",
    indices = [
        Index("messageId"),
        Index("transferStatus")
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val attachmentId: String,
    val messageId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val localPath: String? = null,
    val sha256: String = "",
    val isIncoming: Boolean,
    val transferStatus: String = TransferStatus.PENDING,
    val bytesTransferred: Long = 0L,
    val errorMessage: String? = null
) {
    val progressPercent: Float
        get() = if (fileSize > 0) (bytesTransferred.toFloat() / fileSize).coerceIn(0f, 1f) else 0f

    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    val isVideo: Boolean
        get() = mimeType.startsWith("video/")
}
