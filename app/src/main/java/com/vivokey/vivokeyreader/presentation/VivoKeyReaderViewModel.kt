package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import android.nfc.Tag
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.hoker.lib_utils.domain.ConnectionStatus
import com.hoker.lib_utils.domain.OperationResult
import com.vivokey.lib_bluetooth.domain.models.Host
import com.vivokey.lib_bluetooth.domain.models.Message
import com.vivokey.lib_bluetooth.domain.models.MessageType
import com.vivokey.lib_nfc.domain.NfcController
import com.vivokey.lib_nfc.domain.NfcViewModel
import com.hoker.lib_utils.domain.Timer
import com.vivokey.lib_nfc.di.NfcModule
import com.vivokey.lib_nfc.domain.Consts
import com.vivokey.lib_nfc.domain.ProtocolType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.codec.binary.Hex
import javax.inject.Inject

@OptIn(FlowPreview::class)
@SuppressLint("MissingPermission")
@HiltViewModel
class VivoKeyReaderViewModel @Inject constructor(
    private val nfcControllerFactory: NfcModule.NfcControllerFactory,
    private val bluetoothController: BluetoothController,
    private val timer: Timer
): NfcViewModel, ViewModel() {

    enum class CompartmentState {
        PAIRED_DEVICE_LIST,
        BUTTONS,
        SCAN_ANIM,
        HOST_STATUS,
        MESSAGES,
        TITLE
    }

    companion object {
        private const val VPCD_CTRL_LEN: Byte = 1
        private const val VPCD_CTRL_OFF: Byte = 0
        private const val VPCD_CTRL_ON: Byte = 1
        private const val VPCD_CTRL_RESET: Byte = 2
        private const val VPCD_CTRL_ATR: Byte = 4

        val notSupported = byteArrayOf(0x6a.toByte(), 0x81.toByte())
        val error = byteArrayOf(0x64.toByte(), 0x00.toByte())
    }

    private var nfcController: NfcController? = null
    private val connectionMutex = Mutex()

    val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val bluetoothConnectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

    private val nfcConnectionStatus: StateFlow<ConnectionStatus>?
        get() { return nfcController?.connectionStatus }

    private val _compartment1State = mutableStateOf(CompartmentState.TITLE)
    var compartment1State: CompartmentState
        get() { return _compartment1State.value }
        set(value) { _compartment1State.value = value }

    private val _compartment2State = mutableStateOf(CompartmentState.PAIRED_DEVICE_LIST)
    var compartment2State: CompartmentState
        get() { return _compartment2State.value }
        set(value) { _compartment2State.value = value }

    private val _compartment3State = mutableStateOf(CompartmentState.BUTTONS)
    var compartment3State: CompartmentState
        get() { return _compartment3State.value }
        set(value) { _compartment3State.value = value }

    private val _selectedDevice: MutableState<Host?> = mutableStateOf(null)
    var selectedDevice: Host?
        get() { return _selectedDevice.value }
        set(value) { _selectedDevice.value = value }

    private val _messageLog: MutableState<List<Message?>> = mutableStateOf(listOf())
    val messageLog: List<Message?>
        get() { return _messageLog.value }

    private val _showProgressPulse = mutableStateOf(false)
    var showProgressPulse: Boolean
        get() { return _showProgressPulse.value }
        set(value) { _showProgressPulse.value = value }

    var showSection1 = MutableTransitionState(false)

    var showSection2 = MutableTransitionState(false)

    var showSection3 = MutableTransitionState(false)

    private val _colorGradientAngle = mutableStateOf(0f)
    val colorGradientAngle: Float
        get() { return _colorGradientAngle.value }

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            delay(600)
            showSection2.targetState = true
            delay(600)
            showSection1.targetState = true
            delay(600)
            showSection3.targetState = true
        }

        viewModelScope.launch {
            bluetoothConnectionStatus
                .debounce(1000)
                .collect {
                if(it == ConnectionStatus.DISCONNECTED) {
                    stopColorAnimation()
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

    private fun startColorAnimation() {

        _showProgressPulse.value = true

        if(timerJob == null) {
            timerJob = timer.getTimer(repeatMillis = 1) {
                _colorGradientAngle.value += .5f
            }
        }
        timerJob?.let {
            if(!it.isCancelled) {
                return
            }
            else {
                timerJob = timer.getTimer(repeatMillis = 1) {
                    _colorGradientAngle.value += .5f
                }
            }
        }
    }

    private suspend fun attemptBluetoothConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            _selectedDevice.value?.let {
                bluetoothController.connectOverSPP(it).listen()
            }
        }
    }

    private fun stopColorAnimation() {
        _showProgressPulse.value = false
        timerJob?.cancel()
    }

    private fun killConnections() {
        stopColorAnimation()
        bluetoothController.killConnection()
        nfcController?.close()
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

                    when(val response = nfcController?.transceive(byteArrayOf(message.first()))) {
                        is OperationResult.Success -> {
                            when(val hostResult = bluetoothController.sendMessage(response.data)) {
                                is OperationResult.Success -> {
                                    _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
                                }
                                is OperationResult.Failure -> {
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

                if(Hex.encodeHexString(message).uppercase() == Consts.GET_READER_FIRMWARE_APDU) {
                    handleReaderFirmwareRequest()
                } else if(Hex.encodeHexString(message).uppercase() == Consts.GET_UID_APDU) {
                    handleUidRequest()
                } else if(Hex.encodeHexString(message).uppercase() == Consts.GET_ATS_APDU) {
                    handleAtsRequest()
                } else if(message[4].toInt() == 1) {
                    handleSingleBytePpdu(message)
                } else {
                    when(val response = bluetoothController.sendMessage(notSupported)) {
                        is OperationResult.Success -> {
                            _messageLog.value += Message(bytes = notSupported, type = MessageType.SENT)
                        }
                        is OperationResult.Failure -> {
                            handleError(response.exception)
                        }
                    }
                }

            } else { //Standard APDUs
                when(nfcController?.getProtocolType()) {
                    ProtocolType.ISODEP -> {
                        if (message.isNotEmpty()) {
                            when(val response = nfcController?.transceive(message)) {
                                is OperationResult.Success -> {
                                    when(val hostResult = bluetoothController.sendMessage(response.data)) {
                                        is OperationResult.Success -> {
                                            _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
                                        }
                                        is OperationResult.Failure -> {
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
                    ProtocolType.NFC_A -> {
                        when(val hostResult = bluetoothController.sendMessage(notSupported)) {
                            is OperationResult.Success -> {
                                _messageLog.value += Message(bytes = notSupported, type = MessageType.SENT)
                            }
                            is OperationResult.Failure -> {
                                handleError(hostResult.exception)
                            }
                        }
                    }
                    null -> {
                        handleError()
                    }
                }
            }
        }
    }

    private suspend fun handleSingleBytePpdu(message: ByteArray) {
        when(val response = nfcController?.transceive(byteArrayOf(message[5]))) {
            is OperationResult.Success -> {
                val successResponse = response.data + byteArrayOf(0x90.toByte(), 0x00.toByte())
                when(val hostResult = bluetoothController.sendMessage(successResponse)) {
                    is OperationResult.Success -> {
                        _messageLog.value += Message(bytes = successResponse, type = MessageType.SENT)
                    }
                    is OperationResult.Failure -> {
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

    private suspend fun handleAtsRequest() {
        when(val ats = nfcController?.getATS()) {
            is OperationResult.Success -> {
                if(ats.data == null) {
                    handleError()
                } else {
                    when (val hostResult = bluetoothController.sendMessage(ats.data!!)) {
                        is OperationResult.Success -> {
                            _messageLog.value += Message(bytes = ats.data, type = MessageType.SENT)
                        }

                        is OperationResult.Failure -> {
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
                    is OperationResult.Success -> {
                        _messageLog.value += Message(bytes = error, type = MessageType.SENT)
                    }
                    is OperationResult.Failure -> {
                        handleError(hostResult.exception)
                    }
                }
            }
        }
    }

    private suspend fun handleUidRequest() {
        val uid = nfcController?.getUid()
        if(uid != null) {
            val response = uid + byteArrayOf(0x90.toByte(), 0x00.toByte())
            when(val hostResult = bluetoothController.sendMessage(response)) {
                is OperationResult.Success -> {
                    _messageLog.value += Message(bytes = response, type = MessageType.SENT)
                }
                is OperationResult.Failure -> {
                    handleError(hostResult.exception)
                }
            }
        } else {
            when(val hostResult = bluetoothController.sendMessage(error)) {
                is OperationResult.Success -> {
                    _messageLog.value += Message(bytes = error, type = MessageType.SENT)
                }
                is OperationResult.Failure -> {
                    handleError(hostResult.exception)
                }
            }
        }
    }

    private suspend fun handleReaderFirmwareRequest() {
        val firmwareVersionBytes = Hex.decodeHex(Consts.GET_READER_FIRMWARE_VERSION)
        when(val hostResult = bluetoothController.sendMessage(firmwareVersionBytes)) {
            is OperationResult.Success -> {
                _messageLog.value += Message(bytes = firmwareVersionBytes, type = MessageType.SENT)
            }
            is OperationResult.Failure -> {
                handleError(hostResult.exception)
            }
        }
    }

    private fun handleResetRequest() {
        when(val response = nfcController?.selectISD()) {
            is OperationResult.Success -> {
                _messageLog.value += Message(bytes = response.data, type = MessageType.SENT)
            }
            is OperationResult.Failure -> {
                handleError(response.exception)
            }
            null -> {
                handleError()
            }
        }
    }

    private suspend fun handleAtrRequest() {
        when(val atr = nfcController?.getATR()) {
            is OperationResult.Success -> {
                if(atr.data == null) {
                    handleError()
                } else {
                    when(val hostResult = bluetoothController.sendMessage(atr.data!!)) {
                        is OperationResult.Success -> {
                            _messageLog.value += Message(bytes = atr.data, type = MessageType.SENT)
                        }
                        is OperationResult.Failure -> {
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

    fun onScanPressed() {
        _compartment3State.value = CompartmentState.SCAN_ANIM
        _compartment2State.value = CompartmentState.HOST_STATUS
    }

    fun onSelectHostPressed() {
        _compartment3State.value = CompartmentState.BUTTONS
        _compartment2State.value = CompartmentState.PAIRED_DEVICE_LIST
    }

    override fun onTagScan(tag: Tag) {
        if(_selectedDevice.value == null) {
            return
        }

        nfcController = nfcControllerFactory.getController(tag)
        nfcController?.startConnectionCheckJob()
        observeNfcConnectionStatus()

        viewModelScope.launch(Dispatchers.IO) {
            connectionMutex.withLock {
                startColorAnimation()
                _compartment3State.value = CompartmentState.MESSAGES
                attemptBluetoothConnection()
                nfcController?.connect(tag)
            }
        }
    }

    private fun observeNfcConnectionStatus() {
        nfcController?.let { controller ->
            viewModelScope.launch {
                controller.connectionStatus
                    .debounce(1000)
                    .collect { status ->
                    connectionMutex.withLock {
                        if (status == ConnectionStatus.DISCONNECTED) {
                            stopColorAnimation()
                            killConnections()
                        }
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
        killConnections()
    }

    fun onBack() {
        if(_selectedDevice.value != null) {
            nfcController = null
            _messageLog.value = listOf()
            stopColorAnimation()
            _selectedDevice.value = null
            bluetoothController.killConnection()
            onSelectHostPressed()
        }
    }

    override fun onCleared() {
        super.onCleared()
        nfcController?.cancelConnectionCheckJob()
    }
}