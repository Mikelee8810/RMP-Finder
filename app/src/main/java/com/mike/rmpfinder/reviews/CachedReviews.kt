package com.mike.rmpfinder.reviews

import com.mike.rmpfinder.data.RmpAddress
import com.mike.rmpfinder.data.RmpRestaurant
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** One public review, trimmed to what fits on a card. */
data class Review(
    val author: String,
    val rating: Int,
    val text: String,
    val when_: String,
)

/** A listing on one review source: the headline numbers plus a few reviews. */
data class ReviewSummary(
    val source: String,
    val rating: Double?,
    val ratingCount: Int?,
    /** 1–4, shown as $ to $$$$; null when the source does not publish it. */
    val priceLevel: Int?,
    val reviews: List<Review>,
    /** Opens the full listing in the source's own app or site. */
    val listingUrl: String,
)

sealed interface ReviewsState {
    data object Idle : ReviewsState
    data object Loading : ReviewsState
    data class Loaded(val summaries: List<ReviewSummary>) : ReviewsState
    data object Unavailable : ReviewsState
}

/** The headline numbers for one restaurant, enough to show on a card and to filter by. */
data class Rating(val rating: Double?, val ratingCount: Int?, val priceLevel: Int?)

/**
 * Ratings and reviews, read straight out of the dataset.
 *
 * These used to be fetched from Google Places on the device: every install
 * asked about every restaurant on first launch and again every week, and
 * opening a restaurant asked again for its reviews. That is the same answer
 * bought once per phone per week instead of once for everybody, on Google's
 * priciest tier, and it grew with every new user.
 *
 * Now tools/backfill_google.py asks once, centrally, and the answers ship
 * inside the dataset. Nothing here touches the network. The reviews on a card
 * are a sample rather than the full listing, so the card still links out to
 * Google Maps for the rest - which costs nothing, because it is Google's own
 * app showing Google's own page.
 */
object CachedReviews {

    /** What the list needs: the numbers it shows and filters on. */
    fun rating(restaurant: RmpRestaurant): Rating? {
        if (restaurant.rating == null && restaurant.ratingCount == null && restaurant.priceLevel == null) return null
        return Rating(restaurant.rating, restaurant.ratingCount, restaurant.priceLevel)
    }

    fun ratings(restaurants: List<RmpRestaurant>): Map<String, Rating> =
        restaurants.mapNotNull { restaurant -> rating(restaurant)?.let { restaurant.rmpKey to it } }.toMap()

    /**
     * What the detail sheet needs. Returns an empty list when we hold nothing
     * worth showing, so the card falls back to its "open in Maps" link rather
     * than rendering an empty shell.
     */
    fun summaries(restaurant: RmpRestaurant, address: RmpAddress): List<ReviewSummary> {
        val hasNumbers = restaurant.rating != null || restaurant.ratingCount != null || restaurant.priceLevel != null
        if (!hasNumbers && restaurant.reviews.isEmpty()) return emptyList()
        return listOf(
            ReviewSummary(
                source = "Google",
                rating = restaurant.rating,
                ratingCount = restaurant.ratingCount,
                priceLevel = restaurant.priceLevel,
                reviews = restaurant.reviews.map { Review(it.author, it.rating, it.text, it.when_) },
                listingUrl = mapsListingUrl(restaurant, address),
            )
        )
    }

    /**
     * The Maps search for this place. A stored Place ID makes it exact; without
     * one, name and address is what Maps itself would search for.
     */
    fun mapsListingUrl(restaurant: RmpRestaurant, address: RmpAddress): String {
        val query = encode("${restaurant.displayName} ${address.display()}")
        val placeId = restaurant.googlePlaceId
        return if (placeId.isNullOrBlank()) {
            "https://www.google.com/maps/search/?api=1&query=$query"
        } else {
            "https://www.google.com/maps/search/?api=1&query=$query&query_place_id=${encode(placeId)}"
        }
    }

    /**
     * Directions to the restaurant's actual address. A stored Place ID keeps
     * Maps pinned to the reviewed listing instead of a nearby coordinate.
     */
    fun mapsDirectionsUrl(restaurant: RmpRestaurant, address: RmpAddress, mode: String): String {
        val destination = encode(address.display())
        val placeId = restaurant.googlePlaceId
        val exactPlace = if (placeId.isNullOrBlank()) "" else "&destination_place_id=${encode(placeId)}"
        return "https://www.google.com/maps/dir/?api=1&destination=$destination$exactPlace&travelmode=${encode(mode)}"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
