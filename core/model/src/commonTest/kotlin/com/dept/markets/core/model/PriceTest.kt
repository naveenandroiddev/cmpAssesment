package com.dept.markets.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PriceTest {

    @Test
    fun `parses and formats without a floating point intermediate`() {
        assertEquals(Price(100_100_000), Price.parseOrNull("100.10"))
        assertEquals("100.10", Price.parseOrNull("100.10")!!.format(2))
        assertEquals("0.000001", Price(1).format(6))
        assertEquals("-3.50", Price.parseOrNull("-3.5")!!.format(2))
    }

    @Test
    fun `the classic float trap does not exist here`() {
        val tenth = Price.parseOrNull("0.1")!!
        val fifth = Price.parseOrNull("0.2")!!
        assertEquals(Price.parseOrNull("0.3"), tenth + fifth)
    }

    @Test
    fun `rejects malformed input instead of throwing`() {
        assertNull(Price.parseOrNull("1.2.3"))
        assertNull(Price.parseOrNull("abc"))
        assertNull(Price.parseOrNull(""))
    }

    @Test
    fun `basis point change is rounded half away from zero`() {
        val close = Price.ofUnits(100)
        assertEquals(BasisPoints(100), Price.parseOrNull("101")!!.changeBpsFrom(close))
        assertEquals(BasisPoints(-50), Price.parseOrNull("99.5")!!.changeBpsFrom(close))
        assertEquals(BasisPoints.ZERO, Price.ofUnits(1).changeBpsFrom(Price.ZERO))
    }
}
