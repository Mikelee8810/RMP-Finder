package com.mike.rmpfinder.ui

import com.mike.rmpfinder.data.TimePeriod
import com.mike.rmpfinder.data.WeeklyHours
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

class OvernightHoursTest {
    private val overnight = WeeklyHours(
        timezone = "America/New_York",
        monday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        tuesday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        wednesday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        thursday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        friday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        saturday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
        sunday = listOf(TimePeriod("00:00", "05:00"), TimePeriod("10:00", "24:00")),
    )

    @org.junit.Test
    fun `overnight span reads as one range on the day it starts`() {
        org.junit.Assert.assertEquals("10 AM – 5 AM", displayPeriods(overnight, java.time.DayOfWeek.MONDAY))
    }

    @org.junit.Test
    fun `a plain day is unchanged`() {
        val plain = overnight.copy(monday = listOf(TimePeriod("11:00", "22:00")), tuesday = listOf(TimePeriod("11:00", "22:00")), sunday = listOf(TimePeriod("11:00", "22:00")))
        org.junit.Assert.assertEquals("11 AM – 10 PM", displayPeriods(plain, java.time.DayOfWeek.MONDAY))
    }
}
