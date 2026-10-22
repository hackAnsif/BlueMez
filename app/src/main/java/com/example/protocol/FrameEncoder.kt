package com.example.protocol

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

object FrameEncoder {

    fun encode(frame: ProtocolFrame): ByteArray {
        val payloadStream = ByteArrayOutputStream()
        val dataOut = DataOutputStream(payloadStream)

        when (frame) {
            is ProtocolFrame.Handshake -> {
                dataOut.writeUTF(frame.peerId)
                dataOut.writeUTF(frame.displayName)
                dataOut.writeByte(frame.protocolVersion.toInt())
            }
            is ProtocolFrame.HandshakeAck -> {
                dataOut.writeUTF(frame.peerId)
                dataOut.writeUTF(frame.displayName)
            }
            is ProtocolFrame.TextMessage -> {
                dataOut.writeUTF(frame.messageId)
                dataOut.writeLong(frame.timestamp)
                val textBytes = frame.text.toByteArray(StandardCharsets.UTF_8)
                dataOut.writeInt(textBytes.size)
                dataOut.write(textBytes)
            }
            is ProtocolFrame.MsgDeliveredAck -> {
                dataOut.writeUTF(frame.messageId)
            }
            is ProtocolFrame.MsgReadReceipt -> {
                dataOut.writeUTF(frame.messageId)
            }
            is ProtocolFrame.TransferRequest -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeUTF(frame.fileName)
                dataOut.writeLong(frame.fileSize)
                dataOut.writeUTF(frame.mimeType)
                dataOut.writeUTF(frame.sha256)
            }
            is ProtocolFrame.TransferAccept -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeLong(frame.startOffset)
            }
            is ProtocolFrame.TransferReject -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeUTF(frame.reason)
            }
            is ProtocolFrame.FileChunk -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeInt(frame.chunkIndex)
                dataOut.writeLong(frame.offset)
                dataOut.writeInt(frame.data.size)
                dataOut.write(frame.data)
            }
            is ProtocolFrame.ChunkAck -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeInt(frame.chunkIndex)
                dataOut.writeLong(frame.nextExpectedOffset)
            }
            is ProtocolFrame.TransferComplete -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeUTF(frame.sha256)
            }
            is ProtocolFrame.TransferCancel -> {
                dataOut.writeUTF(frame.transferId)
                dataOut.writeUTF(frame.reason)
            }
            is ProtocolFrame.Ping, is ProtocolFrame.Pong -> {
                // No payload
            }
            is ProtocolFrame.TypingState -> {
                dataOut.writeBoolean(frame.isTyping)
            }
            is ProtocolFrame.Disconnect -> {
                dataOut.writeUTF(frame.reason)
            }
        }

        dataOut.flush()
        val payload = payloadStream.toByteArray()

        // Create full frame: [MAGIC 4B][VERSION 1B][TYPE 1B][PAYLOAD_LEN 4B][PAYLOAD]
        val frameStream = ByteArrayOutputStream(ProtocolConstants.HEADER_SIZE + payload.size)
        val frameOut = DataOutputStream(frameStream)
        frameOut.write(ProtocolConstants.MAGIC_BYTES)
        frameOut.writeByte(ProtocolConstants.PROTOCOL_VERSION.toInt())
        frameOut.writeByte(frame.type.toInt())
        frameOut.writeInt(payload.size)
        if (payload.isNotEmpty()) {
            frameOut.write(payload)
        }
        frameOut.flush()
        return frameStream.toByteArray()
    }
}
