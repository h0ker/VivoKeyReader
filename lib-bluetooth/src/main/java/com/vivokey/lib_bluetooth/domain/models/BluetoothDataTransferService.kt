package com.vivokey.lib_bluetooth.domain.models

import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.codec.binary.Hex
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
                    val messageString = Hex.encodeHexString(buffer.copyOfRange(0, length))
                    Log.i("MESSAGE INFO:", "$length | ${messageString.length/2} | $messageString")
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
        val length = packet[0].toInt() shl 8 or packet[1].toInt()
        Log.i("OUTBOUND:", "$length | ${data.size}")
        System.arraycopy(data, 0, packet, 2, data.size)
        socket.outputStream.write(packet)
        socket.outputStream.flush()
    }
}