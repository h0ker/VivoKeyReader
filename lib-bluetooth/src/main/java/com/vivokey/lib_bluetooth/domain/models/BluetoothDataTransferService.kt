package com.vivokey.lib_bluetooth.domain.models

import android.annotation.SuppressLint
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
    @SuppressLint("NewApi")
    fun listenForIncomingMessages(): Flow<ByteArray?> {
        return flow {
            if(!socket.isConnected) {
                return@flow
            }
            while(true) {
                val buffer = ByteArray(1024)
                try {
                    val lengthBytes = ByteArray(2)
                    socket.inputStream.readNBytes(lengthBytes, 0, 2)
                    val length = lengthBytes[1].toInt() + lengthBytes[0].toInt()

                    socket.inputStream.readNBytes(buffer, 0, length)
                    emit(buffer.copyOfRange(0, length))
                } catch(e: IOException) {
                    Log.i("listenForIncomingMessages()", e.message ?: e.toString())
                    return@flow
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    fun sendMessage(data: ByteArray) {
        val packet = ByteArray(2 + data.size)
        packet[0] = (data.size shr 8).toByte()
        packet[1] = (data.size and 0xff).toByte()
        System.arraycopy(data, 0, packet, 2, data.size)
        socket.outputStream.write(packet)
        socket.outputStream.flush()
    }
}