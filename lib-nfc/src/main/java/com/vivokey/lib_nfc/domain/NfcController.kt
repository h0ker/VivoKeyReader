package com.vivokey.lib_nfc.domain

import android.nfc.Tag
import com.hoker.lib_utils.domain.ConnectionStatus
import com.hoker.lib_utils.domain.OperationResult
import kotlinx.coroutines.flow.StateFlow

interface NfcController {

    val connectionStatus: StateFlow<ConnectionStatus>
    fun connect(tag: Tag): OperationResult<Unit>
    fun close()
    fun getProtocolType(): ProtocolType
    fun getUid(): ByteArray?
    fun transceive(data: ByteArray): OperationResult<ByteArray>
    fun startConnectionCheckJob()
    fun cancelConnectionCheckJob()
    fun checkConnection(): OperationResult<Boolean>
    fun getATR(): OperationResult<ByteArray?>
    fun getATS(): OperationResult<ByteArray?>
    fun selectISD(): OperationResult<ByteArray?>
}