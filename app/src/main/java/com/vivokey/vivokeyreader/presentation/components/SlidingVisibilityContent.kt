package com.vivokey.vivokeyreader.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SlidingVisibilityContent(
    modifier: Modifier = Modifier,
    showContent: MutableTransitionState<Boolean>,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
            .clip(RoundedCornerShape(16.dp))
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
                    .background(Color.LightGray)
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
        }
    }
}