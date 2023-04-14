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
import com.vivokey.lib_nfc.domain.IsodepConnectionStatus
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
import org.apache.commons.codec.binary.Hex
import java.io.IOException
import java.lang.Exception
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class VivoKeyReaderViewModel @Inject constructor(
    private val apexController: ApexController,
    private val bluetoothController: BluetoothController,
    private val timer: Timer
): NfcViewModel, ViewModel() {

    companion object {
        private const val VPCD_CTRL_LEN: Byte = 1
        private const val VPCD_CTRL_OFF: Byte = 0
        private const val VPCD_CTRL_ON: Byte = 1
        private const val VPCD_CTRL_RESET: Byte = 2
        private const val VPCD_CTRL_ATR: Byte = 4
    }

    private val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val bluetoothConnectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

    private val _selectedDevice: MutableState<Host?> = mutableStateOf(null)
    val selectedDevice: Host?
        get() { return _selectedDevice.value }
    
    private val _messageLog: MutableState<List<Message?>> = mutableStateOf(listOf())
    val messageLog: List<Message?>
        get() { return _messageLog.value }

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

    //TODO: Fix this shit
    //TODO: Discuss Room usage here instead of paired device fetch
    private suspend fun attemptBluetoothConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            for(host: Host in pairedDeviceList.value) {
                _selectedDevice.value = host
                bluetoothController.connectOverSPP(host).listen()
            }
        }
    }

    private fun stopColorAnimation() {
        timerJob?.cancel()
    }

    private fun killConnections() {
        stopColorAnimation()
        _messageLog.value = listOf()
        _selectedDevice.value = null
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

    override fun onTagScan(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            startColorAnimation()
            attemptBluetoothConnection()
            apexController.connect(tag)
        }
    }

    fun onBack() {
        if(_selectedDevice.value != null) {
            stopColorAnimation()
            _selectedDevice.value = null
            bluetoothController.killConnection()
        }
    }
}