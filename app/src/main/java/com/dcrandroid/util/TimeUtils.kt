/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util

import android.content.Context
import com.dcrandroid.R
import java.util.concurrent.TimeUnit

object TimeUtils {
    fun getDaysBehind(seconds: Long, context: Context): String {
        val days = TimeUnit.SECONDS.toDays(seconds)
        return if (days == 1L) {
            context.getString(R.string.one_day_behind)
        } else context.getString(R.string.days_behind, days)

    }

    fun calculateTime(seconds: Long, context: Context): String {
        var secs = seconds
        if (secs > 59) {

            // convert to minutes
            val minutes = secs / 60

            if (minutes > 59) {

                // convert to hours
                val hours = minutes / 60

                if (hours > 23) {

                    // convert to days
                    val days = hours / 24
                    formatDays(context, days)
                }

                return formatHours(context, hours)
            }

            return formatMinute(context, minutes)
        }

        if (secs < 0) {
            secs = 0
        }

        //seconds
        return formatSeconds(context, secs)
    }

    private fun formatSeconds(context: Context, seconds: Long): String {
        if (seconds == 1L) {
            return context.getString(R.string.one_second)
        }

        return context.getString(R.string.x_seconds, seconds)
    }

    private fun formatMinute(context: Context, minutes: Long): String {
        if (minutes == 1L) {
            return context.getString(R.string.one_minute)
        }

        return context.getString(R.string.x_minutes, minutes)
    }

    private fun formatHours(context: Context, hours: Long): String {
        if (hours == 1L) {
            return context.getString(R.string.one_hour)
        }

        return context.getString(R.string.x_hours, hours)
    }

    private fun formatDays(context: Context, days: Long): String {
        if (days == 1L) {
            return context.getString(R.string.one_day)
        }

        return context.getString(R.string.x_days, days)
    }

    fun getSyncTimeRemaining(seconds: Long, percentageCompleted: Int, ctx: Context): String {
        if (seconds > 1) {

            if (seconds > 60) {
                val minutes = seconds / 60
                return ctx.getString(R.string.remaining_minute_sync_eta, percentageCompleted, minutes)
            }

            return ctx.getString(R.string.remaining_seconds_sync_eta, percentageCompleted, seconds)
        }

        return ctx.getString(R.string.remaining_sync_eta_less_than_seconds, percentageCompleted)
    }

    fun getSyncTimeRemaining(seconds: Long, ctx: Context): String {
        if (seconds > 60) {
            val minutes = seconds / 60
            return ctx.getString(R.string.time_left_minutes, minutes)
        }

        return ctx.getString(R.string.time_left_seconds, seconds)
    }
}