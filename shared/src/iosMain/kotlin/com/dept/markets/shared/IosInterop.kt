package com.dept.markets.shared

import com.dept.markets.core.domain.MarketDataRepository
import com.dept.markets.core.domain.ObserveMarketBoard
import com.dept.markets.core.domain.QuoteBoard
import com.dept.markets.core.domain.TradeRejection
import com.dept.markets.core.domain.TradeValidation
import com.dept.markets.core.domain.TradeValidator
import com.dept.markets.core.domain.TradeWarning
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.TradeIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("MarketKit")
object MarketKit {

    fun createComponent(): MarketInsightsComponent = MarketInsightsComponent()

    fun tradeValidator(): TradeValidator = TradeValidator()


    fun validationSummary(validation: TradeValidation): ValidationSummary = ValidationSummary(
        isSubmittable = validation.isSubmittable,
        rejections = validation.rejections.map { it.describe() },
        warnings = validation.warnings.map { it.describe() },
    )


    fun quote(board: QuoteBoard, symbol: String): Quote? =
        board.byId[com.dept.markets.core.model.InstrumentId(symbol)]

    fun instrument(component: MarketInsightsComponent, symbol: String): Instrument? =
        component.marketDataRepository.instruments.firstOrNull { it.symbol == symbol }

    fun parsePriceMicros(text: String): Long =
        com.dept.markets.core.model.Price.parseOrNull(text)?.micros ?: 0L

    fun validate(
        validator: TradeValidator,
        intent: TradeIntent,
        instrument: Instrument,
        quote: Quote?,
        phase: MarketPhase,
        nowEpochMillis: Long,
    ): ValidationSummary = validationSummary(
        validator.validate(intent, instrument, quote, phase, nowEpochMillis),
    )
}

data class ValidationSummary(
    val isSubmittable: Boolean,
    val rejections: List<String>,
    val warnings: List<String>,
)


@OptIn(ExperimentalObjCName::class)
@ObjCName("KotlinCancellable")
class Cancellable internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}


@OptIn(ExperimentalObjCName::class)
@ObjCName("MarketBoardSubscription")
class BoardSubscription(repository: MarketDataRepository) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val observe = ObserveMarketBoard(repository)

    fun subscribe(onBoard: (QuoteBoard) -> Unit): Cancellable {
        val job = observe()
            .onEach { board -> onBoard(board) }
            .launchIn(scope)
        return Cancellable(job)
    }

    fun close() {
        scope.cancel()
    }
}

private fun TradeRejection.describe(): String = when (this) {
    TradeRejection.InstrumentMismatch -> "Ticket does not match the selected instrument"
    TradeRejection.NonPositiveQuantity -> "Quantity must be greater than zero"
    is TradeRejection.QuantityNotLotMultiple -> "Quantity must be a multiple of $lotSize"
    is TradeRejection.QuantityAboveLimit -> "Above the per-order limit of $maxQuantity"
    TradeRejection.MissingLimitPrice -> "Limit orders need a price"
    TradeRejection.LimitPriceOnMarketOrder -> "Market orders cannot carry a limit price"
    TradeRejection.NonPositiveLimitPrice -> "Limit price must be greater than zero"
    is TradeRejection.PriceOffTickGrid -> "Price must sit on the ${tickSize.format(6)} tick grid"
    is TradeRejection.LimitPriceFarFromMarket -> "Limit is $deviationBps bp from mid (max $allowedBps bp)"
    is TradeRejection.NotionalAboveLimit -> "Notional ${notional.format(0)} exceeds ${maxNotional.format(0)}"
    is TradeRejection.OrderTypeNotAllowedInPhase -> "Order type not accepted in ${phase.name}"
    is TradeRejection.MarketNotTradable -> "Market is ${phase.name}"
    TradeRejection.NoLiveQuote -> "No live quote for this instrument"
    is TradeRejection.StaleQuote -> "Quote is $ageMillis ms old"
}

private fun TradeWarning.describe(): String = when (this) {
    TradeWarning.CrossesTheSpread -> "Marketable: will execute immediately"
    is TradeWarning.StaleQuote -> "Quote is $ageMillis ms old"
}

@OptIn(ExperimentalObjCName::class)
@ObjCName("SwiftTradeValidator")
class SwiftTradeValidator(
    private val validator: TradeValidator = TradeValidator(),
) {
    fun validateOrder(
        instrument: Instrument,
        quote: Quote?,
        phase: MarketPhase,
        side: com.dept.markets.core.model.Side,
        quantity: Long,
        orderType: com.dept.markets.core.model.OrderType,
        limitPriceMicros: Long,
        nowEpochMillis: Long,
    ): ValidationSummary {
        val intent = TradeIntent(
            instrumentId = instrument.id,
            side = side,
            quantity = quantity,
            orderType = orderType,
            limitPrice = if (orderType == com.dept.markets.core.model.OrderType.LIMIT) {
                com.dept.markets.core.model.Price(limitPriceMicros)
            } else {
                null
            },
        )
        return MarketKit.validationSummary(
            validator.validate(intent, instrument, quote, phase, nowEpochMillis),
        )
    }

    fun formatPrice(micros: Long, decimals: Int): String =
        com.dept.markets.core.model.Price(micros).format(decimals)
}
