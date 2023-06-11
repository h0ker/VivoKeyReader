package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vivokey.vivokeyreader.ui.theme.VivoKeyReaderTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun ThreeCompartmentLayout(
    modifier: Modifier = Modifier,
    showCompartment1: MutableTransitionState<Boolean>,
    showCompartment2: MutableTransitionState<Boolean>,
    showCompartment3: MutableTransitionState<Boolean>,
    compartment1: @Composable () -> Unit,
    compartment2: @Composable () -> Unit,
    compartment3: @Composable () -> Unit,
    showProgressPulse: Boolean,
    colorGradientAngle: Float
) {
    VivoKeyReaderTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .gradientBackground(
                    colors = listOf(
                        if (showProgressPulse) Color.Yellow else Color.DarkGray,
                        Color.DarkGray,
                    ),
                    angle = colorGradientAngle
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.DarkGray)
                    .animateContentSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SlidingVisibilityContent(
                    modifier = Modifier.padding(top = 32.dp),
                    showContent = showCompartment1
                ) {
                    compartment1()
                }
                Divider(
                    height = 20.dp
                )
                SlidingVisibilityContent(showContent = showCompartment2) {
                    compartment2()
                }
                Divider(
                    height = 20.dp
                )
                SlidingVisibilityContent(showContent = showCompartment3) {
                    compartment3()
                }
                /*
                Image(
                    modifier = Modifier.size(64.dp),
                    painter = painterResource(id = R.drawable.monochrome_vivokey),
                    contentDescription = null,
                )
                 */
            }
        }
    }
}

fun Modifier.gradientBackground(colors: List<Color>, angle: Float) = this.then(
    Modifier.drawBehind {
        val angleRad = angle/180f * PI
        val x = cos(angleRad).toFloat()
        val y = sin(angleRad).toFloat()

        val radius = sqrt(size.width.pow(2) + size.height.pow(2)) / 2f
        val offset = center + Offset(x * radius, y * radius)

        val exactOffset = Offset(
            x = min(offset.x.coerceAtLeast(0f), size.width),
            y = size.height - min(offset.y.coerceAtLeast(0f), size.height)
        )

        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(size.width, size.height) - exactOffset,
                end = exactOffset
            ),
            size = size
        )
    }
)