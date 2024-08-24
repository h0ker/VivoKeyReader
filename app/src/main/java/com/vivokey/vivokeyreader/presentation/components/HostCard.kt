package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbidecowboy.supra.presentation.cards.SupraTextureCard
import com.carbidecowboy.supra.presentation.cards.TextureType
import com.vivokey.lib_bluetooth.domain.models.Host

@Composable
fun HostCard(
    host: Host,
    onSelected: () -> Unit
) {
    SupraTextureCard(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                onSelected.invoke()
            },
        textureType = TextureType.TOPOGRAPHIC,
        backgroundColor = MaterialTheme.colorScheme.secondary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = host.name ?: "Unknown",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp
            )
            Text(
                modifier = Modifier.padding(
                    start = 8.dp,
                    bottom = 8.dp
                ),
                text = host.address,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
        }
    }
}