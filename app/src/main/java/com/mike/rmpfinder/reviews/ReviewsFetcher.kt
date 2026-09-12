package com.mike.rmpfinder.reviews

import com.mike.rmpfinder.BuildConfig
import com.mike.rmpfinder.data.RmpAddress
import com.mike.rmpfinder.data.RmpRestaurant
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** One public review, trimmed to what fits on a card. */
data class Review(
    val author: String,
    val rating: Int,
    val text: String,
    val when_: String,
)

/** A listing on one review source: the headline numbers plus the newest reviews. */
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

/**
 * Pulls ratings, price level and the most recent reviews for one restaurant.
 *
 * Google Places (New) returns rating, price level and up to five reviews in a
 * single details call; Yelp Fusion returns rating, price and three excerpts.
 * Both need an API key supplied at build time; without one, that source is
 * simply skipped and the UI falls back to a link. Results are cached for the
 * life of the process because a listing does not change between taps.
 */
class ReviewsFetcher(
    private val googleKey: String = BuildConfig.GOOGLE_PLACES_KEY,
    private val yelpKey: String = BuildConfig.YELP_API_KEY,
) {
    private val cache = ConcurrentHashMap<String, List<ReviewSummary>>()

    val isConfigured: Boolean get() = googleKey.isNotBlank() || yelpKey.isNotBlank()

    suspend fun fetch(restaurant: RmpRestaurant, address: RmpAddress): List<ReviewSummary> = withContext(Dispatchers.IO) {
        cache[restaurant.rmpKey]?.let { return@withContext it }
        val results = buildList {
            if (googleKey.isNotBlank()) runCatching { google(restaurant, address) }.getOrNull()?.let(::add)
            if (yelpKey.isNotBlank()) runCatching { yelp(restaurant, address) }.getOrNull()?.let(::add)
        }
        if (results.isNotEmpty()) cache[restaurant.rmpKey] = results
        results
    }

    private fun google(restaurant: RmpRestaurant, address: RmpAddress): ReviewSummary? {
        val query = "${restaurant.displayName}, ${address.display()}"
        val body = JSONObject().put("textQuery", query).put("maxResultCount", 1)
            .put("locationBias", JSONObject().put("circle", JSONObject()
                .put("center", JSONObject().put("latitude", restaurant.latitude).put("longitude", restaurant.longitude))
                .put("radius", 800.0)))
        val fields = "places.id,places.rating,places.userRatingCount,places.priceLevel,places.reviews,places.googleMapsUri"
        val response = post(
            "https://places.googleapis.com/v1/places:searchText",
            body.toString(),
            mapOf("X-Goog-Api-Key" to googleKey, "X-Goog-FieldMask" to fields, "Content-Type" to "application/json"),
        )
        val place = JSONObject(response).optJSONArray("places")?.optJSONObject(0) ?: return null
        val reviews = place.optJSONArray("reviews").toList().map { r ->
            Review(
                author = r.optJSONObject("authorAttribution")?.optString("displayName").orEmpty().ifBlank { "Google user" },
                rating = r.optInt("rating"),
                text = r.optJSONObject("text")?.optString("text").orEmpty(),
                when_ = r.optString("relativePublishTimeDescription"),
            )
        }.filter { it.text.isNotBlank() }
        return ReviewSummary(
            source = "Google",
            rating = place.optDouble("rating").takeIf { !it.isNaN() },
            ratingCount = place.optInt("userRatingCount").takeIf { place.has("userRatingCount") },
            priceLevel = when (place.optString("priceLevel")) {
                "PRICE_LEVEL_INEXPENSIVE" -> 1
                "PRICE_LEVEL_MODERATE" -> 2
                "PRICE_LEVEL_EXPENSIVE" -> 3
                "PRICE_LEVEL_VERY_EXPENSIVE" -> 4
                else -> null
            },
            reviews = reviews,
            listingUrl = place.optString("googleMapsUri").ifBlank {
                "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, "UTF-8")
            },
        )
    }

    private fun yelp(restaurant: RmpRestaurant, address: RmpAddress): ReviewSummary? {
        val headers = mapOf("Authorization" to "Bearer $yelpKey", "Accept" to "application/json")
        val match = get(
            "https://api.yelp.com/v3/businesses/search?limit=1&term=" + URLEncoder.encode(restaurant.displayName, "UTF-8") +
                "&latitude=${restaurant.latitude}&longitude=${restaurant.longitude}&radius=800",
            headers,
        )
        val business = JSONObject(match).optJSONArray("businesses")?.optJSONObject(0) ?: return null
        val id = business.getString("id")
        val reviews = runCatching {
            JSONObject(get("https://api.yelp.com/v3/businesses/$id/reviews?limit=3&sort_by=newest", headers))
                .optJSONArray("reviews").toList().map { r ->
                    Review(
                        author = r.optJSONObject("user")?.optString("name").orEmpty().ifBlank { "Yelp user" },
                        rating = r.optInt("rating"),
                        text = r.optString("text"),
                        when_ = r.optString("time_created").take(10),
                    )
                }
        }.getOrDefault(emptyList())
        return ReviewSummary(
            source = "Yelp",
            rating = business.optDouble("rating").takeIf { !it.isNaN() },
            ratingCount = business.optInt("review_count").takeIf { business.has("review_count") },
            priceLevel = business.optString("price").length.takeIf { it in 1..4 },
            reviews = reviews,
            listingUrl = business.optString("url"),
        )
    }

    private fun get(url: String, headers: Map<String, String>): String = request("GET", url, null, headers)
    private fun post(url: String, body: String, headers: Map<String, String>): String = request("POST", url, body, headers)

    private fun request(method: String, url: String, body: String?, headers: Map<String, String>): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "RMP-Finder/${BuildConfig.VERSION_NAME}")
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray()) }
        }
        try {
            val code = connection.responseCode
            require(code in 200..299) { "HTTP $code" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONArray?.toList(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
