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
    val cuisineCategories: List<String> get() {
        val name = buildList {
            add(officialName)
            currentName?.let(::add)
            addAll(aliases)
        }.joinToString(" ").lowercase()

        val categories = linkedSetOf<String>()
        fun add(label: String, vararg terms: String) {
            if (terms.any(name::contains)) categories += label
        }

        add("Caribbean", "jamaican", "jerk", "caribbean", "kairibbean", "golden krust", "bunnan", "bun nan", "876 ", "nyaminz", "nago café", "flamez cuisine", "fix-u-plate", "jam it bistro", "tonel to go", "anba tonel", "blaze 718", "caribe restaurant")
        add("Jamaican", "jamaican", "jerk", "golden krust", "fix-u-plate", "blaze 718")
        add("Haitian", "bunnan", "bun nan", "tonel to go", "anba tonel")
        add("Pizza", "pizza", "pizzeria", "papa john", "bites on valentine")
        add("Chicken & Wings", "kentucky fried chicken", "kfc", "popeye", "bojangles", "atomic wings", "chicken", "wing", "8 bit bites", "8-bit bites", "bites on valentine", "more than fries")
        add("Burgers & Fast Food", "burger king", "mcdonald", "wendy", "white castle", "checkers", "sonic drive", "8 bit bites", "8-bit bites", "more than fries")
        add("Seafood", "seafood", "fish", "crab", "off the hook", "ocean eats", "sea and sky feast", "caribe restaurant", "nomas restaurant")
        add("Café & Bakery", "dunkin", "coffee", "cafe", "café", "bakery", "crepe", "french toast")
        add("Deli & Sandwiches", "deli", "sandwich", "subway", "brain food")
        add("Mexican", "mexican", "taco", "tamale", "burrito", "qdoba", "bites on valentine")
        add("Mediterranean & Halal", "gyro", "shawarma", "mediterranean", "halal")
        add("Chinese", "chinese", "dim sum", "dumpling", "wok", "rice noodle", "fok noodles", "cuihua", "dexin yuan", "hua yi jia", "new yee li", "ng's kitchen", "uncle ng", "jojo duck", "lady chow", "mott street", "sun hong wong", "on & on 168", "green garden village", "happy cuisine", "sanmiwago", "hey rich roasted", "kitchen 86", "golden \"z\"", "season restaurant", "carnation restaurant", "tasty & co", "irestaurant")
        add("Cantonese", "hey rich roasted", "kitchen 86", "golden \"z\"", "season restaurant", "carnation restaurant")
        add("Japanese & Sushi", "sushi", "umi premium", "irestaurant")
        add("Thai", "thai")
        add("South Asian", "chaska chai", "dhakaya")
        add("Juice & Smoothies", "juice", "smoothie")
        add("BBQ", "dallas bbq", "bbq")
        add("Latin American", "caridad", "churrasco", "el valle", "nuevo valle", "leche y miel", "lechonera", "los amigos", "sabor latino", "mi sabor", "tu sabor", "mofongo", "loma no.", "el capri", "el nuevo capri", "la barca", "pa' comer", "numero uno", "sabor restaurant", "caribe restaurant", "lebron restaurant", "rosa's steakhouse", "andy's restaurant", "nomas restaurant", "sapoara restaurant", "peter's grill", "k & p lounge")
        add("Dominican", "caridad", "el valle", "nuevo valle", "mofongo", "el capri", "el nuevo capri", "lebron restaurant", "andy's restaurant", "nomas restaurant", "caribe restaurant")
        add("Peruvian", "rosa's steakhouse")
        add("Puerto Rican", "k & p lounge")
        add("Breakfast & Diner", "ihop", "diner", "breakfast", "8 bit bites", "8-bit bites", "bites on valentine", "lebron restaurant", "peter's grill")
        add("Korean Fusion", "memphis seoul")
        add("Soul Food", "a daughter and two sons", "love & soul food", "k & p lounge")
        add("American", "a daughter and two sons", "love & soul food", "8 bit bites", "8-bit bites", "more than fries", "sapoara restaurant", "peter's grill", "k & p lounge")
        add("West African", "sante yallah", "keur aisha")
        add("Azerbaijani & Eastern European", "sheep by baraksha", "sheep by barashka")
        add("Steakhouse", "steakhouse")

        return categories.toList()
    }
    val cuisineLabel: String get() = cuisineCategories.joinToString(" • ")
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
