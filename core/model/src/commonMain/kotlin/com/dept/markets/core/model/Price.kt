package com.dept.markets.core.model

import kotlin.jvm.JvmInline


@JvmInline
value class Price(val micros: Long) : Comparable<Price> {

    val isZero: Boolean get() = micros == 0L

    operator fun plus(other: Price): Price = Price(micros + other.micros)

    operator fun minus(other: Price): Price = Price(micros - other.micros)

    operator fun times(factor: Long): Price = Price(micros * factor)

    override fun compareTo(other: Price): Int = micros.compareTo(other.micros)

    fun changeBpsFrom(reference: Price): BasisPoints {
        if (reference.micros == 0L) return BasisPoints.ZERO
        val delta = micros - reference.micros
        val scaled = delta * 10_000L * 2L / reference.micros
        val rounded = if (scaled >= 0) (scaled + 1) / 2 else (scaled - 1) / 2
        return BasisPoints(rounded.toInt())
    }

    fun format(decimals: Int = 2): String {
        require(decimals in 0..6) { "decimals must be in 0..6, was $decimals" }
        val negative = micros < 0
        val abs = if (negative) -micros else micros
        val divisor = POW10[6 - decimals]
        val rounded = (abs + divisor / 2) / divisor
        val unitPart = rounded / POW10[decimals]
        val fractionPart = rounded % POW10[decimals]
        return buildString {
            if (negative) append('-')
            append(unitPart)
            if (decimals > 0) {
                append('.')
                val digits = fractionPart.toString()
                repeat(decimals - digits.length) { append('0') }
                append(digits)
            }
        }
    }

    override fun toString(): String = format(4)

    companion object {
        val ZERO: Price = Price(0)
        private const val MICROS_PER_UNIT: Long = 1_000_000L
        private val POW10 = longArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)

        fun ofUnits(units: Long): Price = Price(units * MICROS_PER_UNIT)

        fun parseOrNull(text: String): Price? {
            if (text.isEmpty()) return null
            var index = 0
            var negative = false
            when (text[0]) {
                '-' -> { negative = true; index = 1 }
                '+' -> index = 1
            }
            if (index >= text.length) return null
            var units = 0L
            var fraction = 0L
            var fractionDigits = 0
            var seenDot = false
            var seenDigit = false
            while (index < text.length) {
                val char = text[index]
                when {
                    char == '.' && !seenDot -> seenDot = true
                    char in '0'..'9' -> {
                        seenDigit = true
                        val digit = (char - '0').toLong()
                        if (seenDot) {
                            if (fractionDigits < 6) {
                                fraction = fraction * 10 + digit
                                fractionDigits++
                            }
                        } else {
                            units = units * 10 + digit
                        }
                    }
                    else -> return null
                }
                index++
            }
            if (!seenDigit) return null
            var scaledFraction = fraction
            repeat(6 - fractionDigits) { scaledFraction *= 10 }
            val micros = units * MICROS_PER_UNIT + scaledFraction
            return Price(if (negative) -micros else micros)
        }
    }
}

@JvmInline
value class BasisPoints(val value: Int) : Comparable<BasisPoints> {
    override fun compareTo(other: BasisPoints): Int = value.compareTo(other.value)

    fun format(): String {
        val sign = if (value > 0) "+" else ""
        val whole = value / 100
        val cents = if (value < 0) -(value % 100) else value % 100
        return "$sign$whole.${cents.toString().padStart(2, '0')}%"
    }

    fun formatBps(): String = if (value > 0) "+$value bp" else "$value bp"

    companion object {
        val ZERO: BasisPoints = BasisPoints(0)
    }
}
