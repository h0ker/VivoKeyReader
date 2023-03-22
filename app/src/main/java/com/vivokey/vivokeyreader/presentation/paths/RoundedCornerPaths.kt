package com.vivokey.vivokeyreader.presentation.paths

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

class RoundedCornerPaths {
    companion object {
        fun topLeftInvertedCorner(
            drawScope: DrawScope,
            width: Float,
            height: Float
        ) {
            val leftTopCornerPath = Path()
            leftTopCornerPath.apply {
                moveTo(drawScope.size.width, 0f)
                arcTo(
                    rect = Rect(
                        0f,
                        0f,
                        width * 2,
                        height * 2
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )
                lineTo(0f, 0f)
                lineTo(0f, height)
                close()
            }
            drawScope.drawPath(leftTopCornerPath, Color.DarkGray)
        }

        fun topRightInvertedCorner(
            drawScope: DrawScope,
            width: Float,
            height: Float
        ) {
            val path = Path()
            path.apply {
                moveTo(drawScope.size.width - width, 0f)
                arcTo(
                    rect = Rect(
                        offset = Offset(drawScope.size.width - (2 * width), 0f),
                        Size(width * 2, height * 2)
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -90f,
                    forceMoveTo = true
                )
                lineTo(drawScope.size.width, 0f)
                lineTo(drawScope.size.width, height)
                close()
            }

            drawScope.drawPath(path, Color.DarkGray)
        }

        fun bottomCorners(
            drawScope: DrawScope,
            height: Float
        ) {
            val path = Path()
            path.apply {
                moveTo(0f, drawScope.size.height)
                arcTo(
                    rect = Rect(
                        0f,
                        drawScope.size.height - height,
                        height * 2,
                        drawScope.size.height + height
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )
                lineTo(drawScope.size.width - height, drawScope.size.height - height)
                arcTo(
                    rect = Rect(
                        offset = Offset(drawScope.size.width - (2 * height), drawScope.size.height - height),
                        Size(height * 2, height * 2)
                    ),
                    startAngleDegrees = 270f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = true
                )
                lineTo(0f, drawScope.size.height)
                close()
            }

            drawScope.drawPath(path, Color.DarkGray)
        }
    }
}