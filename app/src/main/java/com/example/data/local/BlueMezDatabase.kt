package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.FriendDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.AttachmentEntity
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.MessageEntity

@Database(
    entities = [
        FriendEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BlueMezDatabase : RoomDatabase() {

    abstract fun friendDao(): FriendDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: BlueMezDatabase? = null

        fun getDatabase(context: Context): BlueMezDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlueMezDatabase::class.java,
                    "bluemez.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
