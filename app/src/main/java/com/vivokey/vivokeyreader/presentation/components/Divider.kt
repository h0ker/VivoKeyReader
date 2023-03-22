package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable

fun Divider(
    modifier: Modifier = Modifier,
    height: Dp = 0.dp
) {
    Box(modifier = if(height == 0.dp) {
        modifier
            .fillMaxSize()
            .background(Color.DarkGray)
    } else {
        modifier
            .height(height)
            .fillMaxWidth()
            .background(Color.DarkGray)
        }
    )
}