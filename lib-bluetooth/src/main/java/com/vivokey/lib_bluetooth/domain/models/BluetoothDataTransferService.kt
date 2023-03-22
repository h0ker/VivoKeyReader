package com.vivokey.lib_bluetooth.domain.models

import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    fun listenForIncomingMessages(): Flow<ByteArray?> {
        return flow {
            if(!socket.isConnected) {
                return@flow
            }
            val buffer = ByteArray(1024)
            var readBytes = 0
            while(true) {
                try {
                    readBytes = socket.inputStream.read(buffer)
                } catch(e: IOException) {
                    return@flow
                }
                emit(buffer.copyOfRange(0, readBytes))
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(message: String): Boolean {
        val messageBytes = message.toByteArray()
        return withContext(Dispatchers.IO) {
            try {
                socket.outputStream.write(messageBytes)
            } catch(e: Exception) {
                return@withContext false
            }
            true
        }
    }
}