package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import android.nfc.Tag
import android.os.CountDownTimer
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host
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
import javax.inject.Inject

@SuppressLint("MissingPermission")
@HiltViewModel
class VivoKeyReaderViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val timer: Timer
): NfcViewModel, ViewModel() {

    private val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val connectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

    private val _selectedDevice: MutableState<Host?> = mutableStateOf(null)
    val selectedDevice: Host?
        get() { return _selectedDevice.value }
    
    private val _inputBytes: MutableState<ByteArray> = mutableStateOf(byteArrayOf())
    var inputBytes: ByteArray
        get() { return _inputBytes.value }
        set(value) { _inputBytes.value = value }

    private val _outputStreamText: MutableState<String?> = mutableStateOf(null)
    var outputStreamText: String?
        get() { return _outputStreamText.value }
        set(value) { _outputStreamText.value = value }

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

    fun resetHostConnection() {
        _selectedDevice.value = null
        bluetoothController.killConnection()
    }

    fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if(bluetoothController.trySendMessage(message)) {
                //change this to = message if sending the whole thing and not just a character
                _outputStreamText.value += message
            }
        }
    }

    fun onHostSelected(host: Host) {
        _selectedDevice.value = host
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothController.connectOverSPP(host).listen()
        }
    }

    private fun Flow<ByteArray?>.listen(): Job {
        return onEach { result ->
            println("TEST")
            if (result != null) {
                _inputBytes.value += result
            }
        }.launchIn(viewModelScope)
    }

    fun startColorAnimation() {
        if(timerJob == null) {
            timerJob = timer.getTimer(repeatMillis = 1) {
                _colorGradientAngle.value += 1
            }
        }
        timerJob?.let {
            if(!it.isCancelled) {
                return
            }
            else {
                timerJob = timer.getTimer(repeatMillis = 1) {
                    _colorGradientAngle.value += 1
                }
            }
        }
    }

    //TODO: Fix this shit
    private suspend fun attemptBluetoothConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            for(host: Host in pairedDeviceList.value) {
                _selectedDevice.value = host
                bluetoothController.connectOverSPP(host).listen()

                var delayTotal: Long = 1000
                if(connectionStatus.value != ConnectionStatus.CONNECTED) {
                    while (delayTotal < 4000) {
                        delay(delayTotal)
                        delayTotal += 1000
                    }
                    continue
                } else {
                    break
                }
            }
        }
    }

    fun stopColorAnimation() {
        timerJob?.cancel()
    }

    override fun onTagScan(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            startColorAnimation()
            attemptBluetoothConnection()
            stopColorAnimation()
        }
    }
}