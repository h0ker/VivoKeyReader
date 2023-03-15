package com.vivokey.lib_bluetooth.domain.models

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<Host>>
    val pairedDevices: StateFlow<List<Host>>
    val connectionStatus: StateFlow<ConnectionStatus>

    fun startDiscovery()
    fun stopDiscovery()

    fun connectOverSPP(host: Host): Flow<String?>
    suspend fun trySendMessage(message: String): Boolean
    fun killConnection()

    fun release()
}