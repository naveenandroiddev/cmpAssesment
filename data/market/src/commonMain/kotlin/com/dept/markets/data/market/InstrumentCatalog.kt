package com.dept.markets.data.market

import com.dept.markets.core.model.AssetClass
import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.InstrumentId
import com.dept.markets.core.model.Price
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object InstrumentCatalog {

    val all: ImmutableList<Instrument> = buildList {
        // ---- Government bonds: quoted in price, watched in yield ----------------
        add(bond("US10Y", "US Treasury 10Y 4.25% 2035", AssetClass.GOVERNMENT_BOND))
        add(bond("US02Y", "US Treasury 2Y 4.75% 2027", AssetClass.GOVERNMENT_BOND))
        add(bond("US30Y", "US Treasury 30Y 4.50% 2055", AssetClass.GOVERNMENT_BOND))
        add(bond("DE10Y", "Bundesanleihe 10Y 2.60% 2035", AssetClass.GOVERNMENT_BOND, "EUR"))
        add(bond("GB10Y", "UK Gilt 10Y 4.00% 2035", AssetClass.GOVERNMENT_BOND, "GBP"))
        add(bond("JP10Y", "JGB 10Y 1.10% 2035", AssetClass.GOVERNMENT_BOND, "JPY"))
        add(bond("IT10Y", "BTP 10Y 3.85% 2035", AssetClass.GOVERNMENT_BOND, "EUR"))
        add(bond("FR10Y", "OAT 10Y 3.10% 2035", AssetClass.GOVERNMENT_BOND, "EUR"))

        // ---- Corporate credit ----------------------------------------------------
        add(bond("AAPL31", "Apple Inc 3.85% 2031", AssetClass.CORPORATE_BOND))
        add(bond("MSFT32", "Microsoft Corp 4.10% 2032", AssetClass.CORPORATE_BOND))
        add(bond("JPM29", "JPMorgan Chase 5.20% 2029", AssetClass.CORPORATE_BOND))
        add(bond("VOD30", "Vodafone Group 4.65% 2030", AssetClass.CORPORATE_BOND, "EUR"))

        // ---- Equities -------------------------------------------------------------
        add(equity("AAPL", "Apple Inc"))
        add(equity("MSFT", "Microsoft Corp"))
        add(equity("NVDA", "NVIDIA Corp"))
        add(equity("JPM", "JPMorgan Chase & Co"))
        add(equity("GS", "Goldman Sachs Group"))
        add(equity("TSLA", "Tesla Inc"))
        add(equity("AMZN", "Amazon.com Inc"))
        add(equity("META", "Meta Platforms Inc"))
        add(equity("ASML", "ASML Holding NV", "EUR"))
        add(equity("SAP", "SAP SE", "EUR"))
        add(equity("HSBA", "HSBC Holdings plc", "GBP"))
        add(equity("SHEL", "Shell plc", "GBP"))
    }.toImmutableList()

    private fun bond(
        symbol: String,
        name: String,
        assetClass: AssetClass,
        currency: String = "USD",
    ) = Instrument(
        id = InstrumentId(symbol),
        symbol = symbol,
        name = name,
        assetClass = assetClass,
        currency = currency,
        // 1/64 of a point, the convention for treasuries.
        tickSize = Price(15_625),
        lotSize = 1_000,
        faceValue = 1,
    )

    private fun equity(symbol: String, name: String, currency: String = "USD") = Instrument(
        id = InstrumentId(symbol),
        symbol = symbol,
        name = name,
        assetClass = AssetClass.EQUITY,
        currency = currency,
        tickSize = Price(10_000), // one cent
        lotSize = 1,
        faceValue = 1,
    )
}
