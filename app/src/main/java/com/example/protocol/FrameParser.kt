package com.example.protocol

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.charset.StandardCharsets

object FrameParser {

    fun parse(type: Byte, payload: ByteArray): ProtocolFrame? {
        return try {
            val dataIn = DataInputStream(ByteArrayInputStream(payload))
            when (type) {
                ProtocolConstants.TYPE_HANDSHAKE -> {
                    val peerId = dataIn.readUTF()
                    val displayName = dataIn.readUTF()
                    val version = dataIn.readByte()
                    ProtocolFrame.Handshake(peerId, displayName, version)
                }
                ProtocolConstants.TYPE_HANDSHAKE_ACK -> {
                    val peerId = dataIn.readUTF()
                    val displayName = dataIn.readUTF()
                    ProtocolFrame.HandshakeAck(peerId, displayName)
                }
                ProtocolConstants.TYPE_TEXT_MESSAGE -> {
                    val messageId = dataIn.readUTF()
                    val timestamp = dataIn.readLong()
                    val textLen = dataIn.readInt()
                    if (textLen < 0 || textLen > ProtocolConstants.MAX_PAYLOAD_SIZE) return null
                    val textBytes = ByteArray(textLen)
                    dataIn.readFully(textBytes)
                    val text = String(textBytes, StandardCharsets.UTF_8)
                    ProtocolFrame.TextMessage(messageId, timestamp, text)
                }
                ProtocolConstants.TYPE_MSG_DELIVERED_ACK -> {
                    val messageId = dataIn.readUTF()
                    ProtocolFrame.MsgDeliveredAck(messageId)
                }
                ProtocolConstants.TYPE_MSG_READ_RECEIPT -> {
                    val messageId = dataIn.readUTF()
                    ProtocolFrame.MsgReadReceipt(messageId)
                }
                ProtocolConstants.TYPE_TRANSFER_REQUEST -> {
                    val transferId = dataIn.readUTF()
                    val fileName = dataIn.readUTF()
                    val fileSize = dataIn.readLong()
                    val mimeType = dataIn.readUTF()
                    val sha256 = dataIn.readUTF()
                    ProtocolFrame.TransferRequest(transferId, fileName, fileSize, mimeType, sha256)
                }
                ProtocolConstants.TYPE_TRANSFER_ACCEPT -> {
                    val transferId = dataIn.readUTF()
                    val startOffset = dataIn.readLong()
                    ProtocolFrame.TransferAccept(transferId, startOffset)
                }
                ProtocolConstants.TYPE_TRANSFER_REJECT -> {
                    val transferId = dataIn.readUTF()
                    val reason = dataIn.readUTF()
                    ProtocolFrame.TransferReject(transferId, reason)
                }
                ProtocolConstants.TYPE_FILE_CHUNK -> {
                    val transferId = dataIn.readUTF()
                    val chunkIndex = dataIn.readInt()
                    val offset = dataIn.readLong()
                    val dataSize = dataIn.readInt()
                    if (dataSize < 0 || dataSize > ProtocolConstants.MAX_PAYLOAD_SIZE) return null
                    val chunkData = ByteArray(dataSize)
                    dataIn.readFully(chunkData)
                    ProtocolFrame.FileChunk(transferId, chunkIndex, offset, chunkData)
                }
                ProtocolConstants.TYPE_CHUNK_ACK -> {
                    val transferId = dataIn.readUTF()
                    val chunkIndex = dataIn.readInt()
                    val nextExpectedOffset = dataIn.readLong()
                    ProtocolFrame.ChunkAck(transferId, chunkIndex, nextExpectedOffset)
                }
                ProtocolConstants.TYPE_TRANSFER_COMPLETE -> {
                    val transferId = dataIn.readUTF()
                    val sha256 = dataIn.readUTF()
                    ProtocolFrame.TransferComplete(transferId, sha256)
                }
                ProtocolConstants.TYPE_TRANSFER_CANCEL -> {
                    val transferId = dataIn.readUTF()
                    val reason = dataIn.readUTF()
                    ProtocolFrame.TransferCancel(transferId, reason)
                }
                ProtocolConstants.TYPE_PING -> ProtocolFrame.Ping
                ProtocolConstants.TYPE_PONG -> ProtocolFrame.Pong
                ProtocolConstants.TYPE_TYPING_STATE -> {
                    val isTyping = dataIn.readBoolean()
                    ProtocolFrame.TypingState(isTyping)
                }
                ProtocolConstants.TYPE_DISCONNECT -> {
                    val reason = dataIn.readUTF()
                    ProtocolFrame.Disconnect(reason)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
