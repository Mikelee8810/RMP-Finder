package com.mike.rmpfinder.data

import com.mike.rmpfinder.BrowseFilters
import com.mike.rmpfinder.GeoPoint
import com.mike.rmpfinder.filterRestaurants
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetAcceptanceTest {
    private val datasetBytes by lazy { repoFile("data/restaurants.json").readBytes() }
    private val manifestBytes by lazy { repoFile("data/manifest.json").readBytes() }
    private val restaurants by lazy { DatasetCodec.parseDataset(datasetBytes) }

    @Test
    fun bundledDatasetPassesRuntimeGate() {
        val validated = DatasetValidator.validate(manifestBytes, datasetBytes, appVersion = 1)
        assertEquals(241, validated.restaurants.size)
        assertEquals(241, validated.restaurants.map { it.rmpKey }.toSet().size)
        assertEquals(241, validated.manifest.recordCount)
        assertTrue(validated.missingExistingKeys.isEmpty())
    }

    @Test
    fun checksumMismatchIsRejectedBeforeImport() {
        val corrupted = datasetBytes + byteArrayOf('\n'.code.toByte())
        val error = runCatching { DatasetValidator.validate(manifestBytes, corrupted, appVersion = 1) }.exceptionOrNull()
        assertTrue(error?.message?.contains("checksum") == true)
    }

    @Test
    fun recordCountMismatchIsRejected() {
        val original = manifestBytes.decodeToString()
        val wrongManifest = original.replace("\"recordCount\": 241", "\"recordCount\": 240").encodeToByteArray()
        val error = runCatching { DatasetValidator.validate(wrongManifest, datasetBytes, appVersion = 1) }.exceptionOrNull()
        assertTrue(error?.message?.contains("record count") == true)
    }

    @Test
    fun schemaFailureIsRejected() {
        val source = datasetBytes.decodeToString()
        val invalid = source.replaceFirst("\"rmpKey\":", "\"unexpectedField\": true, \"rmpKey\":").encodeToByteArray()
        val error = runCatching { DatasetCodec.entitiesFromDataset(invalid) }.exceptionOrNull()
        assertTrue(error?.message?.contains("Unexpected JSON field") == true)
    }

    @Test
    fun disappearedExistingKeyIsHeldForReview() {
        val allKeys = restaurants.map { it.rmpKey }.toSet()
        val syntheticOldKey = "bronx|removed test location|1 test street|10451"
        val validated = DatasetValidator.validate(manifestBytes, datasetBytes, appVersion = 1, existingKeys = allKeys + syntheticOldKey)
        assertEquals(listOf(syntheticOldKey), validated.missingExistingKeys)
    }

    @Test
    fun officialAndCurrentAddressesStaySeparateForKnownConflict() {
        val dunkin = restaurants.first { it.rmpKey.contains("2370 grand concourse road") }
        assertEquals("2370 Grand Concourse Road", dunkin.officialAddress.line1)
        assertEquals("2366 Grand Concourse", dunkin.currentAddress?.line1)
        assertNotEquals(dunkin.officialAddress, dunkin.currentAddress)
        assertTrue("address_mismatch" in dunkin.conflictFlags)
    }

    @Test
    fun unsafeHoursNeverProduceOpenNow() {
        val alwaysOpen = restaurants.first { restaurant ->
            restaurant.hoursStatus == "usable" && restaurant.hours?.monday?.any { it.open == "00:00" && it.close == "24:00" } == true
        }
        val mondayNoonNewYork = Instant.parse("2026-09-07T16:00:00Z")
        assertTrue(OpenNow.isOpen(alwaysOpen, mondayNoonNewYork))
        assertFalse(OpenNow.isOpen(alwaysOpen.copy(hoursStatus = "unknown"), mondayNoonNewYork))
        assertFalse(OpenNow.isOpen(alwaysOpen.copy(hoursStatus = "conflicting"), mondayNoonNewYork))
        assertFalse(OpenNow.isOpen(alwaysOpen.copy(businessStatus = "conflicting"), mondayNoonNewYork))
        assertFalse(OpenNow.isOpen(alwaysOpen.copy(businessStatus = "temporarily_closed"), mondayNoonNewYork))
    }

    @Test
    fun searchCoversOfficialAndCurrentAddressOffline() {
        val now = Instant.parse("2026-09-07T16:00:00Z")
        val official = filterRestaurants(restaurants, emptySet(), BrowseFilters(query = "2370 Grand Concourse"), null, now).first
        val current = filterRestaurants(restaurants, emptySet(), BrowseFilters(query = "2366 Grand Concourse"), null, now).first
        assertTrue(official.any { it.rmpKey.contains("2370 grand concourse road") })
        assertTrue(current.any { it.rmpKey.contains("2370 grand concourse road") })
    }

    @Test
    fun boroughOpenFavoritesAndAttentionFiltersWorkOffline() {
        val now = Instant.parse("2026-09-07T16:00:00Z")
        val favorite = restaurants.first().rmpKey
        val favoriteResults = filterRestaurants(restaurants, setOf(favorite), BrowseFilters(favoritesOnly = true), null, now).first
        assertEquals(listOf(favorite), favoriteResults.map { it.rmpKey })
        val bronx = filterRestaurants(restaurants, emptySet(), BrowseFilters(borough = "Bronx"), null, now).first
        assertEquals(38, bronx.size)
        assertTrue(bronx.all { it.borough == "Bronx" })
        val attention = filterRestaurants(restaurants, emptySet(), BrowseFilters(attentionOnly = true), null, now).first
        assertTrue(attention.isNotEmpty() && attention.all { it.needsAttention })
        val open = filterRestaurants(restaurants, emptySet(), BrowseFilters(openOnly = true), null, now).first
        assertTrue(open.all { OpenNow.isOpen(it, now) })
    }

    @Test
    fun nearbySortUsesDistanceWhenOriginExists() {
        val now = Instant.parse("2026-09-07T16:00:00Z")
        val target = restaurants.first()
        val result = filterRestaurants(restaurants, emptySet(), BrowseFilters(), GeoPoint(target.latitude, target.longitude, "test"), now)
        assertEquals(target.rmpKey, result.first.first().rmpKey)
        assertEquals(0.0, result.second[target.rmpKey] ?: -1.0, 0.0001)
    }

    private fun repoFile(path: String): File {
        val working = File(System.getProperty("user.dir"))
        return sequenceOf(File(working, path), File(working, "../$path"), File(working, "../../$path"))
            .firstOrNull { it.exists() }
            ?: error("Could not locate $path from ${working.absolutePath}")
    }
}
