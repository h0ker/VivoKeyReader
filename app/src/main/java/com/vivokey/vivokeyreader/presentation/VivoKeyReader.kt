package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_nfc.domain.IsodepConnectionStatus
import com.vivokey.vivokeyreader.presentation.components.AnimatedScanIcon
import com.vivokey.vivokeyreader.presentation.components.Messages
import com.vivokey.vivokeyreader.presentation.components.SelectedHostStatus
import com.vivokey.vivokeyreader.presentation.components.ThreeCompartmentLayout
import com.vivokey.vivokeyreader.presentation.components.TwoButtonRow
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationGraphicsApi::class)
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
            compartment1 = {
                Text(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp),
                    text = if(viewModel.bluetoothConnectionStatus.collectAsState().value != ConnectionStatus.CONNECTED) "Scan your Apex" else "Uplink established",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 24.sp,
                    color = Color.DarkGray
                )
            },
            compartment2 = {
                if(viewModel.bluetoothConnectionStatus.collectAsState().value != ConnectionStatus.DISCONNECTED) {
                    SelectedHostStatus(
                        modifier = Modifier.statusBarsPadding(),
                        selectedHost = viewModel.selectedDevice,
                        connectionStatus = viewModel.bluetoothConnectionStatus.collectAsState().value
                    )
                } else {
                    AnimatedScanIcon()
                }
            },
            compartment3 = {
                if(viewModel.bluetoothConnectionStatus.collectAsState().value == ConnectionStatus.DISCONNECTED) {
                    TwoButtonRow(
                        buttonOneClicked = {
                        },
                        buttonTwoClicked = {
                        }
                    )
                } else {
                    Messages(viewModel.messageLog)
                }
            },
            colorGradientAngle = viewModel.colorGradientAngle
        )
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }