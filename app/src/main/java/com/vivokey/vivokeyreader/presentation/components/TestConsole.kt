package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestConsole(
    showTestConsole: MutableTransitionState<Boolean>,
    inputStreamText: String?,
    outputStreamText: String?,
    sendMessage: (String) -> Unit
) {

    Column {
        if(showTestConsole.targetState) {
            TitleText(
                text = "Input Stream:",
            )
        }
        SlidingVisibilityContent(showContent = showTestConsole) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            ) {
                inputStreamText?.let {
                    Text(
                        text = it,
                        color = Color.DarkGray
                    )
                }
            }
        }
        TitleText(
            text = "Output Stream:",
        )
        SlidingVisibilityContent(showContent = showTestConsole) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
            ) {
                TextField(
                    value = outputStreamText ?: "",
                    onValueChange = {
                        sendMessage(it.last().toString())
                    }
                )
            }
        }
    }
}