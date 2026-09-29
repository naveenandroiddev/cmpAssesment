package com.dept.markets.data.market

import com.dept.markets.core.domain.ResearchRepository
import com.dept.markets.core.model.Conviction
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.Price
import com.dept.markets.core.model.Rating
import com.dept.markets.core.model.ResearchCall
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay


class StaticResearchRepository(
    private val artificialLatencyMillis: Long = 350L,
) : ResearchRepository {

    override suspend fun researchCalls(): ImmutableList<ResearchCall> {
        delay(artificialLatencyMillis)
        return CALLS
    }

    private companion object {
        val CALLS: ImmutableList<ResearchCall> = listOf(
            ResearchCall(
                id = "RC-1041",
                instrumentId = InstrumentId("US10Y"),
                analyst = "R. Okonkwo",
                headline = "Duration add ahead of the September refunding; 10s look cheap vs 5s30s",
                rating = Rating.BUY,
                conviction = Conviction.HIGH,
                targetPrice = Price.parseOrNull("101.50")!!,
                publishedAtEpochMillis = 1_758_700_000_000L,
            ),
            ResearchCall(
                id = "RC-1042",
                instrumentId = InstrumentId("DE10Y"),
                analyst = "S. Lindqvist",
                headline = "Bund spread compression has run its course; take profit",
                rating = Rating.SELL,
                conviction = Conviction.MEDIUM,
                targetPrice = Price.parseOrNull("97.25")!!,
                publishedAtEpochMillis = 1_758_690_000_000L,
            ),
            ResearchCall(
                id = "RC-1043",
                instrumentId = InstrumentId("NVDA"),
                analyst = "T. Bhatt",
                headline = "Datacentre backlog supports upgrade; raising target",
                rating = Rating.BUY,
                conviction = Conviction.HIGH,
                targetPrice = Price.parseOrNull("214.00")!!,
                publishedAtEpochMillis = 1_758_680_000_000L,
            ),
            ResearchCall(
                id = "RC-1044",
                instrumentId = InstrumentId("JPM"),
                analyst = "M. Delacroix",
                headline = "NII guidance conservative into Q4; constructive but priced",
                rating = Rating.HOLD,
                conviction = Conviction.MEDIUM,
                targetPrice = Price.parseOrNull("198.00")!!,
                publishedAtEpochMillis = 1_758_670_000_000L,
            ),
            ResearchCall(
                id = "RC-1045",
                instrumentId = InstrumentId("AAPL31"),
                analyst = "R. Okonkwo",
                headline = "Credit curve too flat for the refinancing calendar; switch to 2029s",
                rating = Rating.SELL,
                conviction = Conviction.LOW,
                targetPrice = Price.parseOrNull("99.10")!!,
                publishedAtEpochMillis = 1_758_660_000_000L,
            ),
            ResearchCall(
                id = "RC-1046",
                instrumentId = InstrumentId("IT10Y"),
                analyst = "G. Ferrante",
                headline = "BTP-Bund spread: carry still compensates for the political tail",
                rating = Rating.BUY,
                conviction = Conviction.MEDIUM,
                targetPrice = Price.parseOrNull("100.80")!!,
                publishedAtEpochMillis = 1_758_650_000_000L,
            ),
            ResearchCall(
                id = "RC-1047",
                instrumentId = InstrumentId("TSLA"),
                analyst = "T. Bhatt",
                headline = "Delivery mix deteriorating; downgrade on margin path",
                rating = Rating.SELL,
                conviction = Conviction.HIGH,
                targetPrice = Price.parseOrNull("186.50")!!,
                publishedAtEpochMillis = 1_758_640_000_000L,
            ),
            ResearchCall(
                id = "RC-1048",
                instrumentId = InstrumentId("ASML"),
                analyst = "S. Lindqvist",
                headline = "High-NA order book de-risks 2027; initiate at Buy",
                rating = Rating.BUY,
                conviction = Conviction.HIGH,
                targetPrice = Price.parseOrNull("742.00")!!,
                publishedAtEpochMillis = 1_758_630_000_000L,
            ),
        ).toImmutableList()
    }
}
