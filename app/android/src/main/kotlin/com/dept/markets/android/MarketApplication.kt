package com.dept.markets.android

import android.app.Application
import com.dept.markets.shared.MarketInsightsComponent

class MarketApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        component = MarketInsightsComponent()
    }

    companion object {
        lateinit var component: MarketInsightsComponent
            private set
    }
}
