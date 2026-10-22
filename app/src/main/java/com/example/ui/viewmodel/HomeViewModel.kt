package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothManager
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.FriendEntity
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationItem(
    val friend: FriendEntity?,
    val conversation: ConversationEntity,
    val isConnected: Boolean
)

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState
    val isHosting: StateFlow<Boolean> = bluetoothManager.isHosting
    val isDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering
    val displayNameFlow: StateFlow<String> = chatRepository.preferencesManager.displayNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BlueMez User")

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val conversationItems: StateFlow<List<ConversationItem>> = combine(
        chatRepository.allConversationsFlow,
        chatRepository.allFriendsFlow,
        bluetoothManager.connectionState,
        _searchQuery
    ) { conversations, friends, connState, query ->
        val friendMap = friends.associateBy { it.peerId }
        val connectedPeerId = (connState as? BluetoothConnectionState.Connected)?.peerId

        conversations.mapNotNull { conv ->
            val friend = friendMap[conv.peerId]
            val title = friend?.customName ?: conv.peerId
            if (query.isNotBlank() && !title.contains(query, ignoreCase = true) && !conv.lastMessageText.contains(query, ignoreCase = true)) {
                null
            } else {
                ConversationItem(
                    friend = friend,
                    conversation = conv,
                    isConnected = conv.peerId == connectedPeerId
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleHosting() {
        viewModelScope.launch {
            if (isHosting.value) {
                bluetoothManager.stopServer()
            } else {
                val peerId = chatRepository.preferencesManager.getOrCreatePeerId()
                val name = displayNameFlow.value
                bluetoothManager.startServer(peerId, name)
            }
        }
    }

    fun disconnectActive() {
        bluetoothManager.disconnect("User disconnected")
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
        }
    }

    fun deleteFriend(peerId: String) {
        viewModelScope.launch {
            chatRepository.deleteFriend(peerId)
        }
    }
}

class HomeViewModelFactory(
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(chatRepository, bluetoothManager) as T
    }
}
