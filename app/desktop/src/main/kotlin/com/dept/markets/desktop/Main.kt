package com.dept.markets.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.dept.markets.shared.MarketInsightsComponent
import com.dept.markets.shared.MarketInsightsRoot

fun main() = application {
    val component = MarketInsightsComponent()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Market Insights & Execution",
        state = rememberWindowState(size = DpSize(1180.dp, 860.dp)),
    ) {
        MarketInsightsRoot(component)
    }
}
