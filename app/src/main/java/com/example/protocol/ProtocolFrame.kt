package com.example.protocol

sealed class ProtocolFrame(val type: Byte) {

    data class Handshake(
        val peerId: String,
        val displayName: String,
        val protocolVersion: Byte = ProtocolConstants.PROTOCOL_VERSION
    ) : ProtocolFrame(ProtocolConstants.TYPE_HANDSHAKE)

    data class HandshakeAck(
        val peerId: String,
        val displayName: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_HANDSHAKE_ACK)

    data class TextMessage(
        val messageId: String,
        val timestamp: Long,
        val text: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_TEXT_MESSAGE)

    data class MsgDeliveredAck(
        val messageId: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_MSG_DELIVERED_ACK)

    data class MsgReadReceipt(
        val messageId: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_MSG_READ_RECEIPT)

    data class TransferRequest(
        val transferId: String,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String,
        val sha256: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_TRANSFER_REQUEST)

    data class TransferAccept(
        val transferId: String,
        val startOffset: Long = 0L
    ) : ProtocolFrame(ProtocolConstants.TYPE_TRANSFER_ACCEPT)

    data class TransferReject(
        val transferId: String,
        val reason: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_TRANSFER_REJECT)

    data class FileChunk(
        val transferId: String,
        val chunkIndex: Int,
        val offset: Long,
        val data: ByteArray
    ) : ProtocolFrame(ProtocolConstants.TYPE_FILE_CHUNK) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FileChunk
            if (transferId != other.transferId) return false
            if (chunkIndex != other.chunkIndex) return false
            if (offset != other.offset) return false
            if (!data.contentEquals(other.data)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = transferId.hashCode()
            result = 31 * result + chunkIndex
            result = 31 * result + offset.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class ChunkAck(
        val transferId: String,
        val chunkIndex: Int,
        val nextExpectedOffset: Long
    ) : ProtocolFrame(ProtocolConstants.TYPE_CHUNK_ACK)

    data class TransferComplete(
        val transferId: String,
        val sha256: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_TRANSFER_COMPLETE)

    data class TransferCancel(
        val transferId: String,
        val reason: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_TRANSFER_CANCEL)

    object Ping : ProtocolFrame(ProtocolConstants.TYPE_PING)

    object Pong : ProtocolFrame(ProtocolConstants.TYPE_PONG)

    data class TypingState(
        val isTyping: Boolean
    ) : ProtocolFrame(ProtocolConstants.TYPE_TYPING_STATE)

    data class Disconnect(
        val reason: String
    ) : ProtocolFrame(ProtocolConstants.TYPE_DISCONNECT)
}
