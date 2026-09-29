package com.dept.markets.feature.marketinsights.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.OrderType
import com.dept.markets.core.model.Side
import com.dept.markets.feature.marketinsights.MarketInsightsIntent
import com.dept.markets.feature.marketinsights.TicketState
import com.dept.markets.feature.marketinsights.message

@Composable
fun TradeTicketPanel(
    ticket: TicketState,
    onIntent: (MarketInsightsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MarketTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Ticket · ${ticket.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = { onIntent(MarketInsightsIntent.TicketDismissed) }) {
                    Text("Close")
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Side.entries.forEach { side ->
                    val selected = ticket.side == side
                    Button(
                        onClick = { onIntent(MarketInsightsIntent.TicketSideChanged(side)) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                !selected -> MaterialTheme.colorScheme.surface
                                side == Side.BUY -> colors.up
                                else -> colors.down
                            },
                        ),
                    ) { Text(side.name) }
                }
                OrderType.entries.forEach { type ->
                    val selected = ticket.orderType == type
                    TextButton(
                        onClick = { onIntent(MarketInsightsIntent.TicketOrderTypeChanged(type)) },
                    ) {
                        Text(
                            text = type.name,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = ticket.quantityText,
                    onValueChange = { onIntent(MarketInsightsIntent.TicketQuantityChanged(it)) },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                )
                if (ticket.orderType == OrderType.LIMIT) {
                    OutlinedTextField(
                        value = ticket.limitPriceText,
                        onValueChange = { onIntent(MarketInsightsIntent.TicketLimitPriceChanged(it)) },
                        label = { Text("Limit price") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            ticket.validation.rejections.forEach { rejection ->
                Text(
                    text = "• ${rejection.message()}",
                    color = colors.down,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            ticket.validation.warnings.forEach { warning ->
                Text(
                    text = "• ${warning.message()}",
                    color = colors.ratingHold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Button(
                onClick = { onIntent(MarketInsightsIntent.TicketSubmitted) },
                enabled = ticket.validation.isSubmittable && !ticket.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (ticket.side == Side.BUY) colors.up else colors.down,
                ),
            ) {
                Text(if (ticket.isSubmitting) "Sending…" else "Submit ${ticket.side.name}")
            }
        }
    }
}
