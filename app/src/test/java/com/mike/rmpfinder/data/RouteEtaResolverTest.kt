package com.mike.rmpfinder.data

import com.mike.rmpfinder.TransitMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RouteEtaResolverTest {

    @Before
    fun setUp() {
        RouteEtaResolver.clearCache()
    }

    @Test
    fun testParseOsrmResponseSuccess() {
        val sampleJson = """
            {
                "code": "Ok",
                "routes": [
                    {
                        "duration": 960.0,
                        "distance": 1609.34
                    }
                ]
            }
        """.trimIndent()

        val eta = RouteEtaResolver.parseOsrmResponse(sampleJson, isScooter = false)
        assertNotNull(eta)
        assertEquals(16, eta!!.durationMinutes) // 960s = 16 min
        assertEquals(1.0, eta.distanceMiles, 0.01) // 1609.34m = 1.0 mi
        assertTrue(eta.isLiveStreetRoute)
        assertEquals("🚶 16 min street", eta.formatted(TransitMode.WALKING))
    }

    @Test
    fun testParseOsrmResponseScooterAdjustment() {
        val sampleJson = """
            {
                "code": "Ok",
                "routes": [
                    {
                        "duration": 600.0,
                        "distance": 2000.0
                    }
                ]
            }
        """.trimIndent()

        // Scooter applies 0.8x duration factor (600s * 0.8 = 480s = 8 min)
        val eta = RouteEtaResolver.parseOsrmResponse(sampleJson, isScooter = true)
        assertNotNull(eta)
        assertEquals(8, eta!!.durationMinutes)
        assertEquals(1.24, eta.distanceMiles, 0.02)
        assertTrue(eta.isLiveStreetRoute)
        assertEquals("🛴 8 min street", eta.formatted(TransitMode.SCOOTER))
    }

    @Test
    fun testParseOsrmResponseErrorOrEmpty() {
        val errorJson = """{"code": "NoRoute", "routes": []}"""
        assertNull(RouteEtaResolver.parseOsrmResponse(errorJson))

        val invalidJson = "invalid json content"
        assertNull(RouteEtaResolver.parseOsrmResponse(invalidJson))
    }

    @Test
    fun testFallbackEtaCalculatesReasonableTimes() {
        // Midtown (40.7580, -73.9855) to Lower Manhattan (40.7128, -74.0060): ~3.3 straight miles
        val lat1 = 40.7580
        val lon1 = -73.9855
        val lat2 = 40.7128
        val lon2 = -74.0060

        val walkingEta = RouteEtaResolver.fallbackEta(lat1, lon1, lat2, lon2, TransitMode.WALKING)
        assertFalse(walkingEta.isLiveStreetRoute)
        assertTrue("Walking time should be > 60 mins for ~4.3 street miles", walkingEta.durationMinutes > 60)

        val drivingEta = RouteEtaResolver.fallbackEta(lat1, lon1, lat2, lon2, TransitMode.DRIVING)
        assertTrue("Driving time in NYC should be > 15 mins for ~4.5 street miles", drivingEta.durationMinutes >= 20)

        val transitEta = RouteEtaResolver.fallbackEta(lat1, lon1, lat2, lon2, TransitMode.TRANSIT)
        assertTrue("Transit time should include wait + ride", transitEta.durationMinutes >= 20)
    }

    @Test
    fun testCachingBehavior() = runBlocking {
        val lat1 = 40.7580
        val lon1 = -73.9855
        val lat2 = 40.7549
        val lon2 = -73.9840

        // Transit always uses fallback, so resolveEta is deterministic and exercises caching
        val eta1 = RouteEtaResolver.resolveEta(lat1, lon1, lat2, lon2, TransitMode.TRANSIT)
        val eta2 = RouteEtaResolver.resolveEta(lat1, lon1, lat2, lon2, TransitMode.TRANSIT)

        assertEquals(eta1, eta2)
    }
}
