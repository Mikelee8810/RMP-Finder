package com.mike.rmpfinder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RestaurantLogoResolverTest {
    @Test
    fun rmpKeyMappingIsPrimaryAndWorksWithoutWebsite() {
        val mapping = RestaurantLogoResolver.parseMapping(
            """{"bronx|test cafe|1 main street|10451":"test-cafe.png"}""",
        )

        assertEquals(
            listOf(
                "restaurant-logos/test-cafe.png",
                "custom-restaurant-logos/test-cafe.png",
            ),
            RestaurantLogoResolver.assetPathsFor(
                rmpKey = "bronx|test cafe|1 main street|10451",
                website = null,
                mapping = mapping,
            ),
        )
    }

    @Test
    fun mappedLogoWinsAndDomainRemainsFallback() {
        val mapping = mapOf("queens|test|2 main street|11373" to "reviewed-mark.png")

        assertEquals(
            listOf(
                "restaurant-logos/reviewed-mark.png",
                "custom-restaurant-logos/reviewed-mark.png",
                "restaurant-logos/mcdonalds.com.png",
            ),
            RestaurantLogoResolver.assetPathsFor(
                rmpKey = "queens|test|2 main street|11373",
                website = "https://www.mcdonalds.com/us/en-us.html",
                mapping = mapping,
            ),
        )
    }

    @Test
    fun unmappedRestaurantFallsBackToWebsiteDomain() {
        assertEquals(
            listOf("restaurant-logos/kfc.com.png"),
            RestaurantLogoResolver.assetPathsFor(
                rmpKey = "brooklyn|test|3 main street|11201",
                website = "https://www.kfc.com/menu",
                mapping = emptyMap(),
            ),
        )
    }
}
