package com.dept.markets.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.BasisPoints
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.TickDirection
@Composable
fun LivePriceText(
    price: () -> Price,
    modifier: Modifier = Modifier,
    decimals: Int = 3,
    style: TextStyle = MarketTheme.typography.price,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.End,
) {
    val resolved = if (color == Color.Unspecified) style else style.copy(color = color)
    BasicText(
        text = price().format(decimals),
        modifier = modifier,
        style = resolved.copy(textAlign = textAlign),
        maxLines = 1,
    )
}

@Composable
fun LiveChangeText(
    changeBps: () -> BasisPoints,
    direction: () -> TickDirection,
    modifier: Modifier = Modifier,
    style: TextStyle = MarketTheme.typography.priceSmall,
) {
    val colors = MarketTheme.colors
    val value = changeBps()
    val tint = when {
        value.value > 0 -> colors.up
        value.value < 0 -> colors.down
        else -> colors.flat
    }
    BasicText(
        text = value.format(),
        modifier = modifier,
        style = style.copy(color = tint, textAlign = TextAlign.End),
        maxLines = 1,
    )
}
