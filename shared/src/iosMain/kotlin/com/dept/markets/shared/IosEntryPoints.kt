package com.dept.markets.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController


fun MarketInsightsViewController(component: MarketInsightsComponent): UIViewController =
    ComposeUIViewController { MarketInsightsRoot(component) }
