package com.dept.markets.feature.marketinsights.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dept.markets.core.designsystem.component.QuoteGridRow
import com.dept.markets.core.designsystem.theme.MarketTheme

@Composable
fun QuoteGridHeader(modifier: Modifier = Modifier) {
    QuoteGridRow(
        modifier = modifier
            .fillMaxWidth()
            .background(MarketTheme.colors.surfaceElevated),
        height = 32.dp,
    ) {
        HeaderCell("Instrument", Alignment.CenterStart, TextAlign.Start)
        HeaderCell("Last", Alignment.CenterEnd, TextAlign.End)
        HeaderCell("Chg", Alignment.CenterEnd, TextAlign.End)
        HeaderCell("Yield", Alignment.CenterEnd, TextAlign.End)
        HeaderCell("Trend", Alignment.Center, TextAlign.Center)
        HeaderCell("Bid sz", Alignment.CenterEnd, TextAlign.End)
    }
}

@Composable
private fun HeaderCell(label: String, alignment: Alignment, textAlign: TextAlign) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) {
        BasicText(
            text = label,
            style = MarketTheme.typography.caption.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            ),
            maxLines = 1,
        )
    }
}
