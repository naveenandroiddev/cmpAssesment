package com.dept.markets.feature.marketinsights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dept.markets.core.designsystem.theme.MarketTheme
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.feature.marketinsights.component.BoardFilters
import com.dept.markets.feature.marketinsights.component.FeedStatusBar
import com.dept.markets.feature.marketinsights.component.QuoteGridHeader
import com.dept.markets.feature.marketinsights.component.QuoteRow
import com.dept.markets.feature.marketinsights.component.ResearchCard
import com.dept.markets.feature.marketinsights.component.TradeTicketPanel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun MarketInsightsScreen(
    viewModel: MarketInsightsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            val message = when (effect) {
                is MarketInsightsEffect.OrderAccepted ->
                    "Order accepted: ${effect.quantity} ${effect.symbol}"

                is MarketInsightsEffect.OrderRejected -> effect.reason
            }
            snackbarHostState.showSnackbar(message)
        }
    }


    LaunchedEffect(viewModel) {
        var frames = 0
        var windowStart = 0L
        while (true) {
            withFrameNanos { nanos ->
                if (windowStart == 0L) windowStart = nanos
                frames++
                if (nanos - windowStart >= 1_000_000_000L) {
                    viewModel.onFrameRendered(frames)
                    frames = 0
                    windowStart = nanos
                }
            }
        }
    }

    MarketInsightsContent(
        state = state,
        rows = viewModel.visibleRows,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun MarketInsightsContent(
    state: MarketInsightsUiState,
    rows: ImmutableList<MarketRowState>,
    snackbarHostState: SnackbarHostState,
    onIntent: (MarketInsightsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val researchByInstrument = remember(state.researchCalls) { state.researchByInstrument }

    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                FeedStatusBar(
                    stats = state.feedStats,
                    phase = state.phase,
                    stressMode = state.stressMode,
                    onStressModeChange = { onIntent(MarketInsightsIntent.StressModeToggled(it)) },
                )
                BoardFilters(
                    selected = state.filter,
                    onSelect = { onIntent(MarketInsightsIntent.FilterSelected(it)) },
                )
                QuoteGridHeader()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = rows,
                        key = { row -> row.instrument.id.value },
                        contentType = { "quote" },
                    ) { row ->
                        QuoteRow(
                            row = row,
                            hasResearch = row.instrument.id in researchByInstrument,
                            onClick = { id: InstrumentId ->
                                onIntent(MarketInsightsIntent.InstrumentSelected(id))
                            },
                        )
                    }

                    item(key = "research-header", contentType = "section") {
                        Text(
                            text = "Analyst calls",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp),
                        )
                    }

                    if (state.isLoadingResearch) {
                        item(key = "research-loading", contentType = "section") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator() }
                        }
                    }

                    items(
                        items = state.researchCalls,
                        key = { call -> call.id },
                        contentType = { "research" },
                    ) { call ->
                        ResearchCard(
                            call = call,
                            onClick = { id -> onIntent(MarketInsightsIntent.InstrumentSelected(id)) },
                        )
                    }

                    item(key = "footer", contentType = "section") {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Prices are simulated. ${rows.size} instruments subscribed.",
                                style = MarketTheme.typography.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            state.ticket?.let { ticket ->
                TradeTicketPanel(
                    ticket = ticket,
                    onIntent = onIntent,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
