package com.vivokey.lib_bluetooth.domain.models

import com.hoker.lib_utils.domain.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val pairedDevices: StateFlow<List<Host>>
    val connectionStatus: StateFlow<com.hoker.lib_utils.domain.ConnectionStatus>

    suspend fun connectOverSPP(host: Host): Flow<ByteArray?>
    suspend fun sendMessage(message: ByteArray): OperationResult<Unit>
    fun killConnection()
}