package com.dept.markets.data.market

import com.dept.markets.core.domain.FeedStressControl
import com.dept.markets.core.domain.MarketDataRepository
import com.dept.markets.core.domain.QuoteBoard
import com.dept.markets.core.model.BasisPoints
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.TickDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SimulatedMarketDataRepository(
    sharingScope: CoroutineScope,
    override val instruments: ImmutableList<Instrument> = InstrumentCatalog.all,
    private val config: FeedConfig = FeedConfig(),
    private val random: Random = Random(0xC0FFEE),
) : MarketDataRepository, FeedStressControl {

    private val phase = MutableStateFlow(MarketPhase.OPEN)
    private val stressed = MutableStateFlow(false)

    private val board: Flow<QuoteBoard> = flow { simulate() }
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = sharingScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT.inWholeMilliseconds),
            replay = 1, // a new subscriber paints instantly with the last known board
        )

    override fun boardStream(): Flow<QuoteBoard> = board

    fun setPhase(newPhase: MarketPhase) {
        phase.value = newPhase
    }

    override fun setStressEnabled(enabled: Boolean) {
        stressed.value = enabled
    }

    private suspend fun FlowCollector<QuoteBoard>.simulate() {
        val size = instruments.size
        val last = LongArray(size)
        val previousClose = LongArray(size)
        val sequence = LongArray(size)
        val quotes = arrayOfNulls<Quote>(size)

        instruments.forEachIndexed { index, instrument ->
            val seed = seedPriceMicros(instrument)
            last[index] = seed
            previousClose[index] = seed
        }

        var revision = 0L
        val now0 = epochMillis()
        for (index in 0 until size) {
            quotes[index] = buildQuote(index, instruments[index], last, previousClose, sequence, TickDirection.FLAT, now0)
        }
        emitBoard(quotes, revision, phase.value)

        while (currentCoroutineContext().isActive) {
            val activeConfig = if (stressed.value) FeedConfig.STRESS else config
            val now = epochMillis()

            repeat(activeConfig.ticksPerBurst) {
                val index = random.nextInt(size)
                val instrument = instruments[index]
                val current = last[index]
                val volatility = volatilityBps(instrument)

                val driftBps = random.nextInt(-volatility, volatility + 1)
                val deltaMicros = current * driftBps / 10_000L
                val tickSize = instrument.tickSize.micros
                val snapped = ((current + deltaMicros) / tickSize) * tickSize
                val next = if (snapped <= 0L) tickSize else snapped
                val direction = when {
                    next > current -> TickDirection.UP
                    next < current -> TickDirection.DOWN
                    else -> TickDirection.FLAT
                }
                last[index] = next
                sequence[index] = sequence[index] + 1
                quotes[index] = buildQuote(index, instrument, last, previousClose, sequence, direction, now)
            }
            revision++
            emitBoard(quotes, revision, phase.value)
            delay(activeConfig.burstPeriod)
        }
    }

    private suspend fun FlowCollector<QuoteBoard>.emitBoard(
        quotes: Array<Quote?>,
        revision: Long,
        marketPhase: MarketPhase,
    ) {
        val ordered = quotes.map { requireNotNull(it) }.toImmutableList()
        val byId = persistentMapOf<InstrumentId, Quote>()
            .putAll(ordered.associateBy { it.instrumentId })
        emit(QuoteBoard(quotes = ordered, byId = byId, revision = revision, phase = marketPhase))
    }

    private fun buildQuote(
        index: Int,
        instrument: Instrument,
        last: LongArray,
        previousClose: LongArray,
        sequence: LongArray,
        direction: TickDirection,
        now: Long,
    ): Quote {
        val lastMicros = last[index]
        val halfSpread = maxOf(instrument.tickSize.micros / 2, lastMicros / 20_000L)
        return Quote(
            instrumentId = instrument.id,
            last = Price(lastMicros),
            bid = Price(lastMicros - halfSpread),
            ask = Price(lastMicros + halfSpread),
            previousClose = Price(previousClose[index]),
            yieldBps = if (instrument.assetClass.isBond) impliedYield(lastMicros) else null,
            sizeAtBid = 1_000L + (index * 137L % 9_000L),
            sizeAtAsk = 1_000L + (index * 271L % 9_000L),
            updatedAtEpochMillis = now,
            tickDirection = direction,
            sequence = sequence[index],
        )
    }

    private fun impliedYield(priceMicros: Long): BasisPoints {
        val parMicros = 100_000_000L
        val bps = 400L + (parMicros - priceMicros) * 100L / 1_000_000L
        return BasisPoints(bps.toInt())
    }

    private fun seedPriceMicros(instrument: Instrument): Long = when {
        instrument.assetClass.isBond -> 96_000_000L + (instrument.symbol.hashCode().toLong() and 0x3FF) * 8_000L
        else -> 40_000_000L + (instrument.symbol.hashCode().toLong() and 0xFFF) * 90_000L
    }

    private fun volatilityBps(instrument: Instrument): Int =
        if (instrument.assetClass.isBond) 4 else 12

    companion object {
        private val STOP_TIMEOUT: Duration = 5.seconds
    }
}


data class FeedConfig(
    val burstPeriod: Duration = 5.milliseconds,
    val ticksPerBurst: Int = 8,
) {
    companion object {
        val STRESS: FeedConfig = FeedConfig(burstPeriod = 1.milliseconds, ticksPerBurst = 24)
    }
}
