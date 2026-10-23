package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothDeviceModel
import com.example.bluetooth.BluetoothManager
import com.example.data.local.entity.FriendEntity
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiscoveryViewModel(
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState
    val isDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun isBluetoothEnabled(): Boolean = bluetoothManager.isBluetoothEnabled()
    fun hasRequiredPermissions(): Boolean = bluetoothManager.hasRequiredPermissions()

    val savedFriends: StateFlow<List<FriendEntity>> = combine(
        chatRepository.allFriendsFlow,
        _searchQuery
    ) { friends, query ->
        if (query.isBlank()) {
            friends
        } else {
            friends.filter {
                it.customName.contains(query, ignoreCase = true) ||
                it.originalDisplayName.contains(query, ignoreCase = true) ||
                it.bluetoothAddress.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredDevices: StateFlow<List<BluetoothDeviceModel>> = combine(
        bluetoothManager.discoveredDevices,
        chatRepository.allFriendsFlow,
        _searchQuery
    ) { devices, friends, query ->
        val savedAddresses = friends.map { it.bluetoothAddress }.toSet()
        devices.filter { !savedAddresses.contains(it.address) }
            .filter {
                query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.address.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startDiscovery(): Boolean {
        return bluetoothManager.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothManager.stopDiscovery()
    }

    fun connectToFriend(friend: FriendEntity) {
        viewModelScope.launch {
            val peerId = chatRepository.preferencesManager.getOrCreatePeerId()
            val name = chatRepository.preferencesManager.displayNameFlow.stateIn(viewModelScope).value
            bluetoothManager.connectToDevice(
                deviceAddress = friend.bluetoothAddress,
                deviceName = friend.customName,
                myPeerId = peerId,
                myDisplayName = name
            )
        }
    }

    fun connectToDevice(device: BluetoothDeviceModel) {
        viewModelScope.launch {
            val peerId = chatRepository.preferencesManager.getOrCreatePeerId()
            val name = chatRepository.preferencesManager.displayNameFlow.stateIn(viewModelScope).value
            bluetoothManager.connectToDevice(
                deviceAddress = device.address,
                deviceName = device.name,
                myPeerId = peerId,
                myDisplayName = name
            )
        }
    }

    fun removeContact(peerId: String) {
        viewModelScope.launch {
            chatRepository.removeContact(peerId, keepHistory = true)
        }
    }
}

class DiscoveryViewModelFactory(
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscoveryViewModel(chatRepository, bluetoothManager) as T
    }
}
