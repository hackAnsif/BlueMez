package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BlueMezDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: BlueMezDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BlueMezDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BlueMez", appName)
    }

    @Test
    fun testRoomDatabasePersistenceAndQueuedMessages() = runBlocking {
        val friendDao = db.friendDao()
        val messageDao = db.messageDao()
        val conversationDao = db.conversationDao()

        // 1. Insert Friend
        val friend = FriendEntity(
            peerId = "peer-adil-1",
            bluetoothAddress = "AA:BB:CC:DD:EE:FF",
            customName = "Adil",
            originalDisplayName = "Adil's Pixel 8"
        )
        friendDao.insertOrUpdate(friend)

        val retrievedFriend = friendDao.getFriendByPeerId("peer-adil-1")
        assertNotNull(retrievedFriend)
        assertEquals("Adil", retrievedFriend?.customName)

        // 2. Insert Conversation
        val conv = ConversationEntity(
            conversationId = "peer-adil-1",
            peerId = "peer-adil-1",
            lastMessageText = "Hey!",
            lastMessageTimestamp = 1000L,
            unreadCount = 1
        )
        conversationDao.insertOrUpdate(conv)

        // 3. Insert Queued (PENDING) Message
        val pendingMsg = MessageEntity(
            messageId = "msg-offline-1",
            conversationId = "peer-adil-1",
            senderPeerId = "self",
            recipientPeerId = "peer-adil-1",
            text = "Are you nearby?",
            timestamp = 1000L,
            status = MessageStatus.PENDING
        )
        messageDao.insertMessage(pendingMsg)

        // Verify it is returned in pending list for peer
        val pendingList = messageDao.getPendingMessagesForPeer("peer-adil-1")
        assertEquals(1, pendingList.size)
        assertEquals("msg-offline-1", pendingList[0].messageId)

        // 4. Update status to DELIVERED
        messageDao.updateMessageStatus("msg-offline-1", MessageStatus.DELIVERED)
        val updated = messageDao.getMessageById("msg-offline-1")
        assertEquals(MessageStatus.DELIVERED, updated?.status)

        // Verify no more pending messages
        val emptyPending = messageDao.getPendingMessagesForPeer("peer-adil-1")
        assertTrue(emptyPending.isEmpty())
    }

    @Test
    fun testFriendCustomNameUpdate() = runBlocking {
        val friendDao = db.friendDao()
        val friend = FriendEntity(
            peerId = "peer-test-2",
            bluetoothAddress = "11:22:33:44:55:66",
            customName = "Original Name",
            originalDisplayName = "Phone X"
        )
        friendDao.insertOrUpdate(friend)

        friendDao.updateCustomName("peer-test-2", "Best College Friend")
        val updated = friendDao.getFriendByPeerId("peer-test-2")
        assertEquals("Best College Friend", updated?.customName)
    }
}
