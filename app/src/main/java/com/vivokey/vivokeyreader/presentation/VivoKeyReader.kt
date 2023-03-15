package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.vivokeyreader.presentation.components.HostSelection
import com.vivokey.vivokeyreader.presentation.components.TestConsole
import com.vivokey.vivokeyreader.presentation.components.TitleText
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme

@SuppressLint("MissingPermission")
@Composable
fun VivoKeyReader(viewModel: VivoKeyReaderViewModel) {

    VivoKeyReaderTheme {

        Scaffold(
            modifier = Modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                Color.Yellow,
                                Color.Magenta,
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HostSelection(
                        showHostSelection = viewModel.showHostSelection,
                        pairedHostList = viewModel.pairedDeviceList.collectAsState().value,
                        discoveredHostList = viewModel.scannedDeviceList.collectAsState().value,
                        selectedHost = viewModel.selectedDevice,
                        onHostStatusClicked = {
                            viewModel.resetHostConnection()
                        },
                        onHostSelected = { host ->
                            viewModel.onHostSelected(host)
                        },
                        connectionStatus = viewModel.connectionStatus.collectAsState().value
                    )
                    TestConsole(
                        showTestConsole = viewModel.connectionStatus.collectAsState().value == ConnectionStatus.CONNECTED,
                        inputStreamText = viewModel.inputStreamText,
                        outputStreamText = viewModel.outputStreamText,
                        sendMessage = {
                            viewModel.sendMessage(it)
                        }
                    )
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {}
                }
            }
        }
    }
}