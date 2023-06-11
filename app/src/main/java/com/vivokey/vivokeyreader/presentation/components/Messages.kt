package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.hoker.bluetoothrfcomm.R
import com.vivokey.lib_bluetooth.domain.models.Message
import com.vivokey.lib_bluetooth.domain.models.MessageType
import com.vivokey.vivokeyreader.presentation.toHex
import kotlinx.coroutines.launch

@Composable
fun Messages(
    messageLog: List<Message?>
) {

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        state = listState
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
                    MessageType.ERROR -> {
                        ErrorMessage(
                            exception = it.exception
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(messageLog) {
        if(messageLog.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(messageLog.size - 1)
            }
        }
    }
}

@Composable
fun SentMessage(bytes: ByteArray?) {
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
            SelectionContainer {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = bytes?.toHex() ?: "Empty message",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun ReceivedMessage(bytes: ByteArray?) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if(bytes?.size == 1) "CTL" else "APDU",
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
            SelectionContainer {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = bytes?.toHex() ?: "Empty message",
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(
    exception: Throwable? = null
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "EXCEPTION"
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 2.dp,
                color = Color.Black
            ),
            backgroundColor = Color.Red
        ) {
            Row {
                Image(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(8.dp),
                    painter = painterResource(id = R.drawable.acid_skull),
                    contentDescription = null
                )
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    exception?.message?.let {
                        Text(
                            text = it
                        )
                    }
                }
            }
        }
    }
}