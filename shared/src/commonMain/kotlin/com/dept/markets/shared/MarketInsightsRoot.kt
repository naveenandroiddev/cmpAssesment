package com.dept.markets.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.feature.marketinsights.MarketInsightsScreen
import com.dept.markets.feature.marketinsights.MarketInsightsViewModel
@Composable
fun MarketInsightsRoot(component: MarketInsightsComponent) {
    MarketTheme {
        val viewModel: MarketInsightsViewModel = viewModel { component.createViewModel() }
        MarketInsightsScreen(viewModel = viewModel)
    }
}

@Composable
fun MarketInsightsRoot() {
    val component = remember { MarketInsightsComponent() }
    MarketInsightsRoot(component)
}
