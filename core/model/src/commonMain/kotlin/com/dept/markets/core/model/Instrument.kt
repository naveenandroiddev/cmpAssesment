package com.dept.markets.core.model

import kotlin.jvm.JvmInline

@JvmInline
value class InstrumentId(val value: String)

enum class AssetClass {
    GOVERNMENT_BOND,
    CORPORATE_BOND,
    EQUITY,
    ;

    val isBond: Boolean get() = this == GOVERNMENT_BOND || this == CORPORATE_BOND
}

data class Instrument(
    val id: InstrumentId,
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val currency: String,
    /** Smallest permitted price increment. Orders off the tick grid are rejected by the venue. */
    val tickSize: Price,
    /** Minimum tradable quantity increment. */
    val lotSize: Long,
    /** Bonds only: face value per lot, used for notional. Equities use 1. */
    val faceValue: Long = 1L,
)
