package com.vivokey.lib_nfc.data

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.hoker.lib_utils.domain.ConnectionStatus
import com.hoker.lib_utils.domain.OperationResult
import com.hoker.lib_utils.domain.Timer
import com.vivokey.lib_nfc.domain.NfcController
import com.vivokey.lib_nfc.domain.Consts
import com.vivokey.lib_nfc.domain.ProtocolType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.codec.binary.Hex
import javax.inject.Inject
import kotlin.experimental.xor

class IsodepControllerImpl @Inject constructor(
    private val timer: Timer
): NfcController {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = _connectionStatus.asStateFlow()

    private var isoDep: IsoDep? = null
    private var uid: ByteArray? = null
    private var timerJob: Job? = null

    override fun connect(tag: Tag): OperationResult<Unit> {
        close()
        isoDep = IsoDep.get(tag)
        isoDep?.let {
            return try {
                it.connect()
                Log.i("ApexConnection", "----ISODEP CONNECTED")
                _connectionStatus.update { ConnectionStatus.CONNECTED }
                startConnectionCheckJob()
                uid = tag.id
                OperationResult.Success(Unit)
            } catch(e: Exception) {
                OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override fun getProtocolType(): ProtocolType {
        return ProtocolType.ISODEP
    }

    override fun checkConnection(): OperationResult<Boolean> {
        return try {
            OperationResult.Success(isoDep?.isConnected ?: false)
        } catch(e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override fun startConnectionCheckJob() {
        timerJob?.cancel()
        timerJob = timer.repeatEverySecond {
            Log.i("ConnectionCheck", "ISODEP CONNECTION CHECK")
            when(val isConnected = checkConnection()) {
                is OperationResult.Success -> {
                    if(!isConnected.data) {
                        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                        timerJob?.cancel()
                    }
                }
                is OperationResult.Failure -> {
                    _connectionStatus.update { ConnectionStatus.DISCONNECTED }
                    timerJob?.cancel()
                }
            }
        }
    }

    override fun cancelConnectionCheckJob() {
        timerJob?.cancel()
    }

    override fun getATS(): OperationResult<ByteArray?> {
        isoDep?.let {
            return OperationResult.Success(it.historicalBytes)
        }
        return OperationResult.Failure()
    }

    override fun getATR(): OperationResult<ByteArray?> {
        isoDep?.let {
            try {
                if (!it.isConnected) {
                    return OperationResult.Failure()
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
                return OperationResult.Success(atr)
            } catch(e: Exception) {
                close()
                return OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override fun getUid(): ByteArray? {
        isoDep?.let {
            uid?.let {
                return it
            }
            return null
        }
        return null
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    override fun close() {
        isoDep = null
        uid = null
        Log.i("ApexConnection", "----ISODEP CLOSED")
        _connectionStatus.update { ConnectionStatus.DISCONNECTED }
    }

    override fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            OperationResult.Success(isoDep!!.transceive(data))
        } catch(e: Exception) {
            close()
            OperationResult.Failure(e)
        }
    }

    override fun selectISD(): OperationResult<ByteArray?> {
        return when(val result = transceive(Hex.decodeHex(Consts.SELECT_ISD))) {
            is OperationResult.Success -> {
                OperationResult.Success(result.data)
            }
            is OperationResult.Failure -> {
                OperationResult.Failure()
            }
        }
    }
}