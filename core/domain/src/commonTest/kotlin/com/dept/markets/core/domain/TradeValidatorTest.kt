package com.dept.markets.core.domain

import com.dept.markets.core.model.AssetClass
import com.dept.markets.core.model.BasisPoints
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.OrderType
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.Side
import com.dept.markets.core.model.TickDirection
import com.dept.markets.core.model.TimeInForce
import com.dept.markets.core.model.TradeIntent
import com.dept.markets.core.model.TradingLimits
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TradeValidatorTest {

    private val instrumentId = InstrumentId("US10Y")
    private val instrument = Instrument(
        id = instrumentId,
        symbol = "US10Y",
        name = "US Treasury 10Y",
        assetClass = AssetClass.GOVERNMENT_BOND,
        currency = "USD",
        tickSize = Price(15_625), // 1/64 of a point
        lotSize = 1_000,
        faceValue = 1,
    )
    private val now = 1_700_000_000_000L
    private val quote = Quote(
        instrumentId = instrumentId,
        last = Price.ofUnits(99),
        bid = Price(98_984_375),
        ask = Price(99_015_625),
        previousClose = Price.ofUnits(99),
        yieldBps = BasisPoints(423),
        sizeAtBid = 5_000,
        sizeAtAsk = 5_000,
        updatedAtEpochMillis = now,
        tickDirection = TickDirection.FLAT,
        sequence = 1,
    )
    private val validator = TradeValidator(
        limits = TradingLimits(
            maxOrderNotional = Price.ofUnits(5_000_000),
            maxQuantityPerOrder = 100_000,
            maxLimitDeviationBps = 1_000,
        ),
    )

    private fun validate(
        intent: TradeIntent,
        quote: Quote? = this.quote,
        phase: MarketPhase = MarketPhase.OPEN,
        nowMillis: Long = now,
    ) = validator.validate(intent, instrument, quote, phase, nowMillis)

    @Test
    fun `accepts a well formed limit order`() {
        val result = validate(
            TradeIntent(
                instrumentId = instrumentId,
                side = Side.BUY,
                quantity = 1_000,
                orderType = OrderType.LIMIT,
                limitPrice = Price(98_984_375),
            ),
        )
        assertTrue(result.isSubmittable, "expected submittable, got ${result.rejections}")
        assertFalse(result.hasWarnings)
    }

    @Test
    fun `reports every violation, not just the first`() {
        val result = validate(
            TradeIntent(
                instrumentId = instrumentId,
                side = Side.BUY,
                quantity = 999_999, // above limit and not a lot multiple
                orderType = OrderType.LIMIT,
                limitPrice = Price(1), // off tick grid and miles from the market
            ),
        )
        assertFalse(result.isSubmittable)
        assertTrue(result.rejections.size >= 3, "expected multiple rejections, got ${result.rejections}")
    }

    @Test
    fun `rejects quantity that is not a multiple of the lot size`() {
        val result = validate(
            TradeIntent(instrumentId, Side.SELL, 1_500, OrderType.MARKET),
        )
        assertContains(result.rejections, TradeRejection.QuantityNotLotMultiple(1_000))
    }

    @Test
    fun `rejects a limit price off the tick grid`() {
        val result = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.LIMIT, Price(98_984_376)),
        )
        assertContains(result.rejections, TradeRejection.PriceOffTickGrid(instrument.tickSize))
    }

    @Test
    fun `rejects a market order priced off a stale book but only warns on a limit order`() {
        val staleBy = 5_000L
        val market = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.MARKET),
            nowMillis = now + staleBy,
        )
        assertContains(market.rejections, TradeRejection.StaleQuote(staleBy))

        val limit = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.LIMIT, Price(98_984_375)),
            nowMillis = now + staleBy,
        )
        assertTrue(limit.isSubmittable)
        assertContains(limit.warnings, TradeWarning.StaleQuote(staleBy))
    }

    @Test
    fun `warns when a limit order crosses the spread instead of rejecting it`() {
        val result = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.LIMIT, Price(99_015_625)),
        )
        assertTrue(result.isSubmittable)
        assertContains(result.warnings, TradeWarning.CrossesTheSpread)
    }

    @Test
    fun `pre open accepts only day limit orders`() {
        val marketOrder = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.MARKET),
            phase = MarketPhase.PRE_OPEN,
        )
        assertContains(
            marketOrder.rejections,
            TradeRejection.OrderTypeNotAllowedInPhase(MarketPhase.PRE_OPEN),
        )

        val dayLimit = validate(
            TradeIntent(
                instrumentId, Side.BUY, 1_000, OrderType.LIMIT,
                Price(98_984_375), TimeInForce.DAY,
            ),
            phase = MarketPhase.PRE_OPEN,
        )
        assertTrue(dayLimit.isSubmittable)
    }

    @Test
    fun `rejects everything while the instrument is halted`() {
        val result = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.LIMIT, Price(98_984_375)),
            phase = MarketPhase.HALTED,
        )
        assertContains(result.rejections, TradeRejection.MarketNotTradable(MarketPhase.HALTED))
    }

    @Test
    fun `rejects when there is no live quote at all`() {
        val result = validate(
            TradeIntent(instrumentId, Side.BUY, 1_000, OrderType.MARKET),
            quote = null,
        )
        assertContains(result.rejections, TradeRejection.NoLiveQuote)
    }

    @Test
    fun `notional limit is enforced with integer arithmetic`() {
        val result = validate(
            TradeIntent(instrumentId, Side.BUY, 100_000, OrderType.LIMIT, Price(98_984_375)),
        )
        val breach = result.rejections.filterIsInstance<TradeRejection.NotionalAboveLimit>()
        assertEquals(1, breach.size, "expected a notional breach, got ${result.rejections}")
    }
}
