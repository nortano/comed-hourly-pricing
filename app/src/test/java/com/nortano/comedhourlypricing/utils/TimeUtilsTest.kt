package com.nortano.comedhourlypricing.utils

import android.content.Context
import com.nortano.comedhourlypricing.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {
    private val context = mockk<Context>()

    @Test
    fun getRelativeTime_justNow() {
        val now = 1000000L
        val timestamp = now - 30 * 1000 // 30 seconds ago

        every { context.getString(R.string.updated_now) } returns "Updated just now"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated just now", result)
    }

    @Test
    fun getRelativeTime_minutesAgo() {
        val now = 1000000L
        val timestamp = now - 5 * 60 * 1000 // 5 minutes ago

        every { context.getString(R.string.updated_minutes_ago, 5) } returns "Updated 5m ago"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated 5m ago", result)
    }

    @Test
    fun getRelativeTime_hoursAgo() {
        val now = 1000000L
        val timestamp = now - 125 * 60 * 1000 // 2 hours and 5 minutes ago

        every { context.getString(R.string.updated_hours_ago, 2) } returns "Updated 2h ago"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated 2h ago", result)
    }

    // --- Boundary cases ---

    @Test
    fun getRelativeTime_exactlyOneMinute_isJustNow() {
        // minutes = 60000 / 60000 = 1, which satisfies minutes <= 1
        val now = 1000000L
        val timestamp = now - 60_000L // exactly 1 minute ago

        every { context.getString(R.string.updated_now) } returns "Updated just now"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated just now", result)
    }

    @Test
    fun getRelativeTime_exactlyTwoMinutes_isMinutesAgo() {
        // minutes = 120000 / 60000 = 2, first value that escapes the <= 1 guard
        val now = 1000000L
        val timestamp = now - 120_000L // exactly 2 minutes ago

        every { context.getString(R.string.updated_minutes_ago, 2) } returns "Updated 2m ago"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated 2m ago", result)
    }

    @Test
    fun getRelativeTime_exactly59Minutes_isMinutesAgo() {
        // Last value before the hours branch
        val now = 1000000L
        val timestamp = now - 59 * 60_000L

        every { context.getString(R.string.updated_minutes_ago, 59) } returns "Updated 59m ago"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated 59m ago", result)
    }

    @Test
    fun getRelativeTime_exactlyOneHour_isHoursAgo() {
        // minutes = 60, triggers the hours branch; hours = 1
        val now = 1000000L
        val timestamp = now - 60 * 60_000L

        every { context.getString(R.string.updated_hours_ago, 1) } returns "Updated 1h ago"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated 1h ago", result)
    }

    @Test
    fun getRelativeTime_futureTimestamp_isJustNow() {
        // diffMillis is negative → minutes is negative → satisfies minutes <= 1 → "just now"
        // This documents the current behavior for clock-skew scenarios.
        val now = 1000000L
        val timestamp = now + 60_000L // 1 minute in the future

        every { context.getString(R.string.updated_now) } returns "Updated just now"

        val result = getRelativeTime(context, timestamp, now)
        assertEquals("Updated just now", result)
    }
}
