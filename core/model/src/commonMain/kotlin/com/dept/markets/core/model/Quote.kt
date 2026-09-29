package com.dept.markets.core.model

enum class TickDirection { UP, DOWN, FLAT }


data class Quote(
    val instrumentId: InstrumentId,
    val last: Price,
    val bid: Price,
    val ask: Price,
    val previousClose: Price,
    val yieldBps: BasisPoints?,
    val sizeAtBid: Long,
    val sizeAtAsk: Long,
    val updatedAtEpochMillis: Long,
    val tickDirection: TickDirection,
    val sequence: Long,
) {
    val spread: Price get() = ask - bid

    val changeFromCloseBps: BasisPoints get() = last.changeBpsFrom(previousClose)

    val isStale: Boolean get() = false
}
