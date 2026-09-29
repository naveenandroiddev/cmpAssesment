package com.dept.markets.feature.marketinsights.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.Rating
import com.dept.markets.core.model.ResearchCall

@Composable
fun ResearchCard(
    call: ResearchCall,
    onClick: (InstrumentId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MarketTheme.colors
    val ratingColor = when (call.rating) {
        Rating.BUY -> colors.ratingBuy
        Rating.HOLD -> colors.ratingHold
        Rating.SELL -> colors.ratingSell
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(call.instrumentId) },
        color = colors.surfaceElevated,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingDot(ratingColor)
                    BasicText(
                        text = " ${call.rating.name}  ${call.instrumentId.value}",
                        style = MarketTheme.typography.symbol.copy(color = ratingColor),
                    )
                }
                BasicText(
                    text = "Target ${call.targetPrice.format(2)}",
                    style = MarketTheme.typography.priceSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
            BasicText(
                text = call.headline,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                maxLines = 3,
            )
            BasicText(
                text = "${call.analyst} · conviction ${call.conviction.name.lowercase()}",
                modifier = Modifier.padding(top = 6.dp),
                style = MarketTheme.typography.caption.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun RatingDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 2.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .padding(3.dp),
    )
}
