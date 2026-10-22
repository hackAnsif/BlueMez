package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as SystemBluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.protocol.FrameEncoder
import com.example.protocol.FrameReader
import com.example.protocol.ProtocolConstants
import com.example.protocol.ProtocolFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

class BluetoothManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val systemBtManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? SystemBluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = systemBtManager?.adapter

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Idle)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _incomingFrames = MutableSharedFlow<ProtocolFrame>(extraBufferCapacity = 64)
    val incomingFrames: SharedFlow<ProtocolFrame> = _incomingFrames.asSharedFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceModel>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _isHosting = MutableStateFlow(false)
    val isHosting: StateFlow<Boolean> = _isHosting.asStateFlow()

    private val _incomingRequest = MutableStateFlow<IncomingConnectionRequest?>(null)
    val incomingRequest: StateFlow<IncomingConnectionRequest?> = _incomingRequest.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var activeOutputStream: OutputStream? = null
    private val sendMutex = Mutex()

    private var currentPeerId: String? = null
    private var currentPeerDisplayName: String? = null
    private var currentDeviceAddress: String? = null

    private var readJob: Job? = null
    private var heartbeatJob: Job? = null
    private var isReceiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    device?.let { dev ->
                        val name = try { dev.name } catch (e: SecurityException) { null } ?: dev.address ?: "Unknown Device"
                        val model = BluetoothDeviceModel(
                            name = name,
                            address = dev.address,
                            bondState = dev.bondState,
                            rssi = if (rssi != Short.MIN_VALUE) rssi else null
                        )
                        updateDiscoveredDevice(model)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isDiscovering.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                        disconnect("Bluetooth was turned off")
                    }
                }
            }
        }
    }

    init {
        registerReceivers()
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun registerReceivers() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(discoveryReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun unregisterReceivers() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
            isReceiverRegistered = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        if (!hasRequiredPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return false
        }
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            // Populate initially paired bonded devices
            val bondedList = bluetoothAdapter.bondedDevices?.map { dev ->
                val name = try { dev.name } catch (e: Exception) { null } ?: dev.address
                BluetoothDeviceModel(
                    name = name,
                    address = dev.address,
                    bondState = dev.bondState
                )
            } ?: emptyList()
            _discoveredDevices.value = bondedList
            return bluetoothAdapter.startDiscovery()
        } catch (e: Exception) {
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (hasRequiredPermissions() && bluetoothAdapter?.isDiscovering == true) {
            try {
                bluetoothAdapter.cancelDiscovery()
            } catch (e: Exception) {
                // ignore
            }
        }
        _isDiscovering.value = false
    }

    private fun updateDiscoveredDevice(device: BluetoothDeviceModel) {
        val currentList = _discoveredDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            currentList[index] = device
        } else {
            currentList.add(device)
        }
        _discoveredDevices.value = currentList
    }

    @SuppressLint("MissingPermission")
    fun startServer(myPeerId: String, myDisplayName: String) {
        if (!hasRequiredPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        stopServer()

        scope.launch {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    ProtocolConstants.SERVICE_NAME,
                    ProtocolConstants.SERVICE_UUID
                )
                _isHosting.value = true

                while (isActive) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        break
                    } ?: break

                    // Incoming connection accepted! Handle handshake
                    handleIncomingSocket(socket, myPeerId, myDisplayName)
                }
            } catch (e: Exception) {
                // Server socket error
            } finally {
                _isHosting.value = false
            }
        }
    }

    fun stopServer() {
        _isHosting.value = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
        serverSocket = null
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleIncomingSocket(
        socket: BluetoothSocket,
        myPeerId: String,
        myDisplayName: String
    ) {
        try {
            val reader = FrameReader(socket.inputStream)
            val handshakeFrame = withContext(Dispatchers.IO) {
                reader.readNextFrame()
            }

            if (handshakeFrame is ProtocolFrame.Handshake) {
                val devAddress = socket.remoteDevice?.address ?: "Unknown"
                val devName = try { socket.remoteDevice?.name } catch (e: Exception) { null } ?: "Unknown Device"

                // Acknowledge handshake and establish session
                val out = socket.outputStream
                val ackBytes = FrameEncoder.encode(ProtocolFrame.HandshakeAck(myPeerId, myDisplayName))
                out.write(ackBytes)
                out.flush()

                setupActiveConnection(socket, reader, out, handshakeFrame.peerId, handshakeFrame.displayName, devAddress)
            } else {
                socket.close()
            }
        } catch (e: Exception) {
            try { socket.close() } catch (ex: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        deviceAddress: String,
        deviceName: String,
        myPeerId: String,
        myDisplayName: String
    ) {
        if (!hasRequiredPermissions() || bluetoothAdapter == null) return
        stopDiscovery()
        disconnect("Initiating new connection")

        scope.launch {
            _connectionState.value = BluetoothConnectionState.Connecting(deviceAddress, deviceName)

            try {
                val remoteDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
                val socket = remoteDevice.createRfcommSocketToServiceRecord(ProtocolConstants.SERVICE_UUID)

                withContext(Dispatchers.IO) {
                    socket.connect()
                }

                _connectionState.value = BluetoothConnectionState.Handshaking(deviceAddress, deviceName)
                val out = socket.outputStream
                val reader = FrameReader(socket.inputStream)

                // Send Handshake
                val handshakeBytes = FrameEncoder.encode(
                    ProtocolFrame.Handshake(myPeerId, myDisplayName, ProtocolConstants.PROTOCOL_VERSION)
                )
                withContext(Dispatchers.IO) {
                    out.write(handshakeBytes)
                    out.flush()
                }

                // Wait for HandshakeAck
                val ackFrame = withContext(Dispatchers.IO) {
                    reader.readNextFrame()
                }

                if (ackFrame is ProtocolFrame.HandshakeAck) {
                    setupActiveConnection(socket, reader, out, ackFrame.peerId, ackFrame.displayName, deviceAddress)
                } else {
                    socket.close()
                    _connectionState.value = BluetoothConnectionState.Disconnected("Handshake failed with $deviceName")
                }
            } catch (e: Exception) {
                _connectionState.value = BluetoothConnectionState.Disconnected("Failed to connect: ${e.message ?: "Connection refused"}")
            }
        }
    }

    private fun setupActiveConnection(
        socket: BluetoothSocket,
        reader: FrameReader,
        out: OutputStream,
        peerId: String,
        peerDisplayName: String,
        deviceAddress: String
    ) {
        activeSocket = socket
        activeOutputStream = out
        currentPeerId = peerId
        currentPeerDisplayName = peerDisplayName
        currentDeviceAddress = deviceAddress

        _connectionState.value = BluetoothConnectionState.Connected(
            peerId = peerId,
            peerDisplayName = peerDisplayName,
            bluetoothAddress = deviceAddress
        )

        // Start reading frames
        startReading(reader)

        // Start heartbeat keepalive
        startHeartbeat()
    }

    private fun startReading(reader: FrameReader) {
        readJob?.cancel()
        readJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val frame = reader.readNextFrame()
                if (frame == null) {
                    // Stream closed / EOF
                    disconnect("Connection closed by peer")
                    break
                }

                when (frame) {
                    is ProtocolFrame.Ping -> {
                        sendFrame(ProtocolFrame.Pong)
                    }
                    is ProtocolFrame.Pong -> {
                        // Keepalive ack received
                    }
                    is ProtocolFrame.Disconnect -> {
                        disconnect("Peer disconnected: ${frame.reason}")
                        break
                    }
                    else -> {
                        _incomingFrames.emit(frame)
                    }
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(ProtocolConstants.PING_INTERVAL_MS)
                if (_connectionState.value is BluetoothConnectionState.Connected) {
                    sendFrame(ProtocolFrame.Ping)
                }
            }
        }
    }

    suspend fun sendFrame(frame: ProtocolFrame): Boolean {
        return withContext(Dispatchers.IO) {
            sendMutex.withLock {
                try {
                    val out = activeOutputStream ?: return@withLock false
                    val encoded = FrameEncoder.encode(frame)
                    out.write(encoded)
                    out.flush()
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    fun disconnect(reason: String = "User disconnected") {
        readJob?.cancel()
        readJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        scope.launch(Dispatchers.IO) {
            try {
                activeOutputStream?.let { out ->
                    val discBytes = FrameEncoder.encode(ProtocolFrame.Disconnect(reason))
                    out.write(discBytes)
                    out.flush()
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                try { activeOutputStream?.close() } catch (e: Exception) {}
                try { activeSocket?.close() } catch (e: Exception) {}
                activeOutputStream = null
                activeSocket = null
                currentPeerId = null
                currentPeerDisplayName = null
                currentDeviceAddress = null
                _connectionState.value = BluetoothConnectionState.Disconnected(reason)
            }
        }
    }
}
