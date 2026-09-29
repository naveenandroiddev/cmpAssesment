package com.dept.markets.feature.marketinsights

import androidx.compose.runtime.Immutable
import com.dept.markets.core.domain.TradeValidation
import com.dept.markets.core.model.AssetClass
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.OrderType
import com.dept.markets.core.model.ResearchCall
import com.dept.markets.core.model.Side
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class MarketInsightsUiState(
    val isLoadingResearch: Boolean = true,
    val researchCalls: ImmutableList<ResearchCall> = persistentListOf(),
    val filter: BoardFilter = BoardFilter.ALL,
    val phase: MarketPhase = MarketPhase.OPEN,
    val stressMode: Boolean = false,
    val ticket: TicketState? = null,
    val feedStats: FeedStats = FeedStats(),
) {
    val researchByInstrument: Map<InstrumentId, ResearchCall>
        get() = researchCalls.associateBy { it.instrumentId }
}

enum class BoardFilter(val label: String) {
    ALL("All"),
    BONDS("Bonds"),
    EQUITIES("Equities"),
    RESEARCH("With research"),
    ;

    fun matches(assetClass: AssetClass, hasResearch: Boolean): Boolean = when (this) {
        ALL -> true
        BONDS -> assetClass.isBond
        EQUITIES -> assetClass == AssetClass.EQUITY
        RESEARCH -> hasResearch
    }
}

@Immutable
data class FeedStats(
    val boardsReceivedPerSecond: Int = 0,
    val quoteUpdatesPerSecond: Int = 0,
    val framesRenderedPerSecond: Int = 0,
)

@Immutable
data class TicketState(
    val instrumentId: InstrumentId,
    val symbol: String,
    val side: Side = Side.BUY,
    val orderType: OrderType = OrderType.LIMIT,
    val quantityText: String = "",
    val limitPriceText: String = "",
    val validation: TradeValidation = TradeValidation.VALID,
    val isSubmitting: Boolean = false,
)

sealed interface MarketInsightsIntent {
    data class FilterSelected(val filter: BoardFilter) : MarketInsightsIntent
    data class InstrumentSelected(val instrumentId: InstrumentId) : MarketInsightsIntent
    data object TicketDismissed : MarketInsightsIntent
    data class TicketSideChanged(val side: Side) : MarketInsightsIntent
    data class TicketOrderTypeChanged(val orderType: OrderType) : MarketInsightsIntent
    data class TicketQuantityChanged(val text: String) : MarketInsightsIntent
    data class TicketLimitPriceChanged(val text: String) : MarketInsightsIntent
    data object TicketSubmitted : MarketInsightsIntent
    data class StressModeToggled(val enabled: Boolean) : MarketInsightsIntent
    data object ResearchRetried : MarketInsightsIntent
}

sealed interface MarketInsightsEffect {
    data class OrderAccepted(val symbol: String, val quantity: Long) : MarketInsightsEffect
    data class OrderRejected(val reason: String) : MarketInsightsEffect
}
