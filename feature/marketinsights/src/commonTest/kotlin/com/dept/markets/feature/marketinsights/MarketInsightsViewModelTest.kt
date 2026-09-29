package com.dept.markets.feature.marketinsights

import com.dept.markets.core.domain.MarketDataRepository
import com.dept.markets.core.domain.QuoteBoard
import com.dept.markets.core.domain.ResearchRepository
import com.dept.markets.core.model.AssetClass
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.ResearchCall
import com.dept.markets.core.model.TickDirection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MarketInsightsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val instruments = listOf(
        instrument("US10Y", AssetClass.GOVERNMENT_BOND),
        instrument("AAPL", AssetClass.EQUITY),
    ).toImmutableList()

    private val boards = MutableSharedFlow<QuoteBoard>(replay = 1, extraBufferCapacity = 512)

    private var clock = NOW

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `slow lane state is not recreated by price ticks`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        boards.emit(board(revision = 0, last = 100_000_000))
        runCurrent()

        val stateBeforeTicks = viewModel.uiState.value
        repeat(500) { tick ->
            boards.emit(board(revision = tick + 1L, last = 100_000_000 + tick * 1_000L))
        }
        runCurrent()

        assertSame(stateBeforeTicks, viewModel.uiState.value)
    }

    @Test
    fun `fast lane rows receive every applied quote`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        boards.emit(board(revision = 0, last = 100_000_000))
        runCurrent()

        boards.emit(board(revision = 1, last = 101_000_000))
        runCurrent()

        val row = viewModel.visibleRows.first { it.instrument.symbol == "US10Y" }
        assertEquals(Price(101_000_000), row.quote.last)
    }

    @Test
    fun `out of order packets are ignored`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        boards.emit(board(revision = 0, last = 100_000_000, sequence = 10))
        runCurrent()
        boards.emit(board(revision = 1, last = 90_000_000, sequence = 4))
        runCurrent()

        val row = viewModel.visibleRows.first()
        assertEquals(Price(100_000_000), row.quote.last, "a stale packet must not move the price")
    }

    @Test
    fun `filter narrows the visible rows without touching the feed`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        boards.emit(board(revision = 0, last = 100_000_000))
        runCurrent()
        assertEquals(2, viewModel.visibleRows.size)

        viewModel.onIntent(MarketInsightsIntent.FilterSelected(BoardFilter.EQUITIES))
        runCurrent()

        assertEquals(1, viewModel.visibleRows.size)
        assertEquals("AAPL", viewModel.visibleRows.first().instrument.symbol)
    }

    @Test
    fun `opening a ticket validates immediately against the live board`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        boards.emit(board(revision = 0, last = 100_000_000))
        runCurrent()

        viewModel.onIntent(MarketInsightsIntent.InstrumentSelected(InstrumentId("US10Y")))
        runCurrent()

        val ticket = viewModel.uiState.value.ticket
        assertTrue(ticket != null)
        // Default quantity is one lot and the price is the live last: submittable.
        assertTrue(ticket.validation.isSubmittable, "unexpected: ${ticket.validation.rejections}")

        viewModel.onIntent(MarketInsightsIntent.TicketQuantityChanged("7"))
        runCurrent()
        val invalid = viewModel.uiState.value.ticket!!
        assertTrue(!invalid.validation.isSubmittable, "7 is not a multiple of the 1000 lot size")
    }

    @Test
    fun `feed statistics are published once per second, not per tick`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        runCurrent()
        repeat(200) { tick -> boards.emit(board(revision = tick.toLong(), last = 100_000_000)) }
        runCurrent()
        val withinWindow = viewModel.uiState.value.feedStats
        assertEquals(0, withinWindow.boardsReceivedPerSecond, "no publish inside the 1 s window")

        clock = NOW + 1_000
        boards.emit(board(revision = 200, last = 100_000_000))
        runCurrent()

        val stats = viewModel.uiState.value.feedStats
        assertEquals(201, stats.boardsReceivedPerSecond)
        assertEquals(201 * instruments.size, stats.quoteUpdatesPerSecond)
    }

    private fun createViewModel() = MarketInsightsViewModel(
        marketRepository = FakeMarketDataRepository(instruments, boards),
        researchRepository = EmptyResearchRepository,
        boardStream = boards,
        nowEpochMillis = { clock },
        backgroundDispatcher = dispatcher,
    )

    private fun board(revision: Long, last: Long, sequence: Long = revision + 1): QuoteBoard {
        val quotes = instruments.map { instrument ->
            Quote(
                instrumentId = instrument.id,
                last = Price(last),
                bid = Price(last - 10_000),
                ask = Price(last + 10_000),
                previousClose = Price(100_000_000),
                yieldBps = null,
                sizeAtBid = 1_000,
                sizeAtAsk = 1_000,
                updatedAtEpochMillis = NOW,
                tickDirection = TickDirection.UP,
                sequence = sequence,
            )
        }.toImmutableList()
        return QuoteBoard(
            quotes = quotes,
            byId = persistentMapOf<InstrumentId, Quote>().putAll(quotes.associateBy { it.instrumentId }),
            revision = revision,
            phase = MarketPhase.OPEN,
        )
    }

    private fun instrument(symbol: String, assetClass: AssetClass) = Instrument(
        id = InstrumentId(symbol),
        symbol = symbol,
        name = symbol,
        assetClass = assetClass,
        currency = "USD",
        tickSize = Price(1_000_000),
        lotSize = 1_000,
    )


}

private const val NOW = 1_758_700_000_000L

private class FakeMarketDataRepository(
    override val instruments: ImmutableList<Instrument>,
    private val boards: Flow<QuoteBoard>,
) : MarketDataRepository {
    override fun boardStream(): Flow<QuoteBoard> = boards
}

private object EmptyResearchRepository : ResearchRepository {
    override suspend fun researchCalls(): ImmutableList<ResearchCall> = persistentListOf()
}
