package com.mike.rmpfinder.data

data class RmpAddress(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String,
    val zip: String,
) {
    fun display(): String = listOfNotNull(line1, line2, "$city, $state $zip").joinToString(", ")
}

data class TimePeriod(
    val open: String,
    val close: String,
)

data class WeeklyHours(
    val timezone: String,
    val monday: List<TimePeriod>,
    val tuesday: List<TimePeriod>,
    val wednesday: List<TimePeriod>,
    val thursday: List<TimePeriod>,
    val friday: List<TimePeriod>,
    val saturday: List<TimePeriod>,
    val sunday: List<TimePeriod>,
) {
    fun periods(day: java.time.DayOfWeek): List<TimePeriod> = when (day) {
        java.time.DayOfWeek.MONDAY -> monday
        java.time.DayOfWeek.TUESDAY -> tuesday
        java.time.DayOfWeek.WEDNESDAY -> wednesday
        java.time.DayOfWeek.THURSDAY -> thursday
        java.time.DayOfWeek.FRIDAY -> friday
        java.time.DayOfWeek.SATURDAY -> saturday
        java.time.DayOfWeek.SUNDAY -> sunday
    }
}

/** Where one fact on a record came from, and when it was last checked. */
data class RmpSource(
    val kind: String,
    val role: String,
    val url: String?,
    val checkedAt: String,
)

data class RmpRestaurant(
    val rmpKey: String,
    val officialName: String,
    val currentName: String?,
    val aliases: List<String>,
    val officialAddress: RmpAddress,
    val currentAddress: RmpAddress?,
    val borough: String,
    val zip: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val website: String?,
    val menuUrl: String?,
    val imageUrl: String?,
    val businessStatus: String,
    val hoursStatus: String,
    val hours: WeeklyHours?,
    val rmpVerifiedAt: String,
    val businessCheckedAt: String?,
    val conflictFlags: List<String>,
    val sources: List<RmpSource>,
) {
    val displayName: String get() = currentName ?: officialName
    val needsAttention: Boolean get() = conflictFlags.isNotEmpty() || businessStatus in ATTENTION_STATUSES || hoursStatus in ATTENTION_HOURS

    companion object {
        private val ATTENTION_STATUSES = setOf("temporarily_closed", "likely_closed", "closed", "moved", "rebranded", "conflicting", "unknown")
        private val ATTENTION_HOURS = setOf("stale", "partial", "conflicting", "unknown")
    }
}

data class DatasetManifest(
    val datasetVersion: Int,
    val schemaVersion: Int,
    val generatedAt: String,
    val recordCount: Int,
    val sha256: String,
    val datasetUrl: String,
    val minimumAppVersion: Int,
    val discountPercent: Int,
    val policyCheckedAt: String,
    val policySourceUrl: String,
)

sealed interface UpdateResult {
    data class Updated(val version: Int, val count: Int) : UpdateResult
    data class Current(val version: Int) : UpdateResult
    data class NeedsReview(val missingKeys: List<String>) : UpdateResult
    data class Failed(val message: String) : UpdateResult
}
