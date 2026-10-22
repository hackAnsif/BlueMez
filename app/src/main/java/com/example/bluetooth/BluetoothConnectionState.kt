package com.example.bluetooth

sealed class BluetoothConnectionState {
    data class Disconnected(val reason: String? = null) : BluetoothConnectionState()
    object Idle : BluetoothConnectionState()
    data class Scanning(val isScanning: Boolean) : BluetoothConnectionState()
    data class Connecting(val deviceAddress: String, val deviceName: String) : BluetoothConnectionState()
    data class Handshaking(val deviceAddress: String, val deviceName: String) : BluetoothConnectionState()
    data class Connected(
        val peerId: String,
        val peerDisplayName: String,
        val bluetoothAddress: String
    ) : BluetoothConnectionState()
    data class Reconnecting(val peerId: String, val attempt: Int) : BluetoothConnectionState()

    val isConnected: Boolean
        get() = this is Connected
}
