package com.nortano.comedhourlypricing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceUiStateTest {
    @Test
    fun `PriceTier fromPrice returns correct tier`() {
        assertEquals(PriceTier.FREE, PriceTier.fromPrice("-0.5"))
        assertEquals(PriceTier.FREE, PriceTier.fromPrice("0.0"))
        assertEquals(PriceTier.NORMAL, PriceTier.fromPrice("2.4"))
        assertEquals(PriceTier.NORMAL, PriceTier.fromPrice("5.0"))
        assertEquals(PriceTier.ELEVATED, PriceTier.fromPrice("5.1"))
        assertEquals(PriceTier.ELEVATED, PriceTier.fromPrice("9.9"))
        assertEquals(PriceTier.HIGH, PriceTier.fromPrice("10.0"))
        assertEquals(PriceTier.HIGH, PriceTier.fromPrice("15.0"))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice(null))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("abc"))
    }

    @Test
    fun `PriceTrend calculate returns correct trend`() {
        assertEquals(PriceTrend.UP, PriceTrend.calculate("3.0", "2.5"))
        assertEquals(PriceTrend.DOWN, PriceTrend.calculate("2.0", "2.5"))
        assertEquals(PriceTrend.STAGNANT, PriceTrend.calculate("2.5", "2.5"))
        assertEquals(PriceTrend.UNKNOWN, PriceTrend.calculate(null, "2.5"))
        assertEquals(PriceTrend.UNKNOWN, PriceTrend.calculate("2.5", null))
        assertEquals(PriceTrend.UNKNOWN, PriceTrend.calculate("abc", "2.5"))
    }

    @Test
    fun `PriceTrend calculate returns UNKNOWN when hourlyAvgPrice is non-numeric`() {
        // Exercises the second guard: hourlyAvgPrice?.toDoubleOrNull() ?: return UNKNOWN
        assertEquals(PriceTrend.UNKNOWN, PriceTrend.calculate("2.5", "not-a-number"))
        assertEquals(PriceTrend.UNKNOWN, PriceTrend.calculate("2.5", ""))
    }
}
