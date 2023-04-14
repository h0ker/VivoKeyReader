package com.vivokey.lib_bluetooth.domain.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException

class BluetoothDataTransferService(
    private val socket: BluetoothSocket
) {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun listenForIncomingMessages(): Flow<ByteArray?> {
        return flow {
            if(!socket.isConnected) {
                return@flow
            }
            while(true) {
                val buffer = ByteArray(1024)
                try {
                    val length1 = socket.inputStream.read()
                    val length2 = socket.inputStream.read()
                    if(length1 < 0 || length2 < 0) {
                        throw(IOException())
                    }
                    val length = length1 shl 8 or length2
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