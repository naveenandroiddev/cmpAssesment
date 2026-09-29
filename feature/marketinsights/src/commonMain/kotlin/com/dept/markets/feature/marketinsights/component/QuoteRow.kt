package com.dept.markets.feature.marketinsights.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dept.markets.core.designsystem.component.LiveChangeText
import com.dept.markets.core.designsystem.component.LivePriceText
import com.dept.markets.core.designsystem.component.QuoteGridRow
import com.dept.markets.core.designsystem.component.QuoteGridWeights
import com.dept.markets.core.designsystem.component.Sparkline
import com.dept.markets.core.designsystem.component.priceFlash
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.BasisPoints
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.TickDirection
import com.dept.markets.feature.marketinsights.MarketRowState

@Composable
fun QuoteRow(
    row: MarketRowState,
    hasResearch: Boolean,
    onClick: (InstrumentId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MarketTheme.colors
    QuoteGridRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(row.instrument.id) }
            .priceFlash(
                sequence = { row.quote.sequence },
                direction = { row.quote.tickDirection },
            ),
        weights = QuoteGridWeights,
        height = MarketTheme.metrics.rowHeightDp.dp,
    ) {
        SymbolCell(row.instrument, hasResearch)

        Cell(alignment = Alignment.CenterEnd) {
            LivePriceText(
                price = { row.quote.last },
                decimals = if (row.instrument.assetClass.isBond) 3 else 2,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Cell(alignment = Alignment.CenterEnd) {
            LiveChangeText(
                changeBps = { row.quote.changeFromCloseBps },
                direction = { row.quote.tickDirection },
            )
        }

        Cell(alignment = Alignment.CenterEnd) {
            YieldCell(yieldBps = { row.quote.yieldBps })
        }

        Cell(alignment = Alignment.Center) {
            Sparkline(
                series = row.sparkline,
                color = colors.flat,
                modifier = Modifier.fillMaxWidth().height(20.dp).padding(horizontal = 4.dp),
            )
        }

        Cell(alignment = Alignment.CenterEnd) {
            SizeCell(size = { row.quote.sizeAtBid }, direction = { row.quote.tickDirection })
        }
    }
}

@Composable
private fun Cell(
    alignment: Alignment,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = alignment) { content() }
}

@Composable
private fun SymbolCell(instrument: Instrument, hasResearch: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
        androidx.compose.foundation.layout.Column {
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = instrument.symbol,
                    style = MarketTheme.typography.symbol.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                )
                if (hasResearch) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .height(6.dp)
                            .fillMaxSize(0.02f)
                            .background(MarketTheme.colors.ratingBuy),
                    )
                }
            }
            BasicText(
                text = instrument.name,
                style = MarketTheme.typography.caption.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun YieldCell(yieldBps: () -> BasisPoints?) {
    val value = yieldBps()
    BasicText(
        text = value?.formatBps()?.removePrefix("+") ?: "—",
        style = MarketTheme.typography.priceSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        ),
        maxLines = 1,
    )
}

@Composable
private fun SizeCell(size: () -> Long, direction: () -> TickDirection) {
    val colors = MarketTheme.colors
    val tint = when (direction()) {
        TickDirection.UP -> colors.up
        TickDirection.DOWN -> colors.down
        TickDirection.FLAT -> colors.flat
    }
    BasicText(
        text = size().toString(),
        style = MarketTheme.typography.priceSmall.copy(color = tint, textAlign = TextAlign.End),
        maxLines = 1,
    )
}
