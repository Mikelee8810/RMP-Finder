package com.mike.rmpfinder.data

import java.net.URI
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DatasetCodec {
    private val json = Json { ignoreUnknownKeys = false }
    private val zipPattern = Regex("^[0-9]{5}(-[0-9]{4})?$")
    private val timePattern = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
    private val closePattern = Regex("^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$")
    private val businessStatuses = setOf("open", "likely_open", "temporarily_closed", "likely_closed", "closed", "moved", "rebranded", "conflicting", "unknown")
    private val hoursStatuses = setOf("verified", "usable", "stale", "partial", "conflicting", "unknown")
    private val conflictFlags = setOf("name_mismatch", "address_mismatch", "moved", "rebranded", "hours_conflict", "status_conflict", "phone_conflict")
    private val restaurantKeys = setOf("rmpKey", "officialName", "currentName", "aliases", "officialAddress", "currentAddress", "borough", "zip", "coordinates", "phone", "website", "menuUrl", "imageUrl", "imageAttribution", "businessStatus", "hoursStatus", "hours", "rmpVerifiedAt", "businessCheckedAt", "conflictFlags", "sources")
    private val requiredRestaurantKeys = setOf("rmpKey", "officialName", "officialAddress", "borough", "zip", "coordinates", "rmpVerifiedAt", "businessStatus", "hoursStatus")

    fun parseDataset(bytes: ByteArray): List<RmpRestaurant> {
        val root = json.parseToJsonElement(bytes.decodeToString())
        require(root is JsonArray) { "Dataset root must be an array" }
        return root.map { parseRestaurant(it.jsonObject) }
    }

    fun parseRestaurant(rawJson: String): RmpRestaurant = parseRestaurant(json.parseToJsonElement(rawJson).jsonObject)

    fun toEntity(restaurant: RmpRestaurant, rawJson: String): RestaurantEntity {
        val search = buildList {
            add(restaurant.officialName)
            restaurant.currentName?.let(::add)
            addAll(restaurant.aliases)
            add(restaurant.officialAddress.display())
            restaurant.currentAddress?.display()?.let(::add)
            add(restaurant.borough)
            add(restaurant.zip)
        }.joinToString(" ").lowercase()
        return RestaurantEntity(
            rmpKey = restaurant.rmpKey,
            officialName = restaurant.officialName,
            currentName = restaurant.currentName,
            borough = restaurant.borough,
            zip = restaurant.zip,
            officialAddress = restaurant.officialAddress.display(),
            currentAddress = restaurant.currentAddress?.display(),
            latitude = restaurant.latitude,
            longitude = restaurant.longitude,
            phone = restaurant.phone,
            website = restaurant.website,
            menuUrl = restaurant.menuUrl,
            imageUrl = restaurant.imageUrl,
            businessStatus = restaurant.businessStatus,
            hoursStatus = restaurant.hoursStatus,
            rmpVerifiedAt = restaurant.rmpVerifiedAt,
            businessCheckedAt = restaurant.businessCheckedAt,
            hasConflict = restaurant.conflictFlags.isNotEmpty(),
            searchText = search,
            rawJson = rawJson,
        )
    }

    fun entitiesFromDataset(bytes: ByteArray): Pair<List<RmpRestaurant>, List<RestaurantEntity>> {
        val root = json.parseToJsonElement(bytes.decodeToString())
        require(root is JsonArray) { "Dataset root must be an array" }
        val restaurants = root.map { parseRestaurant(it.jsonObject) }
        val entities = root.zip(restaurants).map { (element, restaurant) -> toEntity(restaurant, element.toString()) }
        require(restaurants.map { it.rmpKey }.toSet().size == restaurants.size) { "Dataset contains duplicate RMP keys" }
        return restaurants to entities
    }

    fun parseManifest(bytes: ByteArray): DatasetManifest {
        val obj = json.parseToJsonElement(bytes.decodeToString()).jsonObject
        requireAllowed(obj, setOf("datasetVersion", "schemaVersion", "generatedAt", "recordCount", "sha256", "datasetUrl", "minimumAppVersion", "sourceRevision", "programPolicy"), setOf("datasetVersion", "schemaVersion", "generatedAt", "recordCount", "sha256", "datasetUrl", "minimumAppVersion", "programPolicy"))
        val policy = obj.objectValue("programPolicy")
        requireAllowed(policy, setOf("rmpDiscountPercent", "checkedAt", "sourceUrl"), setOf("rmpDiscountPercent", "checkedAt", "sourceUrl"))
        val generatedAt = obj.string("generatedAt")
        OffsetDateTime.parse(generatedAt)
        val sha = obj.string("sha256")
        require(sha.matches(Regex("^[0-9a-f]{64}$"))) { "Manifest SHA-256 is invalid" }
        val datasetUrl = obj.string("datasetUrl")
        requireUri(datasetUrl)
        val policyUrl = policy.string("sourceUrl")
        requireUri(policyUrl)
        val checkedAt = policy.string("checkedAt")
        LocalDate.parse(checkedAt)
        val datasetVersion = obj.int("datasetVersion").also { require(it >= 1) }
        val schemaVersion = obj.int("schemaVersion").also { require(it >= 1) }
        val recordCount = obj.int("recordCount").also { require(it >= 1) }
        val minimumAppVersion = obj.int("minimumAppVersion").also { require(it >= 1) }
        val discountPercent = policy.int("rmpDiscountPercent").also { require(it == 10) }
        return DatasetManifest(
            datasetVersion = datasetVersion,
            schemaVersion = schemaVersion,
            generatedAt = generatedAt,
            recordCount = recordCount,
            sha256 = sha,
            datasetUrl = datasetUrl,
            minimumAppVersion = minimumAppVersion,
            discountPercent = discountPercent,
            policyCheckedAt = checkedAt,
            policySourceUrl = policyUrl,
        )
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun parseRestaurant(obj: JsonObject): RmpRestaurant {
        requireAllowed(obj, restaurantKeys, requiredRestaurantKeys)
        val rmpKey = obj.string("rmpKey").also { require(it.isNotBlank()) }
        val officialName = obj.string("officialName").also { require(it.isNotBlank()) }
        val currentName = obj.nullableString("currentName")
        val aliases = obj.arrayOrEmpty("aliases").map { it.jsonPrimitive.content }
        val officialAddress = parseAddress(obj.objectValue("officialAddress"))
        val currentAddress = obj["currentAddress"].let { value -> if (value == null || value is JsonNull) null else parseAddress(value.jsonObject) }
        val borough = obj.string("borough").also { require(it.isNotBlank()) }
        val zip = obj.string("zip").also { require(zipPattern.matches(it)) }
        val coordinates = obj.objectValue("coordinates")
        requireAllowed(coordinates, setOf("latitude", "longitude", "addressRole", "provider", "matchQuality", "precision", "checkedAt", "matchedAddress"), setOf("latitude", "longitude", "addressRole", "provider", "matchQuality", "precision", "checkedAt"))
        val latitude = coordinates.double("latitude").also { require(it in 40.4..41.5) }
        val longitude = coordinates.double("longitude").also { require(it in -74.5..-73.4) }
        require(coordinates.string("addressRole") in setOf("official_rmp", "current_business"))
        require(coordinates.string("provider").isNotBlank())
        require(coordinates.string("matchQuality") in setOf("exact", "matched", "approximate", "manual", "unmatched"))
        require(coordinates.string("precision") in setOf("rooftop", "parcel", "street", "interpolated", "approximate", "unknown"))
        LocalDate.parse(coordinates.string("checkedAt"))
        val phone = obj.nullableString("phone")
        val website = obj.nullableString("website")?.also(::requireUri)
        val menuUrl = obj.nullableString("menuUrl")?.also(::requireUri)
        val imageUrl = obj.nullableString("imageUrl")?.also(::requireUri)
        validateImageAttribution(obj["imageAttribution"])
        val businessStatus = obj.string("businessStatus").also { require(it in businessStatuses) }
        val hoursStatus = obj.string("hoursStatus").also { require(it in hoursStatuses) }
        val hours = obj["hours"].let { value -> if (value == null || value is JsonNull) null else parseHours(value.jsonObject) }
        val rmpVerifiedAt = obj.string("rmpVerifiedAt").also { LocalDate.parse(it) }
        val businessCheckedAt = obj.nullableString("businessCheckedAt")?.also { LocalDate.parse(it) }
        val flags = obj.arrayOrEmpty("conflictFlags").map { it.jsonPrimitive.content }.also { values -> require(values.all { it in conflictFlags } && values.distinct().size == values.size) }
        validateSources(obj.arrayOrEmpty("sources"))
        return RmpRestaurant(
            rmpKey, officialName, currentName, aliases, officialAddress, currentAddress, borough, zip,
            latitude, longitude, phone, website, menuUrl, imageUrl, businessStatus, hoursStatus, hours,
            rmpVerifiedAt, businessCheckedAt, flags,
        )
    }

    private fun parseAddress(obj: JsonObject): RmpAddress {
        requireAllowed(obj, setOf("line1", "line2", "city", "state", "zip"), setOf("line1", "city", "state", "zip"))
        val state = obj.string("state").also { require(it == "NY") }
        val zip = obj.string("zip").also { require(zipPattern.matches(it)) }
        return RmpAddress(obj.string("line1"), obj.nullableString("line2"), obj.string("city"), state, zip)
    }

    private fun parseHours(obj: JsonObject): WeeklyHours {
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        requireAllowed(obj, (days + "timezone").toSet(), (days + "timezone").toSet())
        require(obj.string("timezone") == "America/New_York")
        fun periods(day: String): List<TimePeriod> = obj.array(day).map { element ->
            val period = element.jsonObject
            requireAllowed(period, setOf("open", "close"), setOf("open", "close"))
            val open = period.string("open").also { require(timePattern.matches(it)) }
            val close = period.string("close").also { require(closePattern.matches(it)) }
            TimePeriod(open, close)
        }
        return WeeklyHours("America/New_York", periods("monday"), periods("tuesday"), periods("wednesday"), periods("thursday"), periods("friday"), periods("saturday"), periods("sunday"))
    }

    private fun validateImageAttribution(value: JsonElement?) {
        if (value == null || value is JsonNull) return
        val obj = value.jsonObject
        requireAllowed(obj, setOf("sourceUrl", "credit", "license"), setOf("sourceUrl", "credit"))
        requireUri(obj.string("sourceUrl"))
        require(obj.string("credit").isNotBlank())
        obj.nullableString("license")
    }

    private fun validateSources(values: JsonArray) {
        val roles = setOf("rmp_eligibility", "business_status", "hours", "phone", "website", "address", "coordinates", "image")
        values.forEach { element ->
            val obj = element.jsonObject
            requireAllowed(obj, setOf("kind", "role", "url", "checkedAt"), setOf("kind", "role", "checkedAt"))
            require(obj.string("kind").isNotBlank())
            require(obj.string("role") in roles)
            obj.nullableString("url")?.also(::requireUri)
            LocalDate.parse(obj.string("checkedAt"))
        }
    }

    private fun requireAllowed(obj: JsonObject, allowed: Set<String>, required: Set<String>) {
        require(obj.keys.all { it in allowed }) { "Unexpected JSON field" }
        require(required.all { it in obj }) { "Missing required JSON field" }
    }

    private fun requireUri(value: String) {
        require(URI(value).isAbsolute) { "URL must be absolute" }
    }

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: error("Missing string: $key")
    private fun JsonObject.nullableString(key: String): String? = this[key].let { if (it == null || it is JsonNull) null else it.jsonPrimitive.contentOrNull }
    private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: error("Missing integer: $key")
    private fun JsonObject.double(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: error("Missing number: $key")
    private fun JsonObject.objectValue(key: String): JsonObject = this[key]?.jsonObject ?: error("Missing object: $key")
    private fun JsonObject.array(key: String): JsonArray = this[key]?.jsonArray ?: error("Missing array: $key")
    private fun JsonObject.arrayOrEmpty(key: String): JsonArray = this[key]?.jsonArray ?: JsonArray(emptyList())
}
