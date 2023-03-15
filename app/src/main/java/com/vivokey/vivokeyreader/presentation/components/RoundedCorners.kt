package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun RoundedCorners(
    show: Boolean = true,
    inverted: Boolean = false
) {
    if(show && !inverted) {
        Box(
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val path = Path()
                path.apply {
                    moveTo(0f, size.height)
                    arcTo(
                        rect = Rect(
                            0f,
                            0f,
                            size.height * 2,
                            size.height * 2
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = true
                    )
                    lineTo(size.width - size.height, 0f)
                    arcTo(
                        rect = Rect(
                            offset = Offset(size.width - 64.dp.toPx(), 0f),
                            Size(size.height * 2, size.height * 2)
                        ),
                        startAngleDegrees = 270f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = true
                    )
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(path, Color.Black)
            }
        }
    }
    if(show && inverted) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
            ) {
                val path = Path()
                path.apply {
                    moveTo(size.width, 0f)
                    arcTo(
                        rect = Rect(
                            0f,
                            0f,
                            size.width * 2,
                            size.height * 2
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 90f,
                        forceMoveTo = true
                    )
                    lineTo(0f, 0f)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(path, Color.Black)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
            ) {
                val path = Path()
                path.apply {
                    moveTo(0f, 0f)
                    arcTo(
                        rect = Rect(
                            offset = Offset(-32.dp.toPx(), 0f),
                            Size(size.width * 2, size.height * 2)
                        ),
                        startAngleDegrees = 0f,
                        sweepAngleDegrees = -90f,
                        forceMoveTo = true
                    )
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    close()
                }

                drawPath(path, Color.Black)
            }
        }
    }
}