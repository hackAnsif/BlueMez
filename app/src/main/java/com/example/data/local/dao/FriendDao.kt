package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.FriendEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY lastConnectedTimestamp DESC, createdAt DESC")
    fun getAllFriendsFlow(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE peerId = :peerId LIMIT 1")
    suspend fun getFriendByPeerId(peerId: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE bluetoothAddress = :address LIMIT 1")
    suspend fun getFriendByAddress(address: String): FriendEntity?

    @Query("SELECT * FROM friends WHERE peerId = :peerId LIMIT 1")
    fun getFriendFlow(peerId: String): Flow<FriendEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(friend: FriendEntity)

    @Update
    suspend fun update(friend: FriendEntity)

    @Query("UPDATE friends SET customName = :newName WHERE peerId = :peerId")
    suspend fun updateCustomName(peerId: String, newName: String)

    @Query("UPDATE friends SET lastConnectedTimestamp = :timestamp WHERE peerId = :peerId")
    suspend fun updateLastConnected(peerId: String, timestamp: Long)

    @Query("DELETE FROM friends WHERE peerId = :peerId")
    suspend fun deleteFriend(peerId: String)

    @Query("DELETE FROM friends")
    suspend fun clearAll()
}
