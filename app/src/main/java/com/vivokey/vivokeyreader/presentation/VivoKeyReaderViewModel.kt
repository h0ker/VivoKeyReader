package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivokey.lib_bluetooth.domain.models.BluetoothController
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
): ViewModel() {

    val pairedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.pairedDevices }

    val scannedDeviceList: StateFlow<List<Host>>
        get() { return bluetoothController.scannedDevices }

    val connectionStatus: StateFlow<ConnectionStatus>
        get() { return bluetoothController.connectionStatus }

    private val _selectedDevice: MutableState<Host?> = mutableStateOf(null)
    val selectedDevice: Host?
        get() { return _selectedDevice.value }
    
    private val _showHostSelection = mutableStateOf(true)
    var showHostSelection: Boolean
        get() { return _showHostSelection.value }
        set(value) { _showHostSelection.value = value }

    private val _inputStreamText: MutableState<String?> = mutableStateOf(null)
    var inputStreamText: String?
        get() { return _inputStreamText.value ?: "" }
        set(value) { _inputStreamText.value = value }

    private val _outputStreamText: MutableState<String?> = mutableStateOf(null)
    var outputStreamText: String?
        get() { return _outputStreamText.value }
        set(value) { _outputStreamText.value = value }

    init {
        startScan()
    }

    private fun startScan() {
        bluetoothController.startDiscovery()
    }

    private fun stopScan() {
        bluetoothController.stopDiscovery()
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
        stopScan()
        _selectedDevice.value = host
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothController.connectOverSPP(host).listen()
        }
    }

    private fun Flow<String?>.listen(): Job {
        return onEach { result ->
            _inputStreamText.value += result
        }.launchIn(viewModelScope)
    }
}