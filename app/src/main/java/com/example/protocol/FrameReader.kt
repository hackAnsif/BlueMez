package com.example.protocol

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream

class FrameReader(inputStream: InputStream) {

    private val dataIn = DataInputStream(BufferedInputStream(inputStream, 65536))

    /**
     * Reads the next valid ProtocolFrame from the stream.
     * Blocks until a frame is received or stream reaches EOF.
     * Throws EOFException or returns null when disconnected.
     */
    @Synchronized
    fun readNextFrame(): ProtocolFrame? {
        while (true) {
            try {
                // Step 1: Scan for Magic Bytes: 0x42, 0x4D, 0x45, 0x5A ("BMEZ")
                syncToMagicBytes()

                // Step 2: Read Version and Type
                val version = dataIn.readByte()
                if (version != ProtocolConstants.PROTOCOL_VERSION) {
                    // Protocol version mismatch, skip and resync
                    continue
                }

                val type = dataIn.readByte()

                // Step 3: Read Payload Length (Big-Endian 4-byte Int)
                val payloadLength = dataIn.readInt()
                if (payloadLength < 0 || payloadLength > ProtocolConstants.MAX_PAYLOAD_SIZE) {
                    // Invalid length, resync
                    continue
                }

                // Step 4: Read Payload Bytes
                val payload = if (payloadLength > 0) {
                    val bytes = ByteArray(payloadLength)
                    dataIn.readFully(bytes)
                    bytes
                } else {
                    ByteArray(0)
                }

                // Step 5: Parse Frame
                val frame = FrameParser.parse(type, payload)
                if (frame != null) {
                    return frame
                }
                // If frame was unparseable, continue reading next frame
            } catch (e: EOFException) {
                return null
            } catch (e: Exception) {
                return null
            }
        }
    }

    private fun syncToMagicBytes() {
        var matchIndex = 0
        while (matchIndex < ProtocolConstants.MAGIC_BYTES.size) {
            val b = dataIn.readByte()
            if (b == ProtocolConstants.MAGIC_BYTES[matchIndex]) {
                matchIndex++
            } else {
                matchIndex = if (b == ProtocolConstants.MAGIC_BYTES[0]) 1 else 0
            }
        }
    }
}
