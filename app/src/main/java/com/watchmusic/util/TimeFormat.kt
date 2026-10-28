package com.watchmusic.util

/** Compact duration formatting for the small watch screen. */
object TimeFormat {

    fun clock(millis: Long): String {
        if (millis <= 0L) return "0:00"
        val totalSeconds = millis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** Remaining time for the sleep timer; rounds up so it never shows 0:00 early. */
    fun countdown(millis: Long): String {
        if (millis <= 0L) return "0:00"
        val totalSeconds = (millis + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
