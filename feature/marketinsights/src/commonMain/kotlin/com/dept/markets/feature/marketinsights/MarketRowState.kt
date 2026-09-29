package com.dept.markets.feature.marketinsights

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dept.markets.core.designsystem.component.SparklineSeries
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.Quote

@Stable
class MarketRowState(
    val instrument: Instrument,
    initialQuote: Quote,
) {
    var quote: Quote by mutableStateOf(initialQuote)
        internal set

    val sparkline: SparklineSeries = SparklineSeries(capacity = 48)

    internal fun update(next: Quote) {
        if (next.sequence < quote.sequence) return
        quote = next
        sparkline.push(next.last.micros.toFloat())
    }
}
