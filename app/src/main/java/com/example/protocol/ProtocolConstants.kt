package com.example.protocol

import java.util.UUID

object ProtocolConstants {
    // Protocol Identification
    val MAGIC_BYTES = byteArrayOf(0x42, 0x4D, 0x45, 0x5A) // "BMEZ" in ASCII
    const val PROTOCOL_VERSION: Byte = 1

    // Well-defined application Service UUID for RFCOMM SPP
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    const val SERVICE_NAME = "BlueMezMessenger"

    // Frame Types
    const val TYPE_HANDSHAKE: Byte = 0x01
    const val TYPE_HANDSHAKE_ACK: Byte = 0x02
    const val TYPE_TEXT_MESSAGE: Byte = 0x03
    const val TYPE_MSG_DELIVERED_ACK: Byte = 0x04
    const val TYPE_MSG_READ_RECEIPT: Byte = 0x05
    const val TYPE_TRANSFER_REQUEST: Byte = 0x06
    const val TYPE_TRANSFER_ACCEPT: Byte = 0x07
    const val TYPE_TRANSFER_REJECT: Byte = 0x08
    const val TYPE_FILE_CHUNK: Byte = 0x09
    const val TYPE_CHUNK_ACK: Byte = 0x0A
    const val TYPE_TRANSFER_COMPLETE: Byte = 0x0B
    const val TYPE_TRANSFER_CANCEL: Byte = 0x0C
    const val TYPE_PING: Byte = 0x0D
    const val TYPE_PONG: Byte = 0x0E
    const val TYPE_TYPING_STATE: Byte = 0x0F
    const val TYPE_DISCONNECT: Byte = 0x10

    // Size Constraints
    const val HEADER_SIZE = 10 // 4 bytes magic + 1 byte version + 1 byte type + 4 bytes length
    const val MAX_PAYLOAD_SIZE = 64 * 1024 // 64 KB max frame payload
    const val DEFAULT_CHUNK_SIZE = 32 * 1024 // 32 KB chunk for streaming media
    const val DEFAULT_MAX_IMAGE_SIZE_BYTES = 25L * 1024 * 1024 // 25 MB
    const val DEFAULT_MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB

    // Timeouts
    const val SOCKET_CONNECT_TIMEOUT_MS = 15000L
    const val PING_INTERVAL_MS = 10000L
    const val KEEPALIVE_TIMEOUT_MS = 30000L
}
