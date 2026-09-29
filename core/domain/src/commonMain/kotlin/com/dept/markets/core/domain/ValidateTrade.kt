package com.dept.markets.core.domain

import com.dept.markets.core.model.Instrument
import com.dept.markets.core.model.TradeIntent

class ValidateTrade(
    private val validator: TradeValidator = TradeValidator(),
) {
    operator fun invoke(
        intent: TradeIntent,
        instrument: Instrument,
        board: QuoteBoard,
        nowEpochMillis: Long,
    ): TradeValidation = validator.validate(
        intent = intent,
        instrument = instrument,
        quote = board.byId[intent.instrumentId],
        marketPhase = board.phase,
        nowEpochMillis = nowEpochMillis,
    )
}
