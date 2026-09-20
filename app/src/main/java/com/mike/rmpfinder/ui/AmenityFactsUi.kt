package com.mike.rmpfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val icon: ImageVector,
    val value: Boolean?,
)

@Composable
internal fun AmenityFactsContent(restaurant: RmpRestaurant) {
    val top = listOf(
        AmenityFact("Dine-in", Icons.Filled.Restaurant, restaurant.dineIn),
        AmenityFact("Restroom", Icons.Filled.Wc, restaurant.restroom),
        AmenityFact("Parking", Icons.Filled.LocalParking, restaurant.hasParking),
    )
    val meals = listOf(
        AmenityFact("Breakfast", Icons.Filled.BreakfastDining, restaurant.servesBreakfast),
        AmenityFact("Lunch", Icons.Filled.LunchDining, restaurant.servesLunch),
        AmenityFact("Dinner", Icons.Filled.DinnerDining, restaurant.servesDinner),
    )
    val access = listOf(
        AmenityFact("Accessible entrance", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleEntrance),
        AmenityFact("Accessible restroom", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleRestroom),
        AmenityFact("Accessible seating", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleSeating),
        AmenityFact("Accessible parking", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleParking),
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            top.forEach { fact -> AmenityTile(fact, Modifier.weight(1f)) }
        }

        AmenitySectionTitle("Meals")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            meals.forEach { fact -> AmenityTile(fact, Modifier.weight(1f)) }
        }

        AmenitySectionTitle("Wheelchair access")
        access.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val known = status != AmenityStatus.UNKNOWN
    val iconTint = if (known) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
    }
    val labelColor = if (known) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    }
    val description = if (known) "${fact.label}: ${status.label}" else "${fact.label}: not reported"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (known) 0.46f else 0.28f),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 9.dp)
                .semantics(mergeDescendants = true) { contentDescription = description },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = fact.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = fact.label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (known) {
                AmenityStatusPill(status)
            }
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
