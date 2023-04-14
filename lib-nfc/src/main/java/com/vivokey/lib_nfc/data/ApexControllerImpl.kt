package com.vivokey.lib_nfc.data

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.vivokey.lib_nfc.domain.ApduUtils
import com.vivokey.lib_nfc.domain.ApexController
import com.vivokey.lib_nfc.domain.IsodepConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlin.experimental.xor

open class ApexControllerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope
): ApexController {

    private val _connectionStatus = MutableStateFlow(IsodepConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<IsodepConnectionStatus>
        get() = _connectionStatus.asStateFlow()

    private var isoDep: IsoDep? = null
    private val mutex = Mutex()

    override fun connect(tag: Tag) {
        close()
        isoDep = IsoDep.get(tag)
        isoDep?.let {
            it.connect()
            Log.i("ApexConnection", "----ISODEP CONNECTED")
            _connectionStatus.update { IsodepConnectionStatus.CONNECTED }
        }
    }

    override fun getATR(): ByteArray? {
        isoDep?.let {
            try {
                if (!it.isConnected) {
                    return null
                }

                val historicalBytes = it.historicalBytes

                val atr = ByteArray(4 + historicalBytes.size + 1)
                atr[0] = 0x3b.toByte()
                atr[1] = (0x80.toByte() + historicalBytes.size).toByte()
                atr[2] = 0x80.toByte()
                atr[3] = 0x01.toByte()
                System.arraycopy(historicalBytes, 0, atr, 4, historicalBytes.size)

                var tck = atr[1]
                for (idx in 2 until atr.size) {
                    tck = tck.xor(atr[idx])
                }
                atr[atr.size - 1] = tck

                Log.i("ATR:", atr.toHex())
                return atr
            } catch(e: Exception) {
                close()
                throw(e)
            }
        }
        return null
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    override fun close() {
        isoDep = null
        Log.i("ApexConnection", "----ISODEP CLOSED")
        _connectionStatus.update { IsodepConnectionStatus.DISCONNECTED }
    }

    override fun issueApdu(instruction: Byte, p1: Byte, p2: Byte, data: ByteBuffer.() -> Unit): ByteBuffer {
        try {
            val apdu = ByteBuffer
                .allocate(256)
                .put(0)
                .put(instruction)
                .put(p1)
                .put(p2)
                .put(0)
                .apply(data)
                .let {
                    it.put(4, (it.position() - 5).toByte()).array()
                        .copyOfRange(0, it.position())
                }

            return ByteBuffer.allocate(4096).apply {
                var response = splitApduResponse(isoDep!!.transceive(apdu))
                while (response.statusCode != ApduUtils.APDU_OK) {
                    if ((response.statusCode shr 8).toByte() == ApduUtils.APDU_DATA_REMAINING.toByte()) {
                        put(response.data)
                        response = splitApduResponse(
                            isoDep!!.transceive(
                                byteArrayOf(
                                    0,
                                    ApduUtils.SEND_REMAINING_INS.toByte(),
                                    0,
                                    0
                                )
                            )
                        )
                    } else {
                        throw(Exception("Error sending APDU: ${response.statusCode}"))
                    }
                }
                put(response.data).limit(position()).rewind()
            }
        } catch(e: Exception) {
            close()
            throw e
        }
    }

    private fun splitApduResponse(resp: ByteArray): ApduUtils.Companion.ApduResponse {
        return ApduUtils.Companion.ApduResponse(
            resp.copyOfRange(0, resp.size - 2),
            ((0xff and resp[resp.size - 2].toInt()) shl 8) or (0xff and resp[resp.size - 1].toInt())
        )
    }

    override fun transceive(data: ByteArray): ByteArray {
        try {
            return isoDep!!.transceive(data)
        } catch(e: Exception) {
            close()
            throw e
        }
    }

    override fun runSafe(tag: Tag, action: () -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    Log.i("runSafe Mutex", "------MUTEX LOCKED")
                    connect(tag)
                    action.invoke()
                    action::class.java.enclosingMethod?.let {
                        Log.i(
                            "ApexConnection",
                            "--ENCLOSING METHOD: ${it.name}"
                        )
                    }
                    close()
                } catch (e: Exception) {
                    close()
                    throw (e)
                }
            }
            Log.i("runSafe Mutex", "------MUTEX UNLOCKED")
        }
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}