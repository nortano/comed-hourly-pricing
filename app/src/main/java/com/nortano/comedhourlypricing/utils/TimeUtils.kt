package com.nortano.comedhourlypricing.utils

import android.content.Context
import com.nortano.comedhourlypricing.R

fun getRelativeTime(context: Context, timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diffMillis = nowMillis - timestampMillis
    val minutes = diffMillis / (1000 * 60)
    
    return when {
        minutes <= 1 -> context.getString(R.string.updated_now)
        minutes < 60 -> context.getString(R.string.updated_minutes_ago, minutes.toInt())
        else -> {
            val hours = minutes / 60
            context.getString(R.string.updated_hours_ago, hours.toInt())
        }
    }
}
