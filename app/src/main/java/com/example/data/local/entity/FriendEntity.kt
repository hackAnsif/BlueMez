package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey
    val peerId: String,
    val bluetoothAddress: String,
    val customName: String,
    val originalDisplayName: String,
    val isPaired: Boolean = false,
    val lastConnectedTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
