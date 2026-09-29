package com.dept.markets.core.model

enum class Rating { BUY, HOLD, SELL }

enum class Conviction { HIGH, MEDIUM, LOW }

data class ResearchCall(
    val id: String,
    val instrumentId: InstrumentId,
    val analyst: String,
    val headline: String,
    val rating: Rating,
    val conviction: Conviction,
    val targetPrice: Price,
    val publishedAtEpochMillis: Long,
)
