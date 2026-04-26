package com.nortano.comedhourlypricing.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PriceTierTest {
    @Test
    fun `fromPrice returns UNKNOWN for null or non-numeric input`() {
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice(null))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice("abc"))
        assertEquals(PriceTier.UNKNOWN, PriceTier.fromPrice(""))
        // "NaN", "Infinity", "-Infinity" are not parseable by toDoubleOrNull() and return null,
        // so they are caught by the null-return guard on the first line of fromPrice, not by
        // the explicit isNaN()/isInfinite() checks. The outcome is still UNKNOWN either way.
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

    // --- color extension property ---

    @Test
    fun `color returns correct color for each tier`() {
        assertEquals(Color(0xFF00E5FF), PriceTier.FREE.color)
        assertEquals(Color(0xFF00E676), PriceTier.NORMAL.color)
        assertEquals(Color(0xFFFFEA00), PriceTier.ELEVATED.color)
        assertEquals(Color(0xFFFF1744), PriceTier.HIGH.color)
        assertEquals(Color.White, PriceTier.UNKNOWN.color)
    }
}
