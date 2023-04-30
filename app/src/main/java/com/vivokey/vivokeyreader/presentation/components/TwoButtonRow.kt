package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun TwoButtonRow(
    text1: String,
    text2: String,
    buttonOneClicked: () -> Unit,
    buttonTwoClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TextButton(
            onClick = {
                buttonOneClicked()
            }
        ) {
            Text(
                text = text1,
                color = Color.DarkGray,
            )
        }
        TextButton(
            onClick = {
                buttonTwoClicked()
            }
        ) {
            Text(
                text = text2,
                color = Color.DarkGray,
            )
        }
    }
}