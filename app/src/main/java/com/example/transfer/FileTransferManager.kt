package com.example.transfer

import android.content.Context
import android.net.Uri
import com.example.bluetooth.BluetoothManager
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.TransferStatus
import com.example.protocol.ProtocolConstants
import com.example.protocol.ProtocolFrame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TransferProgressEvent(
    val transferId: String,
    val bytesTransferred: Long,
    val totalBytes: Long,
    val status: String
)

class FileTransferManager(
    private val context: Context,
    private val attachmentDao: AttachmentDao,
    private val bluetoothManager: BluetoothManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeOutgoingTransfers = ConcurrentHashMap<String, Job>()
    private val activeIncomingTransfers = ConcurrentHashMap<String, IncomingTransferSession>()
    private val transferLocks = ConcurrentHashMap<String, Mutex>()

    private val _progressEvents = MutableSharedFlow<TransferProgressEvent>(extraBufferCapacity = 64)
    val progressEvents: SharedFlow<TransferProgressEvent> = _progressEvents.asSharedFlow()

    private val mediaDir = File(context.filesDir, "media").apply { mkdirs() }

    private class IncomingTransferSession(
        val transferId: String,
        val fileName: String,
        val totalSize: Long,
        val mimeType: String,
        val expectedSha256: String,
        val tempFile: File,
        val randomAccessFile: RandomAccessFile,
        var receivedBytes: Long = 0L,
        var lastChunkIndex: Int = -1
    )

    fun sanitizeFileName(name: String): String {
        val baseName = File(name).name
        return baseName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .trimStart('.')
            .ifEmpty { "file_${System.currentTimeMillis()}" }
    }

    suspend fun startOutgoingTransfer(
        uri: Uri,
        fileName: String,
        mimeType: String,
        messageId: String,
        maxImageBytes: Long,
        maxVideoBytes: Long
    ): String? {
        val transferId = UUID.randomUUID().toString()
        val sanitizedName = sanitizeFileName(fileName)

        return withContext(Dispatchers.IO) {
            try {
                // Copy selected file to private storage cache to calculate hash and stream reliably
                val cacheFile = File(context.cacheDir, "out_${transferId}_$sanitizedName")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                val fileSize = cacheFile.length()
                val isImage = mimeType.startsWith("image/")
                val isVideo = mimeType.startsWith("video/")

                // Verify size limits
                if (isImage && fileSize > maxImageBytes) {
                    cacheFile.delete()
                    return@withContext null
                }
                if (isVideo && fileSize > maxVideoBytes) {
                    cacheFile.delete()
                    return@withContext null
                }

                // Compute SHA-256
                val sha256 = computeSha256(FileInputStream(cacheFile))

                // Insert into DB
                val attachment = AttachmentEntity(
                    attachmentId = transferId,
                    messageId = messageId,
                    fileName = sanitizedName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    localPath = cacheFile.absolutePath,
                    sha256 = sha256,
                    isIncoming = false,
                    transferStatus = TransferStatus.PENDING,
                    bytesTransferred = 0L
                )
                attachmentDao.insertAttachment(attachment)

                // Send TransferRequest
                val requestSent = bluetoothManager.sendFrame(
                    ProtocolFrame.TransferRequest(
                        transferId = transferId,
                        fileName = sanitizedName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        sha256 = sha256
                    )
                )

                if (!requestSent) {
                    attachmentDao.updateCompletion(transferId, TransferStatus.FAILED, null, "Failed to send transfer request")
                    return@withContext null
                }

                transferId
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun handleTransferRequest(request: ProtocolFrame.TransferRequest, maxBytes: Long) {
        val sanitizedName = sanitizeFileName(request.fileName)

        // Validate size
        if (request.fileSize <= 0 || request.fileSize > maxBytes) {
            bluetoothManager.sendFrame(
                ProtocolFrame.TransferReject(request.transferId, "File size exceeds limit (${request.fileSize} bytes)")
            )
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val tempFile = File(mediaDir, "${request.transferId}.tmp")
                if (tempFile.exists()) tempFile.delete()
                val raf = RandomAccessFile(tempFile, "rw")
                raf.setLength(request.fileSize)

                val session = IncomingTransferSession(
                    transferId = request.transferId,
                    fileName = sanitizedName,
                    totalSize = request.fileSize,
                    mimeType = request.mimeType,
                    expectedSha256 = request.sha256,
                    tempFile = tempFile,
                    randomAccessFile = raf
                )
                activeIncomingTransfers[request.transferId] = session

                // Create Attachment in DB
                val attachment = AttachmentEntity(
                    attachmentId = request.transferId,
                    messageId = request.transferId, // Corresponds to message ID for attachment
                    fileName = sanitizedName,
                    fileSize = request.fileSize,
                    mimeType = request.mimeType,
                    localPath = null,
                    sha256 = request.sha256,
                    isIncoming = true,
                    transferStatus = TransferStatus.TRANSFERRING,
                    bytesTransferred = 0L
                )
                attachmentDao.insertAttachment(attachment)

                // Accept transfer
                bluetoothManager.sendFrame(ProtocolFrame.TransferAccept(request.transferId, startOffset = 0L))
            } catch (e: Exception) {
                bluetoothManager.sendFrame(ProtocolFrame.TransferReject(request.transferId, "Storage error: ${e.message}"))
            }
        }
    }

    fun handleTransferAccept(accept: ProtocolFrame.TransferAccept) {
        val transferId = accept.transferId
        val job = scope.launch(Dispatchers.IO) {
            val attachment = attachmentDao.getAttachmentById(transferId) ?: return@launch
            val file = attachment.localPath?.let { File(it) } ?: return@launch
            if (!file.exists()) return@launch

            attachmentDao.updateProgress(transferId, accept.startOffset, TransferStatus.TRANSFERRING)

            try {
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(accept.startOffset)
                    var currentOffset = accept.startOffset
                    var chunkIndex = 0
                    val buffer = ByteArray(ProtocolConstants.DEFAULT_CHUNK_SIZE)

                    while (currentOffset < attachment.fileSize) {
                        val bytesToRead = minOf(buffer.size.toLong(), attachment.fileSize - currentOffset).toInt()
                        raf.readFully(buffer, 0, bytesToRead)
                        val chunkData = if (bytesToRead == buffer.size) buffer.clone() else buffer.copyOf(bytesToRead)

                        val chunkFrame = ProtocolFrame.FileChunk(
                            transferId = transferId,
                            chunkIndex = chunkIndex,
                            offset = currentOffset,
                            data = chunkData
                        )

                        val sent = bluetoothManager.sendFrame(chunkFrame)
                        if (!sent) {
                            attachmentDao.updateCompletion(transferId, TransferStatus.FAILED, file.absolutePath, "Connection lost during transfer")
                            return@use
                        }

                        currentOffset += bytesToRead
                        chunkIndex++

                        attachmentDao.updateProgress(transferId, currentOffset, TransferStatus.TRANSFERRING)
                        _progressEvents.emit(
                            TransferProgressEvent(transferId, currentOffset, attachment.fileSize, TransferStatus.TRANSFERRING)
                        )
                    }

                    // Send TransferComplete
                    bluetoothManager.sendFrame(ProtocolFrame.TransferComplete(transferId, attachment.sha256))
                    attachmentDao.updateCompletion(transferId, TransferStatus.COMPLETED, file.absolutePath, null)
                    _progressEvents.emit(
                        TransferProgressEvent(transferId, attachment.fileSize, attachment.fileSize, TransferStatus.COMPLETED)
                    )
                }
            } catch (e: CancellationException) {
                bluetoothManager.sendFrame(ProtocolFrame.TransferCancel(transferId, "Cancelled by sender"))
                attachmentDao.updateCompletion(transferId, TransferStatus.CANCELLED, file.absolutePath, "Cancelled")
            } catch (e: Exception) {
                attachmentDao.updateCompletion(transferId, TransferStatus.FAILED, file.absolutePath, e.message)
            } finally {
                activeOutgoingTransfers.remove(transferId)
            }
        }
        activeOutgoingTransfers[transferId] = job
    }

    suspend fun handleFileChunk(chunk: ProtocolFrame.FileChunk) {
        val session = activeIncomingTransfers[chunk.transferId] ?: return
        val lock = transferLocks.getOrPut(chunk.transferId) { Mutex() }

        withContext(Dispatchers.IO) {
            lock.withLock {
                try {
                    session.randomAccessFile.seek(chunk.offset)
                    session.randomAccessFile.write(chunk.data)
                    session.receivedBytes = maxOf(session.receivedBytes, chunk.offset + chunk.data.size)
                    session.lastChunkIndex = chunk.chunkIndex

                    attachmentDao.updateProgress(chunk.transferId, session.receivedBytes, TransferStatus.TRANSFERRING)
                    _progressEvents.emit(
                        TransferProgressEvent(chunk.transferId, session.receivedBytes, session.totalSize, TransferStatus.TRANSFERRING)
                    )

                    // Send ChunkAck
                    bluetoothManager.sendFrame(ProtocolFrame.ChunkAck(chunk.transferId, chunk.chunkIndex, session.receivedBytes))
                } catch (e: Exception) {
                    cancelTransfer(chunk.transferId, "I/O error during chunk write")
                }
            }
        }
    }

    suspend fun handleTransferComplete(complete: ProtocolFrame.TransferComplete) {
        val session = activeIncomingTransfers.remove(complete.transferId) ?: return

        withContext(Dispatchers.IO) {
            try {
                session.randomAccessFile.close()

                // Verify SHA-256
                val computedHash = computeSha256(FileInputStream(session.tempFile))
                if (computedHash.equals(complete.sha256, ignoreCase = true)) {
                    // Finalize file
                    val finalFile = File(mediaDir, "${complete.transferId}_${session.fileName}")
                    session.tempFile.renameTo(finalFile)

                    attachmentDao.updateCompletion(complete.transferId, TransferStatus.COMPLETED, finalFile.absolutePath, null)
                    _progressEvents.emit(
                        TransferProgressEvent(complete.transferId, session.totalSize, session.totalSize, TransferStatus.COMPLETED)
                    )
                } else {
                    session.tempFile.delete()
                    attachmentDao.updateCompletion(complete.transferId, TransferStatus.FAILED, null, "SHA-256 checksum mismatch")
                    _progressEvents.emit(
                        TransferProgressEvent(complete.transferId, session.receivedBytes, session.totalSize, TransferStatus.FAILED)
                    )
                }
            } catch (e: Exception) {
                session.tempFile.delete()
                attachmentDao.updateCompletion(complete.transferId, TransferStatus.FAILED, null, e.message)
            } finally {
                transferLocks.remove(complete.transferId)
            }
        }
    }

    suspend fun cancelTransfer(transferId: String, reason: String = "Cancelled") {
        activeOutgoingTransfers.remove(transferId)?.cancel()

        activeIncomingTransfers.remove(transferId)?.let { session ->
            try {
                session.randomAccessFile.close()
                session.tempFile.delete()
            } catch (e: Exception) {}
        }
        transferLocks.remove(transferId)

        bluetoothManager.sendFrame(ProtocolFrame.TransferCancel(transferId, reason))
        attachmentDao.updateCompletion(transferId, TransferStatus.CANCELLED, null, reason)
        _progressEvents.emit(TransferProgressEvent(transferId, 0, 0, TransferStatus.CANCELLED))
    }

    fun handleTransferCancel(cancel: ProtocolFrame.TransferCancel) {
        scope.launch(Dispatchers.IO) {
            activeOutgoingTransfers.remove(cancel.transferId)?.cancel()
            activeIncomingTransfers.remove(cancel.transferId)?.let { session ->
                try {
                    session.randomAccessFile.close()
                    session.tempFile.delete()
                } catch (e: Exception) {}
            }
            transferLocks.remove(cancel.transferId)
            attachmentDao.updateCompletion(cancel.transferId, TransferStatus.CANCELLED, null, cancel.reason)
            _progressEvents.emit(TransferProgressEvent(cancel.transferId, 0, 0, TransferStatus.CANCELLED))
        }
    }

    private fun computeSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(16384)
        var bytesRead: Int
        inputStream.use {
            while (it.read(buffer).also { count -> bytesRead = count } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
