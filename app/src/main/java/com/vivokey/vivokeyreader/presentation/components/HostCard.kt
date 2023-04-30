package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivokey.lib_bluetooth.domain.models.Host

@Composable
fun HostCard(
    host: Host,
    onSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable {
                onSelected.invoke()
            }
            .padding(8.dp),
        backgroundColor = Color.DarkGray,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = host.name ?: "Unknown",
                color = Color.Yellow,
                fontSize = 24.sp
            )
            Text(
                modifier = Modifier.padding(8.dp),
                text = host.address,
                color = Color.Yellow,
                fontSize = 24.sp
            )
        }
    }
}