package com.dept.markets.shared

import com.dept.markets.core.domain.MarketDataRepository
import com.dept.markets.core.domain.ObserveMarketBoard
import com.dept.markets.core.domain.ResearchRepository
import com.dept.markets.core.domain.TradeValidator
import com.dept.markets.core.domain.ValidateTrade
import com.dept.markets.data.market.SimulatedMarketDataRepository
import com.dept.markets.data.market.StaticResearchRepository
import com.dept.markets.data.market.epochMillis
import com.dept.markets.feature.marketinsights.MarketInsightsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MarketInsightsComponent(
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val simulatedFeed: SimulatedMarketDataRepository =
        SimulatedMarketDataRepository(sharingScope = applicationScope)

    val marketDataRepository: MarketDataRepository = simulatedFeed
    val researchRepository: ResearchRepository = StaticResearchRepository()
    val tradeValidator: TradeValidator = TradeValidator()

    fun createViewModel(): MarketInsightsViewModel = MarketInsightsViewModel(
        marketRepository = marketDataRepository,
        researchRepository = researchRepository,
        boardStream = ObserveMarketBoard(marketDataRepository)(),
        validateTrade = ValidateTrade(tradeValidator),
        stressControl = simulatedFeed,
        nowEpochMillis = ::epochMillis,
    )
}
