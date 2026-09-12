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
    fun everyRecordCarriesTheProvenanceTheDetailScreenShows() {
        // The codec used to validate sources and then drop them, so the detail
        // screen had nothing to show. Every record must keep its provenance.
        assertTrue(restaurants.all { it.sources.isNotEmpty() })
        assertTrue(restaurants.all { restaurant -> restaurant.sources.any { it.role == "rmp_eligibility" } })
        assertTrue(restaurants.all { restaurant -> restaurant.sources.any { it.role == "coordinates" } })

        val otda = restaurants.first().sources.first { it.role == "rmp_eligibility" }
        assertEquals("NYS OTDA Restaurant Meals Program", otda.kind)
        assertTrue(otda.url?.startsWith("https://otda.ny.gov/") == true)

        // A role the schema allows must never appear under a different spelling,
        // or the detail screen would fall back to showing the raw enum value.
        val known = setOf("rmp_eligibility", "business_status", "hours", "phone", "website", "address", "coordinates", "image")
        assertTrue(restaurants.flatMap { it.sources }.all { it.role in known })
        assertTrue(restaurants.flatMap { it.sources }.none { it.kind.isBlank() })
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
    fun kentuckyFriedChickenIsSearchableByKfcAndKentucky() {
        val now = Instant.parse("2026-09-07T16:00:00Z")
        val kfc = filterRestaurants(restaurants, emptySet(), BrowseFilters(query = "KFC"), null, now).first
        val kentucky = filterRestaurants(restaurants, emptySet(), BrowseFilters(query = "Kentucky"), null, now).first

        assertTrue(kfc.isNotEmpty())
        assertEquals(kentucky.map { it.rmpKey }.toSet(), kfc.map { it.rmpKey }.toSet())
        assertTrue(kfc.all { it.officialName.contains("Kentucky Fried Chicken", ignoreCase = true) })
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
    fun cuisineCategoryIsPresentAndFilterableForEveryRestaurant() {
        val now = Instant.parse("2026-09-07T16:00:00Z")
        assertTrue(restaurants.all { it.cuisineCategories.isNotEmpty() && it.cuisineCategories.none(String::isBlank) })
        assertTrue(restaurants.none { "Other" in it.cuisineCategories })

        val chicken = filterRestaurants(restaurants, emptySet(), BrowseFilters(category = "Chicken & Wings"), null, now).first
        assertTrue(chicken.isNotEmpty())
        assertTrue(chicken.all { "Chicken & Wings" in it.cuisineCategories })
        assertTrue(chicken.any { it.officialName.contains("Kentucky Fried Chicken", ignoreCase = true) })

        val combo = restaurants.first { it.officialName.contains("Burger King/Popeyes", ignoreCase = true) }
        assertTrue("Burgers & Fast Food" in combo.cuisineCategories)
        assertTrue("Chicken & Wings" in combo.cuisineCategories)
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
