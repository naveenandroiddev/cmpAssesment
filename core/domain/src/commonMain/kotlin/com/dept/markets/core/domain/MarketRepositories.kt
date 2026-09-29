package com.dept.markets.core.domain

import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.MarketPhase
import com.dept.markets.core.model.Quote
import com.dept.markets.core.model.ResearchCall
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.coroutines.flow.Flow

data class QuoteBoard(
    val quotes: ImmutableList<Quote>,
    val byId: ImmutableMap<InstrumentId, Quote>,
    val revision: Long,
    val phase: MarketPhase,
)

interface MarketDataRepository {
    val instruments: ImmutableList<Instrument>

    fun boardStream(): Flow<QuoteBoard>
}

interface ResearchRepository {

    suspend fun researchCalls(): ImmutableList<ResearchCall>
}
