package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getAttachmentById(attachmentId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE attachmentId = :attachmentId LIMIT 1")
    fun getAttachmentFlow(attachmentId: String): Flow<AttachmentEntity?>

    @Query("SELECT * FROM attachments WHERE messageId = :messageId LIMIT 1")
    suspend fun getAttachmentForMessage(messageId: String): AttachmentEntity?

    @Query("SELECT * FROM attachments ORDER BY attachmentId DESC")
    fun getAllAttachmentsFlow(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transferStatus IN ('PENDING', 'TRANSFERRING')")
    fun getActiveTransfersFlow(): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Query("UPDATE attachments SET bytesTransferred = :bytes, transferStatus = :status WHERE attachmentId = :attachmentId")
    suspend fun updateProgress(attachmentId: String, bytes: Long, status: String)

    @Query("UPDATE attachments SET transferStatus = :status, localPath = :path, errorMessage = :error WHERE attachmentId = :attachmentId")
    suspend fun updateCompletion(attachmentId: String, status: String, path: String?, error: String?)

    @Query("DELETE FROM attachments WHERE attachmentId = :attachmentId")
    suspend fun deleteAttachment(attachmentId: String)

    @Query("DELETE FROM attachments")
    suspend fun clearAll()
}
