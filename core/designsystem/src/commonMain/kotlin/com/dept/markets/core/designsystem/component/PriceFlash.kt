package com.dept.markets.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.TickDirection
@Composable
fun Modifier.priceFlash(
    sequence: () -> Long,
    direction: () -> TickDirection,
): Modifier {
    val colors = MarketTheme.colors
    val durationMillis = MarketTheme.metrics.flashDurationMillis
    val intensity = remember { Animatable(0f) }
    var flashColor by remember { mutableStateOf(Color.Transparent) }

    LaunchedEffect(Unit) {
        snapshotFlow { sequence() }
            .collect {
                val tickDirection = direction()
                if (tickDirection == TickDirection.FLAT) return@collect
                flashColor = if (tickDirection == TickDirection.UP) colors.upFlash else colors.downFlash
                intensity.snapTo(1f)
                intensity.animateTo(0f, animationSpec = tween(durationMillis))
            }
    }

    return drawBehind {
        val alpha = intensity.value
        if (alpha > 0.01f) {
            drawRect(color = flashColor.copy(alpha = flashColor.alpha * alpha))
        }
    }
}
