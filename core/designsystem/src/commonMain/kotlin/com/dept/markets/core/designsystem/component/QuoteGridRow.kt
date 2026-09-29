package com.dept.markets.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val QuoteGridWeights: FloatArray = floatArrayOf(0.26f, 0.18f, 0.16f, 0.14f, 0.12f, 0.14f)

@Composable
fun QuoteGridRow(
    modifier: Modifier = Modifier,
    weights: FloatArray = QuoteGridWeights,
    height: Dp = 44.dp,
    horizontalPadding: Dp = 12.dp,
    columnSpacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        require(measurables.size == weights.size) {
            "QuoteGridRow expects ${weights.size} children, got ${measurables.size}"
        }
        val rowHeight = height.roundToPx()
        val padding = horizontalPadding.roundToPx()
        val spacing = columnSpacing.roundToPx()
        val totalWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val contentWidth = (totalWidth - padding * 2 - spacing * (weights.size - 1)).coerceAtLeast(0)

        var consumed = 0
        val placeables = ArrayList<androidx.compose.ui.layout.Placeable>(measurables.size)
        measurables.forEachIndexed { index, measurable ->

            val columnWidth = if (index == measurables.lastIndex) {
                contentWidth - consumed
            } else {
                (contentWidth * weights[index]).toInt()
            }
            consumed += columnWidth
            placeables += measurable.measure(
                Constraints.fixed(width = columnWidth.coerceAtLeast(0), height = rowHeight),
            )
        }

        layout(totalWidth, rowHeight) {
            var x = padding
            placeables.forEach { placeable ->
                placeable.placeRelative(x = x, y = 0)
                x += placeable.width + spacing
            }
        }
    }
}
