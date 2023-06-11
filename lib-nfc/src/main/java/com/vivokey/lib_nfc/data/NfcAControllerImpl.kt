package com.vivokey.lib_nfc.data

import android.nfc.Tag
import android.nfc.tech.NfcA
import android.util.Log
import com.hoker.lib_utils.domain.ConnectionStatus
import com.hoker.lib_utils.domain.OperationResult
import com.hoker.lib_utils.domain.Timer
import com.vivokey.lib_nfc.domain.NfcController
import com.vivokey.lib_nfc.domain.ProtocolType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.experimental.xor

class NfcAControllerImpl @Inject constructor(
    private val timer: Timer
): NfcController {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<ConnectionStatus>
        get() = _connectionStatus.asStateFlow()

    private var nfcA: NfcA? = null
    private var timerJob: Job? = null

    override fun connect(tag: Tag): OperationResult<Unit> {
        return try {
            close()
            nfcA = NfcA.get(tag)
            nfcA!!.connect()
            Log.i("ApexConnection", "----NFC_A CONNECTED")
            _connectionStatus.update { ConnectionStatus.CONNECTED }
            startConnectionCheckJob()
            OperationResult.Success(Unit)
        } catch(e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override fun getProtocolType(): ProtocolType {
        return ProtocolType.NFC_A
    }

    override fun checkConnection(): OperationResult<Boolean> {
        return try {
            OperationResult.Success(nfcA?.isConnected ?: false)
        } catch(e: Exception) {
            OperationResult.Failure(e)
        }
    }

    override fun startConnectionCheckJob() {
        timerJob?.cancel()
        timerJob = timer.repeatEverySecond {
            Log.i("ConnectionCheck", "NFC-A CONNECTION CHECK")
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

    override fun close() {
        nfcA?.let {
            nfcA = null
            Log.i("ApexConnection", "----NFC_A CLOSED")
            _connectionStatus.update { ConnectionStatus.DISCONNECTED }
        }
    }

    override fun getATR(): OperationResult<ByteArray?> {
        nfcA?.let {
            try {
                val atr = mutableListOf<Byte>()
                //Initial Header
                atr.add(0x3b.toByte())
                //T0
                atr.add(0x8f.toByte())
                //TD1
                atr.add(0x80.toByte())
                //TD2
                atr.add(0x01.toByte())
                //T1
                atr.add(0x80.toByte())
                //Application identifier presence indicator
                atr.add(0x4f.toByte())
                //length
                atr.add(0x0c.toByte())
                //RID
                atr.addAll(
                    byteArrayOf(
                        0xa0.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x03.toByte(),
                        0x06.toByte()
                    ).toList()
                )
                //Standard
                atr.add(0x03.toByte())
                //Card name (FF + SAK)
                atr.addAll(byteArrayOf(0xff.toByte(), it.sak.toByte()).toList())
                //RFU
                atr.addAll(
                    byteArrayOf(
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte(),
                        0x00.toByte()
                    ).toList()
                )
                //TCK
                var tck = atr[1]
                for (idx in 2 until atr.size) {
                    tck = tck.xor(atr[idx])
                }
                atr.add(tck)

                return OperationResult.Success(atr.toByteArray())
            } catch(e: Exception) {
                return OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override fun getATS(): OperationResult<ByteArray?> {
        nfcA?.let {
            return try {
                val uid = it.tag.id
                val sak = it.sak
                val sakByte = sak.toByte()
                OperationResult.Success(uid + sakByte)
            } catch(e: Exception) {
                OperationResult.Failure(e)
            }
        }
        return OperationResult.Failure()
    }

    override fun getUid(): ByteArray? {
        nfcA?.let {
            return it.tag.id
        }
        return null
    }

    override fun selectISD(): OperationResult<ByteArray?> {
        //don't do anything
        return OperationResult.Success(null)
    }

    override fun transceive(data: ByteArray): OperationResult<ByteArray> {
        return try {
            OperationResult.Success(nfcA!!.transceive(data))
        } catch(e: Exception) {
            close()
            OperationResult.Failure(e)
        }
    }
}