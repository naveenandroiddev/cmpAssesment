package com.dept.markets.feature.marketinsights.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.feature.marketinsights.FeedStats

@Composable
fun FeedStatusBar(
    stats: FeedStats,
    phase: MarketPhase,
    stressMode: Boolean,
    onStressModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MarketTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceElevated)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(
            text = buildString {
                append(phase.name.replace('_', ' '))
                append("   ")
                append("${stats.quoteUpdatesPerSecond} upd/s")
                append("   ")
                append("${stats.boardsReceivedPerSecond} snap/s")
                append("   ")
                append("${stats.framesRenderedPerSecond} fps")
            },
            style = MarketTheme.typography.caption.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = "Stress",
                style = MarketTheme.typography.caption.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
            Switch(
                checked = stressMode,
                onCheckedChange = onStressModeChange,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
