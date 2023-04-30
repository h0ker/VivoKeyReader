package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import android.nfc.Tag
import android.util.Log
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host
import com.vivokey.lib_bluetooth.domain.models.Message
import com.vivokey.lib_bluetooth.domain.models.MessageType
import com.vivokey.lib_nfc.domain.ApexController
import com.vivokey.lib_nfc.domain.NfcViewModel
import com.vivokey.vivokeyreader.domain.models.Timer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class VivoKeyReaderViewModel @Inject constructor(
    private val apexController: ApexController,
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
    }

    val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val bluetoothConnectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

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
    }

    private fun Flow<ByteArray?>.listen(): Job {
        return onEach { message ->
            if (message != null) {
                _messageLog.value += Message(message, MessageType.RECEIVED)
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
        apexController.close()
    }

    private suspend fun handleMessage(message: ByteArray) {
        //CTL byte check
        if(message.size == 1) {
            when(message.first()) {
                VPCD_CTRL_ATR -> {
                    try {
                        val atr = apexController.getATR()
                        if(atr == null) {
                            killConnections()
                        }
                        atr?.let {
                            bluetoothController.trySendMessage(it)
                            _messageLog.value += Message(it, MessageType.SENT)
                        }
                    } catch(e: Exception) {
                        Log.i("handleMessage() - ATR", e.message ?: e.toString())
                        killConnections()
                    }
                }
                VPCD_CTRL_RESET -> {
                    try {
                        val response = apexController.selectISD()
                        response?.let {
                            //bluetoothController.trySendMessage(it)
                            _messageLog.value += Message(it, MessageType.SENT)
                        }
                    } catch(e: Exception) {
                        Log.i("handleMessage() - RESET", e.message ?: e.toString())
                        killConnections()
                    }
                }
            }
        } else { //APDU
            try {
                if(message.isNotEmpty()) {
                    val response = apexController.transceive(message)
                    bluetoothController.trySendMessage(response)
                    _messageLog.value += Message(response, MessageType.SENT)
                }
            } catch(e: Exception) {
                Log.i("handleMessage() - APDU", e.message ?: e.toString())
                killConnections()
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
        viewModelScope.launch(Dispatchers.IO) {
            startColorAnimation()
            _compartment3State.value = CompartmentState.MESSAGES
            attemptBluetoothConnection()
            apexController.connect(tag)
        }
    }

    fun onBack() {
        if(_selectedDevice.value != null) {
            _messageLog.value = listOf()
            stopColorAnimation()
            _selectedDevice.value = null
            bluetoothController.killConnection()
            onSelectHostPressed()
        }
    }
}