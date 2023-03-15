package com.vivokey.lib_bluetooth.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.vivokey.lib_bluetooth.domain.models.BluetoothDataTransferService
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import java.net.Socket
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController (
    private val context: Context,
): BluetoothController {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var dataTransferService: BluetoothDataTransferService? = null

    private val _scannedDevices = MutableStateFlow<List<Host>>(emptyList())
    override val scannedDevices: StateFlow<List<Host>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<Host>>(emptyList())
    override val pairedDevices: StateFlow<List<Host>>
        get() = _pairedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = _connectionStatus.asStateFlow()

    private var _socket: BluetoothSocket? = null

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toHost()
            if(newDevice in devices) devices else devices + newDevice
        }
    }

    init {
        updatePairedDevices()
    }

    override suspend fun trySendMessage(message: String): Boolean {
        if(dataTransferService == null) {
            return true
        }
        return dataTransferService?.sendMessage(message) ?: false
    }

    override fun startDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()

        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }

        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
    }

    override fun killConnection() {
        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
        _socket?.close()
        _socket = null
    }

    override fun connectOverSPP(host: Host): Flow<String?> {
        _connectionStatus.update { ConnectionStatus.CONNECTING }
        return flow {
            host.address.let {
                if (BluetoothAdapter.checkBluetoothAddress(it)) {
                    val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(it)
                    val socket =
                        bluetoothDevice?.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    if (socket != null) {
                        try {
                            socket.connect()
                            _socket = socket
                            _connectionStatus.update { ConnectionStatus.CONNECTED }
                            dataTransferService = BluetoothDataTransferService(socket)
                            emitAll(dataTransferService!!.listenForIncomingMessages())
                            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                        } catch (e: Exception) {
                            println(e)
                            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                        }
                    } else {
                        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                    }
                } else {
                    _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                }
            }
        }
    }

    private fun updatePairedDevices() {
        if(!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter?.bondedDevices?.map {
            it.toHost()
        }?.also { devices ->
            _pairedDevices.update { devices }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}