package com.dept.markets.core.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.sample
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class ObserveMarketBoard(
    private val repository: MarketDataRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(framePeriod: Duration = DEFAULT_FRAME_PERIOD): Flow<QuoteBoard> =
        repository.boardStream()
            .conflate()
            .sample(framePeriod)

    companion object {
        val DEFAULT_FRAME_PERIOD: Duration = 16.milliseconds
    }
}
