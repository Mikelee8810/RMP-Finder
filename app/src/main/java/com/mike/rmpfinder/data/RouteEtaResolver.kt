package com.mike.rmpfinder.data

import com.mike.rmpfinder.TransitMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

data class RouteEta(
    val durationMinutes: Int,
    val distanceMiles: Double,
    val isLiveStreetRoute: Boolean,
) {
    fun formatted(mode: TransitMode): String {
        val suffix = if (isLiveStreetRoute) " street" else ""
        return "${mode.iconPrefix} $durationMinutes min$suffix"
    }
}

object RouteEtaResolver {
    private const val OSRM_BASE = "https://routing.openstreetmap.de"
    private const val USER_AGENT = "RMPFinder/1.3.4 (Android)"
    private const val TIMEOUT_MS = 4000

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = ConcurrentHashMap<String, RouteEta>()

    suspend fun resolveEta(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        mode: TransitMode,
    ): RouteEta = withContext(Dispatchers.IO) {
        val cacheKey = "%.4f,%.4f->%.4f,%.4f:%s".format(originLat, originLon, destLat, destLon, mode.name)
        cache[cacheKey]?.let { return@withContext it }

        val live = queryOsrm(originLat, originLon, destLat, destLon, mode)
        val result = live ?: fallbackEta(originLat, originLon, destLat, destLon, mode)
        cache[cacheKey] = result
        result
    }

    internal fun queryOsrm(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        mode: TransitMode,
    ): RouteEta? {
        val (endpoint, isScooter) = when (mode) {
            TransitMode.WALKING -> "routed-foot/route/v1/walking" to false
            TransitMode.BIKING -> "routed-bike/route/v1/bicycling" to false
            TransitMode.SCOOTER -> "routed-bike/route/v1/bicycling" to true
            TransitMode.DRIVING -> "routed-car/route/v1/driving" to false
            TransitMode.TRANSIT -> return null // OSRM has no public transit dispatch
        }

        return try {
            val urlString = "$OSRM_BASE/$endpoint/$originLon,$originLat;$destLon,$destLat?overview=false"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseOsrmResponse(body, isScooter)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseOsrmResponse(jsonString: String, isScooter: Boolean = false): RouteEta? {
        return try {
            val root = jsonParser.parseToJsonElement(jsonString).jsonObject
            val code = root["code"]?.jsonPrimitive?.content
            if (code != "Ok") return null

            val routes = root["routes"]?.jsonArray ?: return null
            if (routes.isEmpty()) return null

            val route = routes[0].jsonObject
            val durationSec = route["duration"]?.jsonPrimitive?.doubleOrNull ?: return null
            val distanceM = route["distance"]?.jsonPrimitive?.doubleOrNull ?: return null
            if (durationSec < 0 || distanceM < 0) return null

            val streetMiles = distanceM * 0.000621371
            val effectiveDurationSec = if (isScooter) durationSec * 0.8 else durationSec
            val minutes = (effectiveDurationSec / 60.0).roundToInt().coerceAtLeast(1)

            RouteEta(
                durationMinutes = minutes,
                distanceMiles = streetMiles,
                isLiveStreetRoute = true,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fallbackEta(
        originLat: Double,
        originLon: Double,
        destLat: Double,
        destLon: Double,
        mode: TransitMode,
    ): RouteEta {
        val straightMiles = distanceMiles(originLat, originLon, destLat, destLon)
        val streetFactor = if (mode == TransitMode.DRIVING) 1.35 else 1.30
        val estimatedStreetMiles = straightMiles * streetFactor
        val mins = when (mode) {
            TransitMode.WALKING -> (estimatedStreetMiles * 22.0).roundToInt().coerceAtLeast(1)
            TransitMode.BIKING -> (estimatedStreetMiles * 6.0).roundToInt().coerceAtLeast(1)
            TransitMode.SCOOTER -> (estimatedStreetMiles * 5.0).roundToInt().coerceAtLeast(1)
            TransitMode.TRANSIT -> (7.0 + straightMiles * 6.0).roundToInt().coerceAtLeast(5)
            TransitMode.DRIVING -> (4.0 + estimatedStreetMiles * 5.0).roundToInt().coerceAtLeast(3)
        }
        return RouteEta(
            durationMinutes = mins,
            distanceMiles = estimatedStreetMiles,
            isLiveStreetRoute = false,
        )
    }

    fun clearCache() {
        cache.clear()
    }
}
