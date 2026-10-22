package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothConnectionState
import com.example.bluetooth.BluetoothDeviceModel
import com.example.bluetooth.BluetoothManager
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiscoveryViewModel(
    private val chatRepository: ChatRepository,
    private val bluetoothManager: BluetoothManager
) : ViewModel() {

    val connectionState: StateFlow<BluetoothConnectionState> = bluetoothManager.connectionState
    val isDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering

    val discoveredDevices: StateFlow<List<BluetoothDeviceModel>> = combine(
        bluetoothManager.discoveredDevices,
        chatRepository.allFriendsFlow
    ) { devices, friends ->
        val savedAddresses = friends.map { it.bluetoothAddress }.toSet()
        devices.map { dev ->
            dev.copy(isSavedFriend = savedAddresses.contains(dev.address))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startDiscovery(): Boolean {
        return bluetoothManager.startDiscovery()
    }

    fun stopDiscovery() {
        bluetoothManager.stopDiscovery()
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
