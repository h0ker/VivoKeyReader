package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hoker.lib_utils.domain.ConnectionStatus

@Composable
fun ConnectionStatusIndicator(
    connectionStatus: ConnectionStatus,
) {

    val connectingColor = rememberInfiniteTransition(label = "").animateColor(
        initialValue = Color.Black,
        targetValue = Color.Yellow,
        animationSpec = infiniteRepeatable(
            // Tween animation for smooth color transition
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            // RepeatMode.Reverse will animate back and forth
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val indicatorColor = when (connectionStatus) {
        ConnectionStatus.CONNECTED -> Color.Green
        ConnectionStatus.CONNECTING -> connectingColor.value
        ConnectionStatus.DISCONNECTED -> Color.Red
    }

    Row(
        modifier = Modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = connectionStatus.textStatus,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 14.sp
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(indicatorColor)
        )
    }
}