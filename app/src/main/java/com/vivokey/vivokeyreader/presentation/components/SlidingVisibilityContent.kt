package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vivokey.vivokeyreader.presentation.paths.RoundedCornerPaths

@Composable
fun SlidingVisibilityContent(
    modifier: Modifier = Modifier,
    showContent: MutableTransitionState<Boolean>,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .drawBehind {
                RoundedCornerPaths.topLeftInvertedCorner(
                    drawScope = this,
                    width = 32.dp.toPx(),
                    height = 32.dp.toPx()
                )
                RoundedCornerPaths.topRightInvertedCorner(
                    drawScope = this,
                    width = 32.dp.toPx(),
                    height = 32.dp.toPx()
                )
            }
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = LinearOutSlowInEasing
                        )
                    )
                    .fillMaxWidth(),
                visibleState = showContent,
                enter = expandVertically(
                    tween(
                        durationMillis = 400
                    )
                ),
                exit = shrinkVertically(
                    tween(
                        durationMillis = 400
                    )
                )
            ) {
                content.invoke()
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 31.dp)
            ) {
                RoundedCornerPaths.bottomCorners(
                    this,
                    height = 32.dp.toPx()
                )
            }
        }
    }
}