package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import android.nfc.Tag
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.carbidecowboy.intra.data.IsodepControllerImpl
import com.carbidecowboy.intra.di.NfcModule
import com.carbidecowboy.intra.domain.NfcAdapterController
import com.carbidecowboy.intra.domain.NfcController
import com.carbidecowboy.intra.domain.NfcViewModel
import com.carbidecowboy.intra.domain.OperationResult
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.hoker.lib_utils.domain.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host
import com.vivokey.lib_bluetooth.domain.models.Message
import com.vivokey.lib_bluetooth.domain.models.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.apache.commons.codec.binary.Hex
import javax.inject.Inject

@OptIn(FlowPreview::class)
@SuppressLint("MissingPermission")
@HiltViewModel
class VivoKeyReaderViewModel @Inject constructor(
    nfcAdapterController: NfcAdapterController,
    nfcControllerFactory: NfcModule.NfcControllerFactory,
    private val bluetoothController: BluetoothController
): NfcViewModel(nfcAdapterController, nfcControllerFactory) {

    companion object {
        private const val VPCD_CTRL_LEN: Byte = 1
        private const val VPCD_CTRL_OFF: Byte = 0
        private const val VPCD_CTRL_ON: Byte = 1
        private const val VPCD_CTRL_RESET: Byte = 2
        private const val VPCD_CTRL_ATR: Byte = 4

        const val SUCCESS_RESPONSE = "9000"
        const val SELECT_CODE = "00A40400"
        const val ISD_AID = "A000000151000000"
        const val SELECT_ISD = "00A4040008A00000015100000000"
        const val GET_READER_FIRMWARE_APDU = "FF00480000"
        const val GET_READER_FIRMWARE_VERSION = "5669766F4B6579536D6172745265616465725F56312E30"
        const val PPDU_DIRECT_TRANSMIT_PREFIX = "FF000000"
        const val PPDU_SPRINGCARD_ENCAPSULATE_PREFIX = "FFFE"
        const val GET_UID_APDU = "FFCA000000"
        const val GET_ATS_APDU = "FFCA010000"
        const val TEST_NFC_A_ATR = "3B8F8001804F0CA0000003060300030000000068"

        val notSupported = byteArrayOf(0x6a.toByte(), 0x81.toByte())
        val error = byteArrayOf(0x64.toByte(), 0x00.toByte())
        val wrongLength = byteArrayOf(0x67.toByte(), 0x00.toByte())
        val commandAborted = byteArrayOf(0x6f.toByte(), 0x00.toByte())
    }

    private var _nfcController: NfcController? = null

    val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val bluetoothConnectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

    private val _selectedDevice: MutableState<Host?> = mutableStateOf(null)
    var selectedDevice: Host?
        get() { return _selectedDevice.value }
        set(value) { _selectedDevice.value = value }

    private val _uid: MutableState<ByteArray?> = mutableStateOf(null)

    private val _messageLog: MutableState<List<Message?>> = mutableStateOf(listOf())
    val messageLog: List<Message?>
        get() { return _messageLog.value }

    init {
        viewModelScope.launch {
            bluetoothConnectionStatus
                .debounce(1000)
                .collect {
                if(it == ConnectionStatus.DISCONNECTED) {
                    killConnections()
                }
            }
        }

        observeNfcConnectionStatus()
    }

    private fun Flow<ByteArray?>.listen(): Job {
        return onEach { message ->
            if (message != null) {
                _messageLog.value += Message(bytes = message, type = MessageType.RECEIVED)
                handleMessage(message)
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun attemptBluetoothConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedDevice.value?.let {
                bluetoothController.connectOverSPP(it).listen()
            }
        }
    }

    private suspend fun killConnections() {
        bluetoothController.killConnection()
        _nfcController?.close()
        _nfcController = null
        _uid.value = null
    }

    private fun checkForDirectTransmit(message: ByteArray): Boolean {
        return try {
            Hex.encodeHexString(message.copyOfRange(0, 4), false) == PPDU_DIRECT_TRANSMIT_PREFIX
        } catch (e: Exception) {
            false
        }
    }

    private fun checkForSpringCardEncapsulate(message: ByteArray): Boolean {
        return try {
            Hex.encodeHexString(message.copyOfRange(0, 2), false) == PPDU_SPRINGCARD_ENCAPSULATE_PREFIX
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun handleMessage(message: ByteArray) {
        //CTL byte check
        if(message.size == 1) {
            when(message.first()) {
                VPCD_CTRL_ATR -> {
                    handleAtrRequest()
                }
                VPCD_CTRL_RESET -> {
                    handleResetRequest()
                }
                else -> {

                    when(val response = _nfcController?.transceive(byteArrayOf(message.first()))) {
                        is OperationResult.Success -> {
                            when(val hostResult = bluetoothController.sendMessage(response.data)) {
                                is com.hoker.lib_utils.domain.OperationResult.Success -> {
                                    _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
                                }
                                is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                                    handleError(hostResult.exception)
                                }
                            }
                        }
                        is OperationResult.Failure -> {
                            handleError(response.exception)
                        }
                        null -> {
                            handleError()
                        }
                    }
                }
            }
        } else { //APDU
            //PPDUs
            if(message.first() == 0xff.toByte()) {

                if (Hex.encodeHexString(message).uppercase() == GET_READER_FIRMWARE_APDU) {
                    handleReaderFirmwareRequest()
                } else if (Hex.encodeHexString(message).uppercase() == GET_UID_APDU) {
                    handleUidRequest()
                } else if (Hex.encodeHexString(message).uppercase() == GET_ATS_APDU) {
                    handleAtsRequest()
                } else if (checkForDirectTransmit(message)) {
                    handlePassthroughPpdu(message)
                } else if (checkForSpringCardEncapsulate(message)) {
                    handlePassthroughPpdu(message)
                }
                else {
                    when(val response = bluetoothController.sendMessage(notSupported)) {
                        is com.hoker.lib_utils.domain.OperationResult.Success -> {
                            _messageLog.value += Message(bytes = notSupported, type = MessageType.SENT)
                        }
                        is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                            handleError(response.exception)
                        }
                    }
                }

            } else { //Standard APDUs
                when(_nfcController) {
                    is IsodepControllerImpl -> {
                        if (message.isNotEmpty()) {
                            when(val response = _nfcController?.transceive(message)) {
                                is OperationResult.Success -> {
                                    when(val hostResult = bluetoothController.sendMessage(response.data)) {
                                        is com.hoker.lib_utils.domain.OperationResult.Success -> {
                                            _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
                                        }
                                        is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                                            handleError(hostResult.exception)
                                        }
                                    }
                                }
                                is OperationResult.Failure -> {
                                    handleError(response.exception)
                                }
                                null -> {
                                    handleError()
                                }
                            }
                        }
                    }
                    null -> {
                        handleError()
                    }
                    else -> {
                        when(val hostResult = bluetoothController.sendMessage(notSupported)) {
                            is com.hoker.lib_utils.domain.OperationResult.Success -> {
                                _messageLog.value += Message(bytes = notSupported, type = MessageType.SENT)
                            }
                            is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                                handleError(hostResult.exception)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handlePassthroughPpdu(message: ByteArray) {

        try {

            val length = message[4].toInt()
            val payload = message.copyOfRange(5, message.size)

            //Get size of payload only
            if (length != message.size - 5) {
                _messageLog.value += Message(bytes = wrongLength, type = MessageType.SENT)
                return
            }

            when(val response = _nfcController?.transceive((payload))) {
                is OperationResult.Success -> {
                    val successResponse = response.data + byteArrayOf(0x90.toByte(), 0x00.toByte())
                    when(val hostResult = bluetoothController.sendMessage(successResponse)) {
                        is com.hoker.lib_utils.domain.OperationResult.Success -> {
                            _messageLog.value += Message(bytes = successResponse, type = MessageType.SENT)
                        }
                        is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                            handleError(hostResult.exception)
                        }
                    }
                }
                is OperationResult.Failure -> {
                    _messageLog.value += Message(bytes = commandAborted, type = MessageType.SENT)
                    bluetoothController.sendMessage(commandAborted)
                    handleError(response.exception)
                }
                null -> {}
            }

        } catch (e: Exception) {
            _messageLog.value += Message(bytes = wrongLength, type = MessageType.SENT)
        }
    }

    private suspend fun handleAtsRequest() {
        when(val ats = _nfcController?.getAts()) {
            is OperationResult.Success -> {
                if(ats.data == null) {
                    handleError()
                } else {
                    when (val hostResult = bluetoothController.sendMessage(ats.data!!)) {
                        is com.hoker.lib_utils.domain.OperationResult.Success -> {
                            _messageLog.value += Message(bytes = ats.data, type = MessageType.SENT)
                        }

                        is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                            handleError(hostResult.exception)
                        }
                    }
                }
            }
            is OperationResult.Failure -> {
                handleError(ats.exception)
            }
            null -> {
                when(val hostResult = bluetoothController.sendMessage(error)) {
                    is com.hoker.lib_utils.domain.OperationResult.Success -> {
                        _messageLog.value += Message(bytes = error, type = MessageType.SENT)
                    }
                    is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                        handleError(hostResult.exception)
                    }
                }
            }
        }
    }

    private suspend fun handleUidRequest() {
        _uid.value?.let { validUid ->
            val response = validUid + byteArrayOf(0x90.toByte(), 0x00.toByte())
            when(val hostResult = bluetoothController.sendMessage(response)) {
                is com.hoker.lib_utils.domain.OperationResult.Success -> {
                    _messageLog.value += Message(bytes = response, type = MessageType.SENT)
                }
                is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                    handleError(hostResult.exception)
                }
            }
        }
        if (_uid.value == null) {
            when(val hostResult = bluetoothController.sendMessage(error)) {
                is com.hoker.lib_utils.domain.OperationResult.Success -> {
                    _messageLog.value += Message(bytes = error, type = MessageType.SENT)
                }
                is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                    handleError(hostResult.exception)
                }
            }
        }
    }

    private suspend fun handleReaderFirmwareRequest() {
        val firmwareVersionBytes = Hex.decodeHex(GET_READER_FIRMWARE_VERSION)
        when(val hostResult = bluetoothController.sendMessage(firmwareVersionBytes)) {
            is com.hoker.lib_utils.domain.OperationResult.Success -> {
                _messageLog.value += Message(bytes = firmwareVersionBytes, type = MessageType.SENT)
            }
            is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                handleError(hostResult.exception)
            }
        }
    }

    private suspend fun handleResetRequest() {
        when(val response = _nfcController?.selectISD()) {
            is com.hoker.lib_utils.domain.OperationResult.Success -> {
                response.data?.let {
                    _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
                }
            }
            is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                handleError(response.exception)
            }
            null -> {
                handleError()
            }
        }
    }

    private suspend fun NfcController.selectISD(): com.hoker.lib_utils.domain.OperationResult<ByteArray?> {
        return when(val result = this.transceive(Hex.decodeHex(SELECT_ISD))) {
            is OperationResult.Success -> {
                com.hoker.lib_utils.domain.OperationResult.Success(result.data)
            }
            is OperationResult.Failure -> {
                com.hoker.lib_utils.domain.OperationResult.Failure()
            }
            else -> com.hoker.lib_utils.domain.OperationResult.Failure()
        }
    }

    private suspend fun handleAtrRequest() {
        when(val atr = _nfcController?.getAtr()) {
            is OperationResult.Success -> {
                if(atr.data == null) {
                    handleError()
                } else {
                    when(val hostResult = bluetoothController.sendMessage(atr.data!!)) {
                        is com.hoker.lib_utils.domain.OperationResult.Success -> {
                            _messageLog.value += Message(bytes = atr.data, type = MessageType.SENT)
                        }
                        is com.hoker.lib_utils.domain.OperationResult.Failure -> {
                            handleError(hostResult.exception)
                        }
                    }
                }
            }
            is OperationResult.Failure -> {
                handleError(atr.exception)
            }
            null -> {
                handleError()
            }
        }
    }

    private fun observeNfcConnectionStatus() {
        _nfcController?.let { controller ->
            viewModelScope.launch {
                controller.connectionStatus
                    .debounce(1000)
                    .collect { status ->
                        if (!status) {
                            killConnections()
                        }
                    }
            }
        }
    }

    private fun handleError(e: Throwable? = null) {
        e?.let {
            _messageLog.value += Message(
                exception = e,
                type = MessageType.ERROR
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            killConnections()
        }
    }

    fun onBack() {
        if(_selectedDevice.value != null) {
            _nfcController = null
            _messageLog.value = listOf()
            _selectedDevice.value = null
            bluetoothController.killConnection()
        }
    }

    override fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController) {
        if(_selectedDevice.value == null) {
            return
        }

        _nfcController = nfcController
        _uid.value = tag.id

        viewModelScope.launch(Dispatchers.IO) {
            observeNfcConnectionStatus()
            attemptBluetoothConnection()
            nfcController.connect(tag)
        }
    }
}