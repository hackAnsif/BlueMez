package com.example.bluetooth

data class BluetoothDeviceModel(
    val name: String,
    val address: String,
    val bondState: Int,
    val rssi: Short? = null,
    val isSavedFriend: Boolean = false
) {
    val isBonded: Boolean
        get() = bondState == android.bluetooth.BluetoothDevice.BOND_BONDED
}

data class IncomingConnectionRequest(
    val deviceAddress: String,
    val deviceName: String,
    val peerId: String,
    val peerDisplayName: String
)
