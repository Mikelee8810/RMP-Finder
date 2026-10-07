package com.mike.rmpfinder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    val facts = listOf(
        AmenityFact("Dine-in", Icons.Filled.Restaurant, restaurant.dineIn),
        AmenityFact("Takeout", Icons.Filled.Restaurant, restaurant.takeout),
        AmenityFact("Breakfast", Icons.Filled.BreakfastDining, restaurant.servesBreakfast),
        AmenityFact("Lunch", Icons.Filled.LunchDining, restaurant.servesLunch),
        AmenityFact("Dinner", Icons.Filled.DinnerDining, restaurant.servesDinner),
        AmenityFact("Entrance", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleEntrance),
        AmenityFact("Accessible WC", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleRestroom),
        AmenityFact("Accessible seats", Icons.AutoMirrored.Filled.Accessible, restaurant.wheelchairAccessibleSeating),
        AmenityFact("Restroom", Icons.Filled.Wc, restaurant.restroom),
        AmenityFact("Parking", Icons.Filled.LocalParking, restaurant.hasParking),
    ).filter { it.value != null } // Only show confirmed facts to save screen real estate

    if (facts.isEmpty()) return

    // Display as clean, compact chips in two-per-row or wrapped lines
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        facts.chunked(2).forEach { rowFacts ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowFacts.forEach { fact ->
                    CompactAmenityChip(fact, Modifier.weight(1f))
                }
                if (rowFacts.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactAmenityChip(fact: AmenityFact, modifier: Modifier = Modifier) {
    val isYes = fact.value == true
    val bg = if (isYes) Color(0xFFE7F4EA) else Color(0xFFF1F1EF)
    val textAndIcon = if (isYes) Color(0xFF1B6E32) else Color(0xFF6E6E6A)
    val symbol = if (isYes) "✓" else "×"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        contentColor = textAndIcon,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(fact.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = textAndIcon)
            Text(
                fact.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = textAndIcon,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            Text(
                symbol,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textAndIcon,
            )
        }
    }
}
