package com.mike.rmpfinder.ui

import com.mike.rmpfinder.data.RmpRestaurant
import java.net.URI
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Resolves a restaurant to bundled logo asset paths without depending on Android UI state. */
internal object RestaurantLogoResolver {
    private const val STANDARD_LOGO_DIRECTORY = "restaurant-logos"
    private const val CUSTOM_LOGO_DIRECTORY = "custom-restaurant-logos"

    fun parseMapping(json: String): Map<String, String> =
        Json.parseToJsonElement(json).jsonObject.mapNotNull { (rmpKey, value) ->
            val primitive = runCatching { value.jsonPrimitive }.getOrNull()
            val filename = primitive
                ?.takeIf { it.isString }
                ?.contentOrNull
                ?.trim()
                ?.takeIf(::isSafeFilename)
            if (rmpKey.isBlank() || filename == null) null else rmpKey to filename
        }.toMap()

    fun assetPathsFor(restaurant: RmpRestaurant, mapping: Map<String, String>): List<String> =
        assetPathsFor(restaurant.rmpKey, restaurant.website, mapping)

    fun assetPathsFor(rmpKey: String, website: String?, mapping: Map<String, String>): List<String> {
        val paths = mutableListOf<String>()

        mapping[rmpKey]?.takeIf(::isSafeFilename)?.let { filename ->
            paths += "$STANDARD_LOGO_DIRECTORY/$filename"
            paths += "$CUSTOM_LOGO_DIRECTORY/$filename"
        }

        websiteDomain(website)?.let { domain ->
            paths += "$STANDARD_LOGO_DIRECTORY/$domain.png"
        }

        return paths.distinct()
    }

    private fun websiteDomain(website: String?): String? {
        val raw = website?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalized = if ("://" in raw) raw else "https://$raw"
        return runCatching { URI(normalized).host }
            .getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.replace(Regex("[^a-z0-9.-]+"), "-")
            ?.trim('-', '.')
            ?.takeIf { it.isNotBlank() }
    }

    private fun isSafeFilename(filename: String): Boolean =
        filename.isNotBlank() &&
            '/' !in filename &&
            '\\' !in filename &&
            filename != "." &&
            filename != ".." &&
            !filename.contains("../")
}
