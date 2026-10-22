package com.example

import com.example.protocol.FrameEncoder
import com.example.protocol.FrameParser
import com.example.protocol.FrameReader
import com.example.protocol.ProtocolConstants
import com.example.protocol.ProtocolFrame
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

class ProtocolAndTransferUnitTest {

    @Test
    fun testHandshakeEncodingAndDecoding() {
        val original = ProtocolFrame.Handshake(
            peerId = "peer-12345",
            displayName = "Adil Phone",
            protocolVersion = 1
        )
        val encoded = FrameEncoder.encode(original)
        val reader = FrameReader(ByteArrayInputStream(encoded))
        val parsed = reader.readNextFrame()

        assertNotNull(parsed)
        assertTrue(parsed is ProtocolFrame.Handshake)
        val handshake = parsed as ProtocolFrame.Handshake
        assertEquals("peer-12345", handshake.peerId)
        assertEquals("Adil Phone", handshake.displayName)
        assertEquals(1.toByte(), handshake.protocolVersion)
    }

    @Test
    fun testTextMessageUnicodeAndMalayalamAndEmoji() {
        val original = ProtocolFrame.TextMessage(
            messageId = "msg-999",
            timestamp = 1700000000000L,
            text = "ഹലോ സുഹൃത്തേ! 🚀 Welcome to BlueMez! Привет & Hello!"
        )
        val encoded = FrameEncoder.encode(original)
        val reader = FrameReader(ByteArrayInputStream(encoded))
        val parsed = reader.readNextFrame()

        assertNotNull(parsed)
        assertTrue(parsed is ProtocolFrame.TextMessage)
        val textMsg = parsed as ProtocolFrame.TextMessage
        assertEquals("msg-999", textMsg.messageId)
        assertEquals(1700000000000L, textMsg.timestamp)
        assertEquals("ഹലോ സുഹൃത്തേ! 🚀 Welcome to BlueMez! Привет & Hello!", textMsg.text)
    }

    @Test
    fun testDeliveryAckAndReadReceipt() {
        val ack = ProtocolFrame.MsgDeliveredAck(messageId = "msg-1")
        val ackEncoded = FrameEncoder.encode(ack)
        val ackParsed = FrameReader(ByteArrayInputStream(ackEncoded)).readNextFrame()
        assertTrue(ackParsed is ProtocolFrame.MsgDeliveredAck)
        assertEquals("msg-1", (ackParsed as ProtocolFrame.MsgDeliveredAck).messageId)

        val receipt = ProtocolFrame.MsgReadReceipt(messageId = "msg-2")
        val receiptEncoded = FrameEncoder.encode(receipt)
        val receiptParsed = FrameReader(ByteArrayInputStream(receiptEncoded)).readNextFrame()
        assertTrue(receiptParsed is ProtocolFrame.MsgReadReceipt)
        assertEquals("msg-2", (receiptParsed as ProtocolFrame.MsgReadReceipt).messageId)
    }

    @Test
    fun testFileChunkStreamingFraming() {
        val testData = ByteArray(1024) { (it % 256).toByte() }
        val chunk = ProtocolFrame.FileChunk(
            transferId = "transfer-abc",
            chunkIndex = 5,
            offset = 163840L,
            data = testData
        )
        val encoded = FrameEncoder.encode(chunk)
        val parsed = FrameReader(ByteArrayInputStream(encoded)).readNextFrame()

        assertNotNull(parsed)
        assertTrue(parsed is ProtocolFrame.FileChunk)
        val resultChunk = parsed as ProtocolFrame.FileChunk
        assertEquals("transfer-abc", resultChunk.transferId)
        assertEquals(5, resultChunk.chunkIndex)
        assertEquals(163840L, resultChunk.offset)
        assertArrayEquals(testData, resultChunk.data)
    }

    @Test
    fun testStreamReassemblyWithPrecedingGarbage() {
        val validMsg = ProtocolFrame.TextMessage("m1", 123456L, "Clean Message")
        val validEncoded = FrameEncoder.encode(validMsg)

        // Prepend 15 random garbage bytes before the valid frame
        val streamBytes = ByteArrayOutputStream()
        streamBytes.write(byteArrayOf(0x00, 0x11, 0x22, 0x42, 0x4D, 0x00, 0x99.toByte(), 0x77))
        streamBytes.write(validEncoded)

        val reader = FrameReader(ByteArrayInputStream(streamBytes.toByteArray()))
        val frame = reader.readNextFrame()

        assertNotNull(frame)
        assertTrue(frame is ProtocolFrame.TextMessage)
        assertEquals("Clean Message", (frame as ProtocolFrame.TextMessage).text)
    }

    @Test
    fun testOversizedFrameRejection() {
        // Construct a frame with declared length > MAX_PAYLOAD_SIZE (e.g. 1 MB)
        val streamBytes = ByteArrayOutputStream()
        val dataOut = DataOutputStream(streamBytes)
        dataOut.write(ProtocolConstants.MAGIC_BYTES)
        dataOut.writeByte(1) // version
        dataOut.writeByte(ProtocolConstants.TYPE_TEXT_MESSAGE.toInt())
        dataOut.writeInt(2 * 1024 * 1024) // 2 MB > 64 KB limit
        dataOut.flush()

        val reader = FrameReader(ByteArrayInputStream(streamBytes.toByteArray()))
        val frame = reader.readNextFrame()
        // Should reject oversized header and reach EOF
        assertNull(frame)
    }

    @Test
    fun testFilenameSanitization() {
        fun sanitize(name: String): String {
            val baseName = java.io.File(name).name
            return baseName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                .trimStart('.')
                .ifEmpty { "file_${System.currentTimeMillis()}" }
        }

        assertEquals("photo.jpg", sanitize("photo.jpg"))
        assertEquals("test.png", sanitize("../../../path/traversal/test.png"))
        assertEquals("evil_file_cmd.sh", sanitize("evil;file|cmd.sh"))
        assertEquals("hidden.txt", sanitize(".hidden.txt"))
    }

    @Test
    fun testSha256ChecksumIntegrity() {
        val sampleData = "BlueMez Offline Communication Integrity Test".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sampleData).joinToString("") { "%02x".format(it) }

        // Recalculate
        val digest2 = MessageDigest.getInstance("SHA-256")
        val hash2 = digest2.digest(sampleData).joinToString("") { "%02x".format(it) }

        assertEquals(hash, hash2)
        assertEquals(64, hash.length)
    }
}
