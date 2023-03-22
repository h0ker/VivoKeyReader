package com.vivokey.lib_bluetooth.domain.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<Host>>
    val connectionStatus: StateFlow<ConnectionStatus>

    fun connectOverSPP(host: Host): Flow<ByteArray?>
    suspend fun trySendMessage(message: String): Boolean
    fun killConnection()
}