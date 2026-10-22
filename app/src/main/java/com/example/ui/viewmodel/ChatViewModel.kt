package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothManager
import com.example.data.local.entity.FriendEntity
import com.example.data.local.entity.MessageEntity
import com.example.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    val peerId: String,
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    val messages: StateFlow<List<MessageEntity>> = chatRepository.getMessagesForConversation(peerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friend: StateFlow<FriendEntity?> = chatRepository.getFriendFlow(peerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isConnectedToPeer: StateFlow<Boolean> = bluetoothManager.connectionState.map { state ->
        state is BluetoothConnectionState.Connected && state.peerId == peerId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPeerTyping: StateFlow<Boolean> = chatRepository.isPeerTyping

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private var typingJob: Job? = null

    init {
        chatRepository.setActiveChat(peerId)
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.setActiveChat(null)
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        if (isConnectedToPeer.value) {
            typingJob?.cancel()
            typingJob = viewModelScope.launch {
                chatRepository.sendTypingState(text.isNotBlank())
                if (text.isNotBlank()) {
                    delay(3000)
                    chatRepository.sendTypingState(false)
                }
            }
        }
    }

    fun sendMessage() {
        val text = _inputText.value
        if (text.isBlank()) return
        _inputText.value = ""
        viewModelScope.launch {
            chatRepository.sendTypingState(false)
            chatRepository.sendTextMessage(peerId, text)
        }
    }

    fun sendMedia(uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            chatRepository.sendMedia(peerId, uri, fileName, mimeType)
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.retryMessage(messageId)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    fun updateFriendCustomName(newName: String) {
        viewModelScope.launch {
            chatRepository.updateCustomFriendName(peerId, newName)
        }
    }
}

class ChatViewModelFactory(
    private val peerId: String,
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(peerId, chatRepository, bluetoothManager) as T
    }
}
