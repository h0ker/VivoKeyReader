package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun PermissionAlert(
    onRequestPermissions: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        Text(
            modifier = Modifier.statusBarsPadding(),
            text = "Permissions Required",
            fontSize = 24.sp,
            color = Color.Yellow
        )
        Button(
            modifier = Modifier.statusBarsPadding(),
            onClick = {
                onRequestPermissions.invoke()
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Yellow
            )
        ) {
            Text(
                text = "Enable Permissions",
                color = Color.Black,
                fontSize = 12.sp
            )
        }
    }
}