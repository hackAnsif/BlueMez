package com.example

import android.app.Application
import com.example.bluetooth.BluetoothManager
import com.example.data.local.BlueMezDatabase
import com.example.data.local.PreferencesManager
import com.example.repository.ChatRepository
import com.example.transfer.FileTransferManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlueMezApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy { BlueMezDatabase.getDatabase(this) }
    val preferencesManager by lazy { PreferencesManager(this) }
    val bluetoothManager by lazy { BluetoothManager(this) }
    val transferManager by lazy {
        FileTransferManager(
            context = this,
            attachmentDao = database.attachmentDao(),
            bluetoothManager = bluetoothManager
        )
    }
    val chatRepository by lazy {
        ChatRepository(
            context = this,
            friendDao = database.friendDao(),
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            attachmentDao = database.attachmentDao(),
            bluetoothManager = bluetoothManager,
            transferManager = transferManager,
            preferencesManager = preferencesManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        applicationScope.launch {
            // Ensure peer ID is generated on startup
            preferencesManager.getOrCreatePeerId()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        bluetoothManager.unregisterReceivers()
        bluetoothManager.disconnect("Application terminated")
    }

    companion object {
        lateinit var instance: BlueMezApp
            private set
    }
}
