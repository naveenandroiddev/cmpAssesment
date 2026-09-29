package com.dept.markets.core.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Stable
class SparklineSeries(val capacity: Int = 64) {

    private val values = FloatArray(capacity)
    private var head = 0
    private var size = 0

    var version by mutableIntStateOf(0)
        private set

    var minimum: Float = Float.MAX_VALUE
        private set
    var maximum: Float = -Float.MAX_VALUE
        private set

    fun push(value: Float) {
        values[head] = value
        head = (head + 1) % capacity
        if (size < capacity) size++
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (index in 0 until size) {
            val candidate = values[(head - size + index + capacity) % capacity]
            if (candidate < min) min = candidate
            if (candidate > max) max = candidate
        }
        minimum = min
        maximum = max
        version++
    }

    fun count(): Int = size

    fun valueAt(index: Int): Float = values[(head - size + index + capacity) % capacity]
}

@Composable
fun rememberSparklineSeries(capacity: Int = 64): SparklineSeries =
    remember(capacity) { SparklineSeries(capacity) }

@Composable
fun Sparkline(
    series: SparklineSeries,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 1.5f,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val path = Path()
            val stroke = Stroke(width = strokeWidthDp.dp.toPx())
            onDrawBehind {
                @Suppress("UNUSED_EXPRESSION")
                series.version
                val count = series.count()
                if (count < 2) return@onDrawBehind
                val min = series.minimum
                val max = series.maximum
                val range = (max - min).takeIf { it > 0.0001f } ?: 1f
                val stepX = size.width / (count - 1)
                path.rewind()
                for (index in 0 until count) {
                    val normalized = (series.valueAt(index) - min) / range
                    val x = stepX * index
                    val y = size.height - normalized * size.height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path = path, color = color, style = stroke)
                val lastX = size.width
                val lastY = size.height -
                    ((series.valueAt(count - 1) - min) / range) * size.height
                drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(lastX, lastY))
            }
        },
    )
}
