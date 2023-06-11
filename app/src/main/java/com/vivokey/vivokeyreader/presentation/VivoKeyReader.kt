package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hoker.lib_utils.domain.ConnectionStatus
import com.vivokey.vivokeyreader.presentation.components.AnimatedScanIcon
import com.vivokey.vivokeyreader.presentation.components.HostCard
import com.vivokey.vivokeyreader.presentation.components.Messages
import com.vivokey.vivokeyreader.presentation.components.SelectedHostStatus
import com.vivokey.vivokeyreader.presentation.components.ThreeCompartmentLayout
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme

@SuppressLint("MissingPermission")
@Composable
fun VivoKeyReader(
    viewModel: VivoKeyReaderViewModel = hiltViewModel()
) {

    VivoKeyReaderTheme {

        ThreeCompartmentLayout(
            showCompartment1 = viewModel.showSection1,
            showCompartment2 = viewModel.showSection2,
            showCompartment3 = viewModel.showSection3,
            colorGradientAngle = viewModel.colorGradientAngle,
            showProgressPulse = viewModel.showProgressPulse,
            compartment1 = {
                when(viewModel.compartment1State) {

                    VivoKeyReaderViewModel.CompartmentState.TITLE -> {
                        Text(
                            modifier = Modifier
                                .padding(16.dp),
                            text = if(viewModel.bluetoothConnectionStatus.collectAsState().value == ConnectionStatus.CONNECTED) {
                                "Link established"
                            } else if(viewModel.selectedDevice != null) {
                                   "Scan NFC Device"
                            } else {
                                   "Select a Host"
                            },
                            fontSize = 24.sp,
                            color = Color.DarkGray
                        )
                    }

                    else -> {}
                }
            },
            compartment2 = {

                when(viewModel.compartment2State) {

                    VivoKeyReaderViewModel.CompartmentState.PAIRED_DEVICE_LIST -> {
                        LazyColumn {
                            items(viewModel.pairedDeviceList.value) { host ->
                                host.name?.let {
                                    HostCard(host = host) {
                                        viewModel.selectedDevice = host
                                        viewModel.onScanPressed()
                                    }
                                }
                            }
                        }
                    }

                    VivoKeyReaderViewModel.CompartmentState.HOST_STATUS -> {
                        SelectedHostStatus(
                            modifier = Modifier.statusBarsPadding(),
                            selectedHost = viewModel.selectedDevice,
                            connectionStatus = viewModel.bluetoothConnectionStatus.collectAsState().value
                        ) {
                            viewModel.onSelectHostPressed()
                        }
                    }

                    else -> {}
                }
            },
            compartment3 = {
                when (viewModel.compartment3State) {

                    VivoKeyReaderViewModel.CompartmentState.MESSAGES -> {
                        Messages(viewModel.messageLog)
                    }

                    VivoKeyReaderViewModel.CompartmentState.SCAN_ANIM -> {
                        AnimatedScanIcon()
                    }

                    else -> {}
                }
            },
        )
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }