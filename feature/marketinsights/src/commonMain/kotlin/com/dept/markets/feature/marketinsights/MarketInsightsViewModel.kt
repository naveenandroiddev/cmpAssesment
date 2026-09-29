package com.dept.markets.feature.marketinsights

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dept.markets.core.domain.FeedStressControl
import com.dept.markets.core.domain.MarketDataRepository
import com.dept.markets.core.domain.QuoteBoard
import com.dept.markets.core.domain.ResearchRepository
import com.dept.markets.core.domain.ValidateTrade
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.OrderType
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Side
import com.dept.markets.core.model.TradeIntent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class MarketInsightsViewModel(
    private val marketRepository: MarketDataRepository,
    private val researchRepository: ResearchRepository,
    private val boardStream: Flow<QuoteBoard>,
    private val validateTrade: ValidateTrade = ValidateTrade(),
    private val stressControl: FeedStressControl? = null,
    private val nowEpochMillis: () -> Long,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketInsightsUiState())
    val uiState: StateFlow<MarketInsightsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<MarketInsightsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    var visibleRows: ImmutableList<MarketRowState> by mutableStateOf(persistentListOf())
        private set

    private val rowsById = LinkedHashMap<InstrumentId, MarketRowState>()
    private var allRows: List<MarketRowState> = emptyList()

    private var latestBoard: QuoteBoard? = null

    private var boardsThisSecond = 0
    private var quoteUpdatesThisSecond = 0
    private var lastStatsPublishedAt = 0L

    init {
        observeBoard()
        loadResearch()
    }

    fun onIntent(intent: MarketInsightsIntent) {
        when (intent) {
            is MarketInsightsIntent.FilterSelected -> applyFilter(intent.filter)
            is MarketInsightsIntent.InstrumentSelected -> openTicket(intent.instrumentId)
            MarketInsightsIntent.TicketDismissed -> _uiState.update { it.copy(ticket = null) }
            is MarketInsightsIntent.TicketSideChanged -> editTicket { it.copy(side = intent.side) }
            is MarketInsightsIntent.TicketOrderTypeChanged -> editTicket {
                it.copy(
                    orderType = intent.orderType,
                    limitPriceText = if (intent.orderType == OrderType.MARKET) "" else it.limitPriceText,
                )
            }

            is MarketInsightsIntent.TicketQuantityChanged -> editTicket {
                it.copy(quantityText = intent.text.filter(Char::isDigit).take(9))
            }

            is MarketInsightsIntent.TicketLimitPriceChanged -> editTicket {
                it.copy(limitPriceText = intent.text.filter { char -> char.isDigit() || char == '.' }.take(12))
            }

            MarketInsightsIntent.TicketSubmitted -> submitTicket()
            is MarketInsightsIntent.StressModeToggled -> {
                stressControl?.setStressEnabled(intent.enabled)
                _uiState.update { it.copy(stressMode = intent.enabled) }
            }

            MarketInsightsIntent.ResearchRetried -> loadResearch()
        }
    }

    private fun observeBoard() {
        viewModelScope.launch(backgroundDispatcher) {
            boardStream.collect { board -> applyBoard(board) }
        }
    }

    private fun applyBoard(board: QuoteBoard) {
        latestBoard = board
        if (rowsById.isEmpty()) createRows(board)

        Snapshot.withMutableSnapshot {
            board.quotes.forEach { quote ->
                rowsById[quote.instrumentId]?.update(quote)
            }
        }

        boardsThisSecond++
        quoteUpdatesThisSecond += board.quotes.size
        publishFeedStatsIfDue()

        val currentPhase = _uiState.value.phase
        if (board.phase != currentPhase) {
            _uiState.update { it.copy(phase = board.phase) }
        }
    }

    private fun publishFeedStatsIfDue() {
        val now = nowEpochMillis()
        if (lastStatsPublishedAt == 0L) {
            lastStatsPublishedAt = now
            return
        }
        if (now - lastStatsPublishedAt < STATS_WINDOW_MILLIS) return
        val elapsed = (now - lastStatsPublishedAt).coerceAtLeast(1L)
        val boardsPerSecond = (boardsThisSecond * 1_000L / elapsed).toInt()
        val updatesPerSecond = (quoteUpdatesThisSecond * 1_000L / elapsed).toInt()
        lastStatsPublishedAt = now
        boardsThisSecond = 0
        quoteUpdatesThisSecond = 0
        _uiState.update {
            it.copy(
                feedStats = it.feedStats.copy(
                    boardsReceivedPerSecond = boardsPerSecond,
                    quoteUpdatesPerSecond = updatesPerSecond,
                ),
            )
        }
    }

    private fun createRows(board: QuoteBoard) {
        marketRepository.instruments.forEach { instrument ->
            val quote = board.byId[instrument.id] ?: return@forEach
            rowsById[instrument.id] = MarketRowState(instrument, quote)
        }
        allRows = rowsById.values.toList()
        recomputeVisibleRows()
    }

    private fun applyFilter(filter: BoardFilter) {
        _uiState.update { it.copy(filter = filter) }
        recomputeVisibleRows()
    }

    private fun recomputeVisibleRows() {
        val state = _uiState.value
        val withResearch = state.researchCalls.mapTo(HashSet()) { it.instrumentId }
        visibleRows = allRows
            .filter { row ->
                state.filter.matches(row.instrument.assetClass, row.instrument.id in withResearch)
            }
            .toImmutableList()
    }

    private fun loadResearch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingResearch = true) }
            val calls = researchRepository.researchCalls()
            _uiState.update { it.copy(isLoadingResearch = false, researchCalls = calls) }
            recomputeVisibleRows()
        }
    }

    fun onFrameRendered(framesPerSecond: Int) {
        _uiState.update { it.copy(feedStats = it.feedStats.copy(framesRenderedPerSecond = framesPerSecond)) }
    }

    private fun openTicket(instrumentId: InstrumentId) {
        val row = rowsById[instrumentId] ?: return
        val quote = row.quote
        _uiState.update {
            it.copy(
                ticket = TicketState(
                    instrumentId = instrumentId,
                    symbol = row.instrument.symbol,
                    quantityText = row.instrument.lotSize.toString(),
                    limitPriceText = quote.last.format(3),
                ),
            )
        }
        revalidateTicket()
    }

    private fun editTicket(transform: (TicketState) -> TicketState) {
        _uiState.update { state -> state.ticket?.let { state.copy(ticket = transform(it)) } ?: state }
        revalidateTicket()
    }

    private fun revalidateTicket() {
        val state = _uiState.value
        val ticket = state.ticket ?: return
        val board = latestBoard ?: return
        val instrument = rowsById[ticket.instrumentId]?.instrument ?: return
        val intent = ticket.toIntent() ?: return
        val validation = validateTrade(intent, instrument, board, nowEpochMillis())
        _uiState.update { it.copy(ticket = it.ticket?.copy(validation = validation)) }
    }

    private fun submitTicket() {
        val ticket = _uiState.value.ticket ?: return
        revalidateTicket()
        val validated = _uiState.value.ticket ?: return
        if (!validated.validation.isSubmittable) {
            viewModelScope.launch {
                _effects.send(MarketInsightsEffect.OrderRejected("Order failed pre-trade checks"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(ticket = it.ticket?.copy(isSubmitting = true)) }
            delay(450) // stand-in for the order gateway round trip
            _uiState.update { it.copy(ticket = null) }
            _effects.send(
                MarketInsightsEffect.OrderAccepted(
                    symbol = ticket.symbol,
                    quantity = ticket.quantityText.toLongOrNull() ?: 0L,
                ),
            )
        }
    }
}

private const val STATS_WINDOW_MILLIS = 1_000L

internal fun TicketState.toIntent(): TradeIntent? {
    val quantity = quantityText.toLongOrNull() ?: 0L
    val limitPrice: Price? = when (orderType) {
        OrderType.MARKET -> null
        OrderType.LIMIT -> Price.parseOrNull(limitPriceText)
    }
    return TradeIntent(
        instrumentId = instrumentId,
        side = side,
        quantity = quantity,
        orderType = orderType,
        limitPrice = limitPrice,
    )
}

internal val TicketState.sideLabel: String
    get() = if (side == Side.BUY) "Buy" else "Sell"
