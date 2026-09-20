package com.mike.rmpfinder.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mike.rmpfinder.R
import com.mike.rmpfinder.data.RmpRestaurant

internal enum class AmenityStatus(val label: String, val marker: String) {
    YES("Yes", "✓"),
    NO("No", "×"),
    UNKNOWN("Unknown", "?"),
}

internal fun amenityStatus(value: Boolean?): AmenityStatus = when (value) {
    true -> AmenityStatus.YES
    false -> AmenityStatus.NO
    null -> AmenityStatus.UNKNOWN
}

private data class AmenityFact(
    val label: String,
    @DrawableRes val iconRes: Int,
    val value: Boolean?,
)

@Composable
internal fun AmenityFactsContent(restaurant: RmpRestaurant) {
    val top = listOf(
        AmenityFact("Dine-in", R.drawable.amenity_dine_in, restaurant.dineIn),
        AmenityFact("Restroom", R.drawable.amenity_restroom, restaurant.restroom),
    )
    val meals = listOf(
        AmenityFact("Breakfast", R.drawable.amenity_breakfast, restaurant.servesBreakfast),
        AmenityFact("Lunch", R.drawable.amenity_lunch, restaurant.servesLunch),
        AmenityFact("Dinner", R.drawable.amenity_dinner, restaurant.servesDinner),
    )
    val access = listOf(
        AmenityFact("Entrance", R.drawable.amenity_accessible_entrance, restaurant.wheelchairAccessibleEntrance),
        AmenityFact("Restroom", R.drawable.amenity_accessible_restroom, restaurant.wheelchairAccessibleRestroom),
        AmenityFact("Seating", R.drawable.amenity_accessible_seating, restaurant.wheelchairAccessibleSeating),
        AmenityFact("Parking", R.drawable.amenity_accessible_parking, restaurant.wheelchairAccessibleParking),
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            top.forEach { fact -> AmenityTile(fact, Modifier.weight(1f)) }
        }

        AmenitySectionTitle("Meals")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            meals.forEach { fact -> AmenityTile(fact, Modifier.weight(1f)) }
        }

        AmenitySectionTitle("Wheelchair access")
        access.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { fact -> AmenityTile(fact, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AmenitySectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun AmenityTile(fact: AmenityFact, modifier: Modifier = Modifier) {
    val status = amenityStatus(fact.value)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Image(
                painter = painterResource(fact.iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = fact.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            AmenityStatusPill(status)
        }
    }
}

@Composable
private fun AmenityStatusPill(status: AmenityStatus) {
    val (background, foreground) = when (status) {
        AmenityStatus.YES -> Color(0xFFE7F4EA) to Color(0xFF257A3E)
        AmenityStatus.NO -> Color(0xFFF0F0EE) to Color(0xFF666762)
        AmenityStatus.UNKNOWN -> Color(0xFFF0F0EE) to Color(0xFF666762)
    }
    Text(
        text = "${status.marker} ${status.label}",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
