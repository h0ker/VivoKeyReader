package com.vivokey.vivokeyreader.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.carbidecowboy.supra.presentation.scaffolds.SupraGyroScaffold
import com.hoker.bluetoothrfcomm.R
import com.vivokey.vivokeyreader.presentation.components.ConnectionStatusIndicator
import com.vivokey.vivokeyreader.presentation.components.HostCard
import com.vivokey.vivokeyreader.presentation.components.Messages
import com.vivokey.vivokeyreader.presentation.components.SelectedHostStatus
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme

@SuppressLint("MissingPermission")
@Composable
fun VivoKeyReader(
    viewModel: VivoKeyReaderViewModel = hiltViewModel()
) {

    val bluetoothConnectionStatus = viewModel.bluetoothConnectionStatus.collectAsState()

    VivoKeyReaderTheme {

        SupraGyroScaffold(
            borderColor = MaterialTheme.colorScheme.surfaceVariant,
            backgroundColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .statusBarsPadding()
                        .height(40.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_monochrome),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(end = 16.dp),
                        text = "SMART READER",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            bottomBar = {
                if (viewModel.selectedDevice == null) {
                    Text(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(start = 16.dp, bottom = 16.dp),
                        text = "Select a Host",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SelectedHostStatus(
                            selectedHost = viewModel.selectedDevice
                        ) {
                            viewModel.onBack()
                        }
                        ConnectionStatusIndicator(connectionStatus = bluetoothConnectionStatus.value)
                    }
                }
            }
        ) {

            Column {
                if (viewModel.selectedDevice == null) {
                    LazyColumn {
                        items(viewModel.pairedDeviceList.value) { host ->
                            host.name?.let {
                                HostCard(host = host) {
                                    viewModel.selectedDevice = host
                                }
                            }
                        }
                    }
                }
                if (viewModel.selectedDevice != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Comm History",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 24.sp
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            thickness = 2.dp
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (viewModel.messageLog.isEmpty()) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "Scan your NFC device",
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Messages(
                            messageLog = viewModel.messageLog
                        )
                    }
                }
            }
        }
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }