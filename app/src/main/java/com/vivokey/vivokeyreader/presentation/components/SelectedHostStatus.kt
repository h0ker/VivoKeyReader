package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vivokey.lib_bluetooth.domain.models.Host

@Composable
fun SelectedHostStatus(
    modifier: Modifier = Modifier,
    selectedHost: Host?,
    onCancel: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                onCancel()
            }
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = selectedHost?.name ?: "Unknown",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 22.sp
            )
            Text(
                text = selectedHost?.address ?: "",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }
    }
}