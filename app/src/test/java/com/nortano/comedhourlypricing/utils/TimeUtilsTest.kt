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
}
