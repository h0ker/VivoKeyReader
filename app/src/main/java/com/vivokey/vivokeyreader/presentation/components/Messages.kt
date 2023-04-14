package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vivokey.lib_bluetooth.domain.models.Message
import com.vivokey.lib_bluetooth.domain.models.MessageType
import com.vivokey.vivokeyreader.presentation.toHex

@Composable
fun Messages(
    messageLog: List<Message?>
) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        items(messageLog) { message ->
            message?.let {
                when(it.type) {
                    MessageType.SENT -> {
                        SentMessage(bytes = it.bytes)
                    }
                    MessageType.RECEIVED -> {
                        ReceivedMessage(bytes = it.bytes)
                    }
                }
            }
        }
    }
}

@Composable
fun SentMessage(bytes: ByteArray) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 2.dp,
                color = Color.Black
            ),
            backgroundColor = Color.Green
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = bytes.toHex(),
                color = Color.Black
            )
        }
    }
}

@Composable
fun ReceivedMessage(bytes: ByteArray) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if(bytes.size == 1) "CTL" else "APDU",
            color = Color.Black
        )
        Card(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 2.dp,
                color = Color.Black
            ),
            backgroundColor = Color.Cyan
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = bytes.toHex(),
                color = Color.Black
            )
        }
    }
}