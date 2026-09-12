package com.mike.rmpfinder.reviews

import android.content.Context
import com.mike.rmpfinder.data.RmpRestaurant
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

/** The headline numbers for one restaurant, enough to show on a card and to filter by. */
data class Rating(val rating: Double?, val ratingCount: Int?, val priceLevel: Int?, val fetchedAt: Long)

/**
 * Ratings and price levels for every restaurant, so the list can show and
 * filter by them without opening each place.
 *
 * Filled once in the background from Google Places (241 text-search calls,
 * well inside the free monthly tier), persisted as a small JSON file, and
 * refreshed after a week. Restaurants whose lookup fails are retried on the
 * next launch rather than blocking the rest.
 */
class RatingsStore(context: Context, private val fetcher: ReviewsFetcher) {
    private val file = File(context.filesDir, "ratings-cache.json")
    private val state = MutableStateFlow<Map<String, Rating>>(load())
    val ratings: StateFlow<Map<String, Rating>> = state

    suspend fun fill(restaurants: List<RmpRestaurant>) {
        if (!fetcher.isConfigured) return
        val cutoff = System.currentTimeMillis() - TTL_MS
        val missing = restaurants.filter { (state.value[it.rmpKey]?.fetchedAt ?: 0L) < cutoff }
        if (missing.isEmpty()) return
        val gate = Semaphore(4)
        coroutineScope {
            missing.chunked(24).forEach { batch ->
                batch.map { restaurant ->
                    async(Dispatchers.IO) {
                        gate.withPermit {
                            fetcher.headline(restaurant, restaurant.currentAddress ?: restaurant.officialAddress)?.let { restaurant.rmpKey to it }
                        }
                    }
                }.awaitAll().filterNotNull().takeIf { it.isNotEmpty() }?.let { found ->
                    state.value = state.value + found
                    save()
                }
            }
        }
    }

    private fun load(): Map<String, Rating> = runCatching {
        if (!file.exists()) return emptyMap()
        val root = JSONObject(file.readText())
        root.keys().asSequence().associateWith { key ->
            val o = root.getJSONObject(key)
            Rating(
                rating = o.optDouble("rating").takeIf { !it.isNaN() },
                ratingCount = if (o.has("count")) o.getInt("count") else null,
                priceLevel = if (o.has("price")) o.getInt("price") else null,
                fetchedAt = o.optLong("at"),
            )
        }
    }.getOrDefault(emptyMap())

    private suspend fun save() = withContext(Dispatchers.IO) {
        val root = JSONObject()
        state.value.forEach { (key, r) ->
            root.put(key, JSONObject().apply {
                r.rating?.let { put("rating", it) }
                r.ratingCount?.let { put("count", it) }
                r.priceLevel?.let { put("price", it) }
                put("at", r.fetchedAt)
            })
        }
        runCatching { file.writeText(root.toString()) }
    }

    private companion object {
        const val TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
