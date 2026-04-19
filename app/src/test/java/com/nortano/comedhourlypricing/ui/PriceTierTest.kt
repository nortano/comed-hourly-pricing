package com.nortano.comedhourlypricing.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceTierTest {

    @Test
    fun `fromPrice returns UNKNOWN for null or invalid input`() {
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice(null))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("abc"))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice(""))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("NaN"))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("Infinity"))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("-Infinity"))
    }

    @Test
    fun `fromPrice returns FREE for 0 or negative prices`() {
        assertEquals(PriceTier.FREE, PriceTier.fromPrice("0.0"))
        assertEquals(PriceTier.FREE, PriceTier.fromPrice("0"))
        assertEquals(PriceTier.FREE, PriceTier.fromPrice("-1.5"))
    }

    @Test
    fun `fromPrice returns NORMAL for prices between 0 and 5`() {
        assertEquals(PriceTier.NORMAL, PriceTier.fromPrice("0.1"))
        assertEquals(PriceTier.NORMAL, PriceTier.fromPrice("2.5"))
        assertEquals(PriceTier.NORMAL, PriceTier.fromPrice("5.0"))
    }

    @Test
    fun `fromPrice returns ELEVATED for prices between 5 and 9_9`() {
        assertEquals(PriceTier.ELEVATED, PriceTier.fromPrice("5.1"))
        assertEquals(PriceTier.ELEVATED, PriceTier.fromPrice("7.5"))
        assertEquals(PriceTier.ELEVATED, PriceTier.fromPrice("9.9"))
    }

    @Test
    fun `fromPrice returns HIGH for prices above 9_9`() {
        assertEquals(PriceTier.HIGH, PriceTier.fromPrice("10.0"))
        assertEquals(PriceTier.HIGH, PriceTier.fromPrice("20.5"))
    }
}
