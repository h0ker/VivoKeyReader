package com.vivokey.vivokeyreader.presentation.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivokey.lib_bluetooth.domain.models.ConnectionStatus
import com.vivokey.lib_bluetooth.domain.models.Host

@SuppressLint("MissingPermission")
@Composable
fun HostSelection(
    showHostSelection: MutableTransitionState<Boolean>,
    pairedHostList: List<Host?>,
    discoveredHostList: List<Host?>,
    selectedHost: Host?,
    onHostStatusClicked: () -> Unit,
    onHostSelected: (Host) -> Unit,
    connectionStatus: ConnectionStatus
) {
    Column {

        TitleText(
            modifier = Modifier.statusBarsPadding(),
            text = "Host",
        )

        SlidingVisibilityContent(
            showContent = showHostSelection
        ) {
            AnimatedVisibility(
                visible = selectedHost == null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "Paired Hosts:",
                            fontSize = 42.sp,
                            color = Color.DarkGray
                        )
                    }
                    items(pairedHostList) { host ->
                        host?.let {
                            HostCard(host = host) {
                                onHostSelected.invoke(host)
                            }
                        }
                    }
                    item {
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "Discovered Hosts:",
                            fontSize = 42.sp,
                            color = Color.DarkGray
                        )
                    }
                    items(discoveredHostList) { host ->
                        host?.let {
                            HostCard(host = host) {
                                onHostSelected.invoke(host)
                            }
                        }
                    }
                    item {
                        Spacer(
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = selectedHost != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SelectedHostStatus(
                    modifier = Modifier.clickable {
                        onHostStatusClicked.invoke()
                    },
                    selectedHost = selectedHost,
                    connectionStatus = connectionStatus
                )
            }
        }
    }
}