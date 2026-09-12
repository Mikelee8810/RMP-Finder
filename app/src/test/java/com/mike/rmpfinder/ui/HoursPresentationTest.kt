package com.mike.rmpfinder.ui

import com.mike.rmpfinder.data.TimePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoursPresentationTest {
    @Test
    fun allDayPeriodIsPresentedAsOpen24Hours() {
        val periods = listOf(TimePeriod(open = "00:00", close = "24:00"))

        assertTrue(isOpen24Hours(periods))
        assertEquals("Open 24 hours", formatPeriods(periods))
    }

    @Test
    fun overnightCloseAtMidnightKeepsItsActualRange() {
        val periods = listOf(TimePeriod(open = "18:00", close = "24:00"))

        assertFalse(isOpen24Hours(periods))
        assertEquals("6 PM – Midnight", formatPeriods(periods))
    }
}
