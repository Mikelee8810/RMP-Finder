package com.mike.rmpfinder.reviews

import com.mike.rmpfinder.data.RmpAddress
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.RmpReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ratings and reviews are read from the dataset rather than fetched, so the
 * thing worth pinning down is what happens when the dataset is silent: a
 * restaurant we never looked up must not render an empty ratings row or an
 * empty reviews card.
 */
class CachedReviewsTest {

    private val address = RmpAddress("1 Example Street", null, "Brooklyn", "NY", "11201")

    private fun restaurant(
        rating: Double? = null,
        ratingCount: Int? = null,
        priceLevel: Int? = null,
        reviews: List<RmpReview> = emptyList(),
        placeId: String? = null,
    ) = RmpRestaurant(
        rmpKey = "brooklyn|example|1 example street|11201",
        officialName = "Example",
        currentName = null,
        aliases = emptyList(),
        officialAddress = address,
        currentAddress = null,
        borough = "Brooklyn",
        zip = "11201",
        latitude = 40.69,
        longitude = -73.99,
        phone = null,
        website = null,
        menuUrl = null,
        imageUrl = null,
        businessStatus = "likely_open",
        hoursStatus = "usable",
        hours = null,
        googlePlaceId = placeId,
        rating = rating,
        ratingCount = ratingCount,
        priceLevel = priceLevel,
        takeout = null,
        dineIn = null,
        reviews = reviews,
        googleCheckedAt = null,
        rmpVerifiedAt = "2026-09-09",
        businessCheckedAt = null,
        conflictFlags = emptyList(),
        sources = emptyList(),
    )

    @Test
    fun aRestaurantWeNeverLookedUpHasNoRating() {
        assertNull(CachedReviews.rating(restaurant()))
        assertTrue(CachedReviews.ratings(listOf(restaurant())).isEmpty())
    }

    @Test
    fun aPriceLevelAloneIsStillWorthShowing() {
        val only = CachedReviews.rating(restaurant(priceLevel = 2))

        assertEquals(2, only?.priceLevel)
        assertNull(only?.rating)
    }

    @Test
    fun noNumbersAndNoReviewsMeansNoCard() {
        assertTrue(CachedReviews.summaries(restaurant(), address).isEmpty())
    }

    @Test
    fun ratingWithoutReviewsStillShowsTheHeadline() {
        val summaries = CachedReviews.summaries(restaurant(rating = 4.2, ratingCount = 88), address)

        assertEquals(1, summaries.size)
        assertEquals(4.2, summaries.first().rating!!, 0.001)
        assertEquals(88, summaries.first().ratingCount)
        assertTrue(summaries.first().reviews.isEmpty())
    }

    @Test
    fun reviewsAreCarriedThroughInOrder() {
        val stored = listOf(
            RmpReview("Ada", 5, "Great jerk chicken.", "a month ago"),
            RmpReview("Grace", 3, "Slow at lunch.", "2 years ago"),
        )

        val carried = CachedReviews.summaries(restaurant(rating = 4.0, reviews = stored), address).first().reviews

        assertEquals(listOf("Ada", "Grace"), carried.map { it.author })
        assertEquals(listOf(5, 3), carried.map { it.rating })
        assertEquals("2 years ago", carried[1].when_)
    }

    @Test
    fun aStoredPlaceIdMakesTheMapsLinkExact() {
        val url = CachedReviews.mapsListingUrl(restaurant(placeId = "ChIJ_test_123"), address)

        assertTrue(url, url.contains("query_place_id=ChIJ_test_123"))
    }

    @Test
    fun withoutAPlaceIdTheMapsLinkFallsBackToASearch() {
        val url = CachedReviews.mapsListingUrl(restaurant(), address)

        assertTrue(url, url.startsWith("https://www.google.com/maps/search/?api=1&query="))
        assertTrue(url, !url.contains("query_place_id"))
    }

    @Test
    fun directionsUseAddressAndStoredPlaceId() {
        val url = CachedReviews.mapsDirectionsUrl(restaurant(placeId = "ChIJ_test_123"), address, "transit")

        assertTrue(url, url.contains("destination=1+Example+Street%2C+Brooklyn%2C+NY+11201"))
        assertTrue(url, url.contains("destination_place_id=ChIJ_test_123"))
        assertTrue(url, url.contains("travelmode=transit"))
    }

    @Test
    fun directionsWithoutPlaceIdStillUseAddress() {
        val url = CachedReviews.mapsDirectionsUrl(restaurant(), address, "walking")

        assertTrue(url, url.contains("destination=1+Example+Street%2C+Brooklyn%2C+NY+11201"))
        assertTrue(url, !url.contains("destination_place_id"))
    }
}
