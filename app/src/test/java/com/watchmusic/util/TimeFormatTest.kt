package com.watchmusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun clock_formatsMinutesAndSeconds() {
        assertEquals("0:00", TimeFormat.clock(0L))
        assertEquals("0:00", TimeFormat.clock(-1234L))
        assertEquals("0:00", TimeFormat.clock(999L))
        assertEquals("0:01", TimeFormat.clock(1_000L))
        assertEquals("0:59", TimeFormat.clock(59_999L))
        assertEquals("1:00", TimeFormat.clock(60_000L))
        assertEquals("12:34", TimeFormat.clock(754_000L))
    }

    @Test
    fun clock_includesHoursWhenNeeded() {
        assertEquals("1:00:00", TimeFormat.clock(3_600_000L))
        assertEquals("1:01:01", TimeFormat.clock(3_661_000L))
    }

    @Test
    fun countdown_roundsUpSoItNeverShowsZeroEarly() {
        assertEquals("0:00", TimeFormat.countdown(0L))
        assertEquals("0:01", TimeFormat.countdown(1L))
        assertEquals("0:01", TimeFormat.countdown(999L))
        assertEquals("0:01", TimeFormat.countdown(1_000L))
        assertEquals("0:02", TimeFormat.countdown(1_001L))
        assertEquals("1:00", TimeFormat.countdown(60_000L))
        assertEquals("1:00:00", TimeFormat.countdown(3_600_000L))
    }
}
