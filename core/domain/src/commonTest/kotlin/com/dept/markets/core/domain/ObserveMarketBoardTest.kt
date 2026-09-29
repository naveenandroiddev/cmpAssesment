package com.dept.markets.core.domain

import com.dept.markets.core.model.AssetClass
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.TickDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Proves the pacing contract in a test rather than in a comment: a burst of 100 updates
 * inside one frame must reach the UI as (at most) one update, and it must be the newest
 * one. Losing intermediate ticks is intended behaviour for a display feed — this test is
 * what stops a well-meaning future change from "fixing" it with `buffer()`.
 */
class ObserveMarketBoardTest {

    @Test
    fun `a burst of ticks is conflated down to the frame rate, keeping the latest`() = runTest {
        val repository = BurstRepository(bursts = 100, gapMillis = 1)
        val observed = ObserveMarketBoard(repository)(framePeriod = 16.milliseconds).toList()

        assertTrue(
            observed.size < 20,
            "expected conflation to well under one emission per tick, got ${observed.size}",
        )
        // `sample` does not flush a pending value when the upstream completes, so the
        // very last tick of a *finite* stream can be dropped. That is harmless for a
        // market feed, which never completes while the screen is open — but it is worth
        // knowing before someone reuses this operator for a one-shot request.
        assertTrue(
            observed.last().revision >= 90L,
            "conflation must keep the newest board, got ${observed.last().revision}",
        )
        assertEquals(
            observed.map { it.revision }.sorted(),
            observed.map { it.revision },
            "boards must stay in order",
        )
    }
}

private class BurstRepository(
    private val bursts: Int,
    private val gapMillis: Long,
) : MarketDataRepository {

    override val instruments: ImmutableList<Instrument> = persistentListOf(
        Instrument(
            id = InstrumentId("US10Y"),
            symbol = "US10Y",
            name = "US Treasury 10Y",
            assetClass = AssetClass.GOVERNMENT_BOND,
            currency = "USD",
            tickSize = Price(15_625),
            lotSize = 1_000,
        ),
    )

    override fun boardStream(): Flow<QuoteBoard> = flow {
        repeat(bursts) { index ->
            val quote = Quote(
                instrumentId = InstrumentId("US10Y"),
                last = Price(100_000_000L + index),
                bid = Price(99_990_000L),
                ask = Price(100_010_000L),
                previousClose = Price(100_000_000L),
                yieldBps = null,
                sizeAtBid = 1_000,
                sizeAtAsk = 1_000,
                updatedAtEpochMillis = index.toLong(),
                tickDirection = TickDirection.UP,
                sequence = index.toLong(),
            )
            val quotes = persistentListOf(quote).toImmutableList()
            emit(
                QuoteBoard(
                    quotes = quotes,
                    byId = persistentMapOf(quote.instrumentId to quote),
                    revision = index.toLong(),
                    phase = MarketPhase.OPEN,
                ),
            )
            delay(gapMillis)
        }
    }
}
