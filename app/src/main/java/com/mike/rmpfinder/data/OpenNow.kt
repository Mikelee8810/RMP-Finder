package com.mike.rmpfinder.data

import java.time.Instant
import java.time.ZoneId

object OpenNow {
    private val allowedBusinessStatuses = setOf("open", "likely_open", "rebranded")
    private val allowedHoursStatuses = setOf("verified", "usable")

    fun isOpen(restaurant: RmpRestaurant, instant: Instant = Instant.now()): Boolean {
        if (restaurant.businessStatus !in allowedBusinessStatuses) return false
        if (restaurant.hoursStatus !in allowedHoursStatuses) return false
        val hours = restaurant.hours ?: return false
        val now = instant.atZone(ZoneId.of(hours.timezone))
        val minute = now.hour * 60 + now.minute
        return hours.periods(now.dayOfWeek).any { period ->
            val start = minutes(period.open)
            val end = if (period.close == "24:00") 24 * 60 else minutes(period.close)
            minute in start until end
        }
    }

    fun label(restaurant: RmpRestaurant, instant: Instant = Instant.now()): String = when {
        restaurant.businessStatus in setOf("closed", "likely_closed") -> "Closed"
        restaurant.businessStatus == "temporarily_closed" -> "Temporarily closed"
        restaurant.businessStatus == "moved" -> "Moved — check details"
        restaurant.businessStatus == "conflicting" -> "Status conflict — check details"
        restaurant.hoursStatus !in allowedHoursStatuses || restaurant.hours == null -> "Hours need review"
        isOpen(restaurant, instant) -> "Open now"
        else -> "Closed now"
    }

    private fun minutes(value: String): Int {
        val parts = value.split(':')
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}

fun distanceMiles(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
    val earthRadiusMiles = 3958.7613
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(toLat)
    val dLat = Math.toRadians(toLat - fromLat)
    val dLon = Math.toRadians(toLon - fromLon)
    val a = kotlin.math.sin(dLat / 2).let { it * it } + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * kotlin.math.sin(dLon / 2).let { it * it }
    return earthRadiusMiles * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}
