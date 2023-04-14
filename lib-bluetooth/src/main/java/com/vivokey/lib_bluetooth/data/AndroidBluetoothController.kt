package com.vivokey.lib_bluetooth.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import org.apache.commons.codec.binary.Hex
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

    private var _dataTransferService: BluetoothDataTransferService? = null

    private val _pairedDevices = MutableStateFlow<List<Host>>(emptyList())
    override val pairedDevices: StateFlow<List<Host>>
        get() = _pairedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = _connectionStatus.asStateFlow()

    private var _socket: BluetoothSocket? = null

    init {
        updatePairedDevices()
    }

    override suspend fun trySendMessage(message: ByteArray) {
        Log.i("trySendMessage():", Hex.encodeHexString(message))
        _dataTransferService?.sendMessage(message)
    }

    override fun killConnection() {
        Log.i("killConnection():", "Killing Bluetooth Connection")
        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
        _socket?.close()
        _socket = null
        _dataTransferService = null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override suspend fun connectOverSPP(host: Host): Flow<ByteArray?> {
        Log.i("connectOverSPP():", host.name ?: "No Host")
        _connectionStatus.update { ConnectionStatus.CONNECTING }
        return flow {

            host.address.let {
                if (BluetoothAdapter.checkBluetoothAddress(it)) {
                    val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(it)
                    val socket =
                        bluetoothDevice?.createRfcommSocketToServiceRecord(SPP_UUID)
                    if (socket != null) {
                        try {
                            socket.connect()
                            _socket = socket
                            _connectionStatus.update { ConnectionStatus.CONNECTED }
                            _dataTransferService = BluetoothDataTransferService(socket)
                            _dataTransferService?.let { transferService ->
                                emitAll(transferService.listenForIncomingMessages())
                            }
                        } catch (e: Exception) {
                            Log.i("connectOverSPP():", e.message ?: e.toString())
                            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                        }
                    }
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