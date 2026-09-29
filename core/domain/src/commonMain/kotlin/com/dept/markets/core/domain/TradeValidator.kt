package com.dept.markets.core.domain

import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.OrderType
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.Side
import com.dept.markets.core.model.TimeInForce
import com.dept.markets.core.model.TradeIntent
import com.dept.markets.core.model.TradingLimits
import com.dept.markets.core.model.Instrument
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("TradeValidator")
class TradeValidator(
    private val limits: TradingLimits = TradingLimits.DESK_DEFAULT,
    private val maxQuoteAgeMillis: Long = 2_000L,
) {

    fun validate(
        intent: TradeIntent,
        instrument: Instrument,
        quote: Quote?,
        marketPhase: MarketPhase,
        nowEpochMillis: Long,
    ): TradeValidation {
        val failures = mutableListOf<TradeRejection>()
        val warnings = mutableListOf<TradeWarning>()

        if (intent.instrumentId != instrument.id) {
            failures += TradeRejection.InstrumentMismatch
        }

        validateQuantity(intent, instrument, failures)
        validatePricing(intent, instrument, quote, failures, warnings)
        validateMarketPhase(intent, marketPhase, failures)
        validateNotional(intent, instrument, quote, failures)
        validateQuoteFreshness(intent, quote, nowEpochMillis, failures, warnings)

        return TradeValidation(
            rejections = failures.toImmutableList(),
            warnings = warnings.toImmutableList(),
        )
    }

    private fun validateQuantity(
        intent: TradeIntent,
        instrument: Instrument,
        failures: MutableList<TradeRejection>,
    ) {
        if (intent.quantity <= 0L) {
            failures += TradeRejection.NonPositiveQuantity
            return
        }
        if (instrument.lotSize > 0L && intent.quantity % instrument.lotSize != 0L) {
            failures += TradeRejection.QuantityNotLotMultiple(instrument.lotSize)
        }
        if (intent.quantity > limits.maxQuantityPerOrder) {
            failures += TradeRejection.QuantityAboveLimit(limits.maxQuantityPerOrder)
        }
    }

    private fun validatePricing(
        intent: TradeIntent,
        instrument: Instrument,
        quote: Quote?,
        failures: MutableList<TradeRejection>,
        warnings: MutableList<TradeWarning>,
    ) {
        when (intent.orderType) {
            OrderType.MARKET -> {
                if (intent.limitPrice != null) failures += TradeRejection.LimitPriceOnMarketOrder
            }

            OrderType.LIMIT -> {
                val limitPrice = intent.limitPrice
                if (limitPrice == null) {
                    failures += TradeRejection.MissingLimitPrice
                    return
                }
                if (limitPrice.micros <= 0L) {
                    failures += TradeRejection.NonPositiveLimitPrice
                    return
                }
                val tick = instrument.tickSize.micros
                if (tick > 0L && limitPrice.micros % tick != 0L) {
                    failures += TradeRejection.PriceOffTickGrid(instrument.tickSize)
                }
                if (quote != null && !quote.last.isZero) {
                    val mid = Price((quote.bid.micros + quote.ask.micros) / 2)
                    val deviation = limitPrice.changeBpsFrom(mid).value
                    val magnitude = if (deviation < 0) -deviation else deviation
                    if (magnitude > limits.maxLimitDeviationBps) {
                        failures += TradeRejection.LimitPriceFarFromMarket(
                            deviationBps = deviation,
                            allowedBps = limits.maxLimitDeviationBps,
                        )
                    }
                    // Marketable limit orders are legal and often intentional; the trader
                    // just deserves to know the order will execute immediately.
                    val crosses = when (intent.side) {
                        Side.BUY -> limitPrice >= quote.ask
                        Side.SELL -> limitPrice <= quote.bid
                    }
                    if (crosses) warnings += TradeWarning.CrossesTheSpread
                }
            }
        }
    }

    private fun validateMarketPhase(
        intent: TradeIntent,
        marketPhase: MarketPhase,
        failures: MutableList<TradeRejection>,
    ) {
        when (marketPhase) {
            MarketPhase.OPEN -> Unit
            MarketPhase.PRE_OPEN ->
                if (intent.orderType != OrderType.LIMIT || intent.timeInForce != TimeInForce.DAY) {
                    failures += TradeRejection.OrderTypeNotAllowedInPhase(marketPhase)
                }

            MarketPhase.CLOSED, MarketPhase.HALTED ->
                failures += TradeRejection.MarketNotTradable(marketPhase)
        }
    }

    private fun validateNotional(
        intent: TradeIntent,
        instrument: Instrument,
        quote: Quote?,
        failures: MutableList<TradeRejection>,
    ) {
        val referencePrice = intent.limitPrice ?: quote?.last ?: return
        if (intent.quantity <= 0L) return
        val notionalMicros = referencePrice.micros * intent.quantity * instrument.faceValue
        val notional = Price(notionalMicros)
        if (notional > limits.maxOrderNotional) {
            failures += TradeRejection.NotionalAboveLimit(notional, limits.maxOrderNotional)
        }
    }

    private fun validateQuoteFreshness(
        intent: TradeIntent,
        quote: Quote?,
        nowEpochMillis: Long,
        failures: MutableList<TradeRejection>,
        warnings: MutableList<TradeWarning>,
    ) {
        if (quote == null) {
            failures += TradeRejection.NoLiveQuote
            return
        }
        val age = nowEpochMillis - quote.updatedAtEpochMillis
        if (age > maxQuoteAgeMillis) {
            // A stale book is fatal for a market order (unknowable fill price) but only
            // advisory for a limit order (the price is the trader's, not the venue's).
            if (intent.orderType == OrderType.MARKET) {
                failures += TradeRejection.StaleQuote(age)
            } else {
                warnings += TradeWarning.StaleQuote(age)
            }
        }
    }
}


data class TradeValidation(
    val rejections: ImmutableList<TradeRejection> = persistentListOf(),
    val warnings: ImmutableList<TradeWarning> = persistentListOf(),
) {
    val isSubmittable: Boolean get() = rejections.isEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty()

    companion object {
        val VALID: TradeValidation = TradeValidation()
    }
}


sealed interface TradeRejection {
    data object InstrumentMismatch : TradeRejection
    data object NonPositiveQuantity : TradeRejection
    data class QuantityNotLotMultiple(val lotSize: Long) : TradeRejection
    data class QuantityAboveLimit(val maxQuantity: Long) : TradeRejection
    data object MissingLimitPrice : TradeRejection
    data object LimitPriceOnMarketOrder : TradeRejection
    data object NonPositiveLimitPrice : TradeRejection
    data class PriceOffTickGrid(val tickSize: Price) : TradeRejection
    data class LimitPriceFarFromMarket(val deviationBps: Int, val allowedBps: Int) : TradeRejection
    data class NotionalAboveLimit(val notional: Price, val maxNotional: Price) : TradeRejection
    data class OrderTypeNotAllowedInPhase(val phase: MarketPhase) : TradeRejection
    data class MarketNotTradable(val phase: MarketPhase) : TradeRejection
    data object NoLiveQuote : TradeRejection
    data class StaleQuote(val ageMillis: Long) : TradeRejection
}

sealed interface TradeWarning {
    data object CrossesTheSpread : TradeWarning
    data class StaleQuote(val ageMillis: Long) : TradeWarning
}
