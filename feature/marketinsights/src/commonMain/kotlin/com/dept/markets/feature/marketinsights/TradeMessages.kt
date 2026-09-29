package com.dept.markets.feature.marketinsights

import com.dept.markets.core.domain.TradeRejection
import com.dept.markets.core.domain.TradeWarning

fun TradeRejection.message(): String = when (this) {
    TradeRejection.InstrumentMismatch -> "Ticket does not match the selected instrument"
    TradeRejection.NonPositiveQuantity -> "Enter a quantity greater than zero"
    is TradeRejection.QuantityNotLotMultiple -> "Quantity must be a multiple of $lotSize"
    is TradeRejection.QuantityAboveLimit -> "Above the per-order limit of $maxQuantity"
    TradeRejection.MissingLimitPrice -> "Limit orders need a price"
    TradeRejection.LimitPriceOnMarketOrder -> "Market orders cannot carry a limit price"
    TradeRejection.NonPositiveLimitPrice -> "Limit price must be greater than zero"
    is TradeRejection.PriceOffTickGrid -> "Price must sit on the ${tickSize.format(6)} tick grid"
    is TradeRejection.LimitPriceFarFromMarket ->
        "Limit is $deviationBps bp from mid; desk allows $allowedBps bp"
    is TradeRejection.NotionalAboveLimit ->
        "Notional ${notional.format(0)} exceeds the desk limit ${maxNotional.format(0)}"
    is TradeRejection.OrderTypeNotAllowedInPhase ->
        "Only day limit orders are accepted in ${phase.name.replace('_', ' ').lowercase()}"
    is TradeRejection.MarketNotTradable -> "Market is ${phase.name.lowercase()}"
    TradeRejection.NoLiveQuote -> "No live quote for this instrument"
    is TradeRejection.StaleQuote -> "Quote is ${ageMillis} ms old; refresh before trading"
}

fun TradeWarning.message(): String = when (this) {
    TradeWarning.CrossesTheSpread -> "Marketable: this order will execute immediately"
    is TradeWarning.StaleQuote -> "Quote is ${ageMillis} ms old"
}
