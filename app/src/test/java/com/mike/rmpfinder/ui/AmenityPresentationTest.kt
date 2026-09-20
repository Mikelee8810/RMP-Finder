package com.mike.rmpfinder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AmenityPresentationTest {
    @Test
    fun `true maps to yes`() {
        assertEquals(AmenityStatus.YES, amenityStatus(true))
    }

    @Test
    fun `false maps to no`() {
        assertEquals(AmenityStatus.NO, amenityStatus(false))
    }

    @Test
    fun `null maps to explicit unknown`() {
        assertEquals(AmenityStatus.UNKNOWN, amenityStatus(null))
        assertEquals("Unknown", AmenityStatus.UNKNOWN.label)
        assertEquals("?", AmenityStatus.UNKNOWN.marker)
    }
}
