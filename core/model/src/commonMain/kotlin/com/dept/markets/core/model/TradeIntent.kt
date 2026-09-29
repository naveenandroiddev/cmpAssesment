package com.dept.markets.core.model

enum class Side { BUY, SELL }

enum class OrderType { MARKET, LIMIT }

enum class TimeInForce { DAY, IMMEDIATE_OR_CANCEL, FILL_OR_KILL }

data class TradeIntent(
    val instrumentId: InstrumentId,
    val side: Side,
    val quantity: Long,
    val orderType: OrderType,
    val limitPrice: Price? = null,
    val timeInForce: TimeInForce = TimeInForce.DAY,
)

data class TradingLimits(
    val maxOrderNotional: Price,
    val maxQuantityPerOrder: Long,
    val maxLimitDeviationBps: Int,
) {
    companion object {
        val DESK_DEFAULT: TradingLimits = TradingLimits(
            maxOrderNotional = Price.ofUnits(5_000_000),
            maxQuantityPerOrder = 100_000,
            maxLimitDeviationBps = 1_000,
        )
    }
}

enum class MarketPhase { PRE_OPEN, OPEN, CLOSED, HALTED }
