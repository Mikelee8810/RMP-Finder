@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mike.rmpfinder.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mike.rmpfinder.BuildConfig
import com.mike.rmpfinder.MainViewModel
import com.mike.rmpfinder.RmpUiState
import com.mike.rmpfinder.data.OpenNow
import com.mike.rmpfinder.data.RmpAddress
import com.mike.rmpfinder.data.RmpRepository
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.TimePeriod
import com.mike.rmpfinder.update.AppUpdateChecker
import com.mike.rmpfinder.update.AppUpdateState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BreakfastDining
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SetMeal
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow

private enum class MainTab { HOME, MAP, INFO }

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

@Composable
fun RmpFinderRoot(
    viewModel: MainViewModel,
    mapView: MapView,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.allRestaurants.firstOrNull { it.rmpKey == selectedKey }

    BackHandler(enabled = selected != null || tab != MainTab.HOME) {
        if (selected != null) selectedKey = null else tab = MainTab.HOME
    }

    if (state.origin == null) {
        LocationRequiredScreen(onRequestLocation, onOpenLocationSettings)
        return
    }

    if (selected != null) {
        RestaurantDetail(
            restaurant = selected,
            favorite = selected.rmpKey in state.favoriteKeys,
            distanceMiles = state.distances[selected.rmpKey],
            now = state.now,
            onBack = { selectedKey = null },
            onFavorite = { viewModel.toggleFavorite(selected) },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { RmpBottomNavigation(tab = tab, onTab = { tab = it }) },
    ) { padding ->
        when (tab) {
            MainTab.HOME -> HomeScreen(state, viewModel, { selectedKey = it.rmpKey }, Modifier.padding(padding))
            MainTab.MAP -> MapScreen(state, mapView, { selectedKey = it.rmpKey }, Modifier.padding(padding))
            MainTab.INFO -> InfoScreen(state, viewModel::refreshData, viewModel::checkAppUpdate, Modifier.padding(padding))
        }
    }
}

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

@Composable
private fun RmpBottomNavigation(tab: MainTab, onTab: (MainTab) -> Unit) {
    // A floating espresso dock with one tomato bubble for the active tab.
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(50),
            color = RmpTokens.Ink,
            shadowElevation = 18.dp,
        ) {
            Row(Modifier.fillMaxSize().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NavItem(tab == MainTab.HOME, Icons.Rounded.Home, Icons.Outlined.Home, "Home") { onTab(MainTab.HOME) }
                NavItem(tab == MainTab.MAP, Icons.Rounded.Map, Icons.Outlined.Map, "Map") { onTab(MainTab.MAP) }
                NavItem(tab == MainTab.INFO, Icons.Rounded.Person, Icons.Outlined.Person, "Info") { onTab(MainTab.INFO) }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, activeIcon: ImageVector, idleIcon: ImageVector, label: String, onClick: () -> Unit) {
    val tint = if (selected) Color.White else RmpTokens.InkFaint
    Row(
        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50))
            .background(if (selected) RmpTokens.Accent else Color.Transparent)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (selected) activeIcon else idleIcon, contentDescription = label, modifier = Modifier.size(24.dp), tint = tint)
        AnimatedVisibility(visible = selected) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// First run: location
// ---------------------------------------------------------------------------

@Composable
private fun LocationRequiredScreen(onRequestLocation: () -> Unit, onOpenLocationSettings: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("RMP Finder", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.White)
                Text("Food near you,\n10% off.", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text(
                    "241 restaurants on the NY Restaurant Meals Program. Turn on location to sort them by distance. It never leaves your phone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRequestLocation,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                ) { Text("Turn on location", style = MaterialTheme.typography.labelLarge) }
                Text(
                    "Open app settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(role = Role.Button, onClick = onOpenLocationSettings).padding(14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------

private data class Cuisine(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val color: Color,
)

private val QuickCuisines = listOf(
    Cuisine("Pizza", "Pizza", Icons.Rounded.LocalPizza, RmpTokens.Accent),
    Cuisine("Chicken", "Chicken & Wings", Icons.Rounded.Fastfood, RmpTokens.Warn),
    Cuisine("Burgers", "Burgers & Fast Food", Icons.Rounded.LunchDining, RmpTokens.ButterInk),
    Cuisine("Caribbean", "Caribbean", Icons.Rounded.Restaurant, RmpTokens.Open),
    Cuisine("Latin", "Latin American", Icons.Rounded.Restaurant, RmpTokens.Accent),
    Cuisine("Chinese", "Chinese", Icons.Rounded.RamenDining, RmpTokens.Warn),
    Cuisine("Seafood", "Seafood", Icons.Rounded.SetMeal, RmpTokens.Open),
    Cuisine("Café", "Café & Bakery", Icons.Rounded.Coffee, RmpTokens.ButterInk),
    Cuisine("Mexican", "Mexican", Icons.Rounded.Restaurant, RmpTokens.Accent),
    Cuisine("Halal", "Mediterranean & Halal", Icons.Rounded.Restaurant, RmpTokens.Open),
    Cuisine("Breakfast", "Breakfast & Diner", Icons.Rounded.BreakfastDining, RmpTokens.Warn),
    Cuisine("Deli", "Deli & Sandwiches", Icons.Rounded.LunchDining, RmpTokens.ButterInk),
)

/** The warm ground every screen sits on: apricot at the top fading to cream. */
@Composable
private fun WarmGround(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFFFE4CC), Color(0xFFFFF6EA), Color(0xFFFFF6EA))))) { content() }
}

@Composable
private fun HomeScreen(
    state: RmpUiState,
    viewModel: MainViewModel,
    onRestaurant: (RmpRestaurant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showBoroughs by rememberSaveable { mutableStateOf(false) }
    val filters = state.filters
    val filtersActive = filters.query.isNotBlank() || filters.openOnly || filters.favoritesOnly || filters.borough != null || filters.category != null
    val openNearby = remember(state.visibleRestaurants, state.now, filtersActive) {
        if (filtersActive) emptyList() else state.visibleRestaurants.filter { OpenNow.isOpen(it, state.now) }.take(10)
    }

    WarmGround(modifier) {
        if (state.allRestaurants.isEmpty()) { LoadingList(); return@WarmGround }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
            item {
                Column(Modifier.padding(horizontal = 22.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(RmpTokens.Accent), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            }
                            Column {
                                Text("Near you", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Current location", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        IconToggle(selected = filters.favoritesOnly, activeIcon = Icons.Rounded.Favorite, idleIcon = Icons.Rounded.FavoriteBorder, description = "Saved", onClick = viewModel::toggleFavoritesOnly)
                    }
                    Text(
                        buildAnnotatedString {
                            append("Hungry?\n")
                            withStyle(SpanStyle(color = RmpTokens.Accent)) { append("10% off") }
                            append(" near you.")
                        },
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { SearchField(value = filters.query, onValueChange = viewModel::setQuery) }
                        Surface(
                            onClick = { showBoroughs = !showBoroughs },
                            shape = CircleShape,
                            color = if (showBoroughs || filters.borough != null) RmpTokens.Ink else RmpTokens.Accent,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(50.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Tune, contentDescription = "Filters", tint = Color.White) }
                        }
                    }
                }
                AnimatedVisibility(visible = showBoroughs, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChipPill(selected = filters.openOnly, label = "Open now", onClick = viewModel::toggleOpenOnly)
                        MainViewModel.BOROUGHS.forEach { borough ->
                            FilterChipPill(selected = filters.borough == borough, label = borough, onClick = { viewModel.setBorough(borough) })
                        }
                    }
                }
            }
            item {
                // Cuisine rail: white discs on the warm ground.
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    QuickCuisines.forEach { cuisine ->
                        CuisineDisc(cuisine = cuisine, selected = filters.category == cuisine.category, onClick = { viewModel.setCategory(cuisine.category) })
                    }
                }
            }

            if (openNearby.size >= 2) {
                item {
                    SectionHeader("Open right now", modifier = Modifier.padding(bottom = 10.dp), trailing = "${openNearby.size} nearby")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        openNearby.forEach { r -> OpenCard(r, state.distances[r.rmpKey], state.now, onRestaurant) }
                    }
                }
            }

            item {
                SectionHeader(
                    title = when {
                        filters.query.isNotBlank() -> "Results"
                        filters.category != null -> filters.category!!
                        filters.favoritesOnly -> "Saved"
                        filters.openOnly -> "Open now"
                        filters.borough != null -> filters.borough!!
                        else -> "All restaurants"
                    },
                    trailing = if (filtersActive) "Reset" else "${state.visibleRestaurants.size} places",
                    onTrailing = if (filtersActive) { { viewModel.clearFilters(); showBoroughs = false } } else null,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
                )
            }
            if (state.visibleRestaurants.isEmpty()) {
                item { EmptyState(icon = Icons.Rounded.SearchOff, title = "No matches", body = "Try another name, cuisine, or borough.", action = "Reset filters", onAction = viewModel::clearFilters) }
            } else {
                items(state.visibleRestaurants, key = { it.rmpKey }) { restaurant ->
                    Box(Modifier.padding(horizontal = 22.dp, vertical = 5.dp)) {
                        StoreRow(restaurant, state.distances[restaurant.rmpKey], restaurant.rmpKey in state.favoriteKeys, state.now, onRestaurant, onFavorite = { viewModel.toggleFavorite(restaurant) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) {
            if (onTrailing != null) {
                Surface(onClick = onTrailing, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(trailing, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            } else {
                Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun IconToggle(selected: Boolean, activeIcon: ImageVector, idleIcon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .selectable(selected = selected, onClick = onClick, role = Role.Checkbox),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (selected) activeIcon else idleIcon, contentDescription = description, modifier = Modifier.size(22.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = RmpTokens.InkMuted, modifier = Modifier.size(22.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("Search for “pizza”", style = MaterialTheme.typography.bodyLarge, color = RmpTokens.InkFaint, maxLines = 1)
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = RmpTokens.Ink),
                    cursorBrush = SolidColor(RmpTokens.Accent),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search restaurants" },
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Rounded.Cancel, contentDescription = "Clear search", tint = RmpTokens.InkFaint, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CuisineDisc(cuisine: Cuisine, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(70.dp).clip(MaterialTheme.shapes.small).selectable(selected = selected, onClick = onClick, role = Role.Checkbox).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(shape = CircleShape, color = if (selected) RmpTokens.Ink else Color.White, shadowElevation = if (selected) 0.dp else 4.dp, modifier = Modifier.size(62.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    cuisine.icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (selected) RmpTokens.Butter else cuisine.color,
                )
            }
        }
        Text(cuisine.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false, color = if (selected) RmpTokens.Accent else RmpTokens.Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Horizontal card for places open right now. */
@Composable
private fun OpenCard(restaurant: RmpRestaurant, distanceMiles: Double?, now: Instant, onClick: (RmpRestaurant) -> Unit) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    val availability = availabilityOf(restaurant, now)
    Surface(onClick = { onClick(restaurant) }, shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.width(168.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(brand.copy(alpha = 0.18f), brand.copy(alpha = 0.45f)))), contentAlignment = Alignment.Center) {
                BrandCircle(restaurant, logo, size = 68)
                distanceMiles?.let { miles ->
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.92f),
                        contentColor = RmpTokens.Ink,
                    ) {
                        Text(formatMiles(miles), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), maxLines = 1, softWrap = false)
                    }
                }
            }
            Text(restaurant.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Tag(availability?.long ?: "Hours unavailable", availability?.tone ?: Tone.NEUTRAL)
        }
    }
}

@Composable
private fun FilterChipPill(selected: Boolean, label: String, onClick: () -> Unit, icon: ImageVector? = null, trailing: ImageVector? = null) {
    Surface(
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.Checkbox),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) RmpTokens.Ground else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, RmpTokens.Hairline),
    ) {
        Row(Modifier.padding(start = 14.dp, end = if (trailing != null) 8.dp else 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (trailing != null) Icon(trailing, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/** Grid tile: a round logo with the name and distance under it. */
@Composable
private fun StoreTile(modifier: Modifier, restaurant: RmpRestaurant, distanceMiles: Double?, favorite: Boolean, now: Instant, onClick: (RmpRestaurant) -> Unit) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val availability = availabilityOf(restaurant, now)
    Column(
        modifier.clip(MaterialTheme.shapes.small).clickable { onClick(restaurant) }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            BrandCircle(restaurant, logo, size = 84)
            if (favorite) {
                Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                }
            }
        }
        Text(restaurant.displayName, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 17.sp)
        Text(
            listOfNotNull(distanceMiles?.let(::formatMiles), availability?.short?.takeIf { availability.tone == Tone.OPEN }).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = if (availability?.tone == Tone.OPEN) RmpTokens.Open else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** List card: round logo, name, the facts that decide a visit, and a save heart. */
@Composable
private fun StoreRow(restaurant: RmpRestaurant, distanceMiles: Double?, favorite: Boolean, now: Instant, onClick: (RmpRestaurant) -> Unit, onFavorite: () -> Unit) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val availability = availabilityOf(restaurant, now)
    Surface(
        onClick = { onClick(restaurant) },
        shape = RoundedCornerShape(22.dp),
        color = RmpTokens.Paper,
        border = BorderStroke(1.dp, RmpTokens.Hairline),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BrandCircle(restaurant, logo, size = 60)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(restaurant.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (distanceMiles != null) { Text(formatMiles(distanceMiles), style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted); Dot() }
                    Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag("10% off", Tone.BUTTER)
                    if (availability != null) Tag(availability.short, availability.tone)
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = if (favorite) "Remove from saved" else "Save", tint = if (favorite) RmpTokens.Accent else RmpTokens.InkFaint, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun Tag(text: String, tone: Tone) {
    Surface(shape = RoundedCornerShape(50), color = tone.container, contentColor = tone.ink) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@Composable
private fun LoadingList() {
    Column(Modifier.fillMaxSize().semantics { contentDescription = "Loading restaurants" }.padding(horizontal = 22.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { repeat(4) { Box(Modifier.size(width = 84.dp, height = 34.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { repeat(3) { Box(Modifier.weight(1f).height(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) } }
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.55f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                    Box(Modifier.fillMaxWidth(0.35f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                TonalButton(text = action, onClick = onAction)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Map
// ---------------------------------------------------------------------------

@Composable
private fun MapScreen(
    state: RmpUiState,
    mapView: MapView,
    onRestaurant: (RmpRestaurant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mapLoadFailed by remember(mapView) { mutableStateOf(false) }
    var selectedMapKey by rememberSaveable { mutableStateOf<String?>(null) }
    val nearby = state.visibleRestaurants.take(20)
    val selectedMapRestaurant = remember(state.visibleRestaurants, selectedMapKey) {
        state.visibleRestaurants.firstOrNull { it.rmpKey == selectedMapKey }
    }
    val filtersActive = state.filters.query.isNotBlank() || state.filters.openOnly || state.filters.favoritesOnly ||
        state.filters.borough != null || state.filters.category != null

    LaunchedEffect(selectedMapKey, selectedMapRestaurant) {
        if (selectedMapKey != null && selectedMapRestaurant == null) selectedMapKey = null
    }

    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        // The root's floating dock deliberately overlaps the screen. Give the
        // collapsed sheet enough lift that its first results remain readable
        // and tappable rather than disappearing under the dock.
        sheetPeekHeight = 248.dp,
        sheetContainerColor = RmpTokens.Paper,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetDragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline))
        },
        sheetContent = {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.62f)) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (filtersActive) "Matching places" else "Closest to you", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    Text("${nearby.size} ${if (filtersActive) "shown" else "nearby"}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp)) {
                    if (nearby.isEmpty()) {
                        item {
                            MapEmptyState()
                        }
                    } else {
                        items(nearby, key = { it.rmpKey }) { restaurant ->
                            val availability = availabilityOf(restaurant, state.now)
                            Row(
                                Modifier.fillMaxWidth().clickable { onRestaurant(restaurant) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                val ctx = LocalContext.current
                                val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(ctx, restaurant) }
                                BrandCircle(restaurant, logo, size = 52)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(restaurant.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        state.distances[restaurant.rmpKey]?.let { Text(formatMiles(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        Dot()
                                        Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        if (availability != null) {
                                            Dot()
                                            Text(availability.short, style = MaterialTheme.typography.labelSmall, color = availability.tone.ink)
                                        }
                                    }
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = RmpTokens.InkFaint)
                            }
                            Hairline(Modifier.padding(start = 66.dp))
                        }
                    }
                }
            }
        },
    ) { sheetPadding ->
        Box(Modifier.fillMaxSize().padding(sheetPadding)) {
            RmpMap(
                mapView = mapView,
                restaurants = state.visibleRestaurants,
                originLat = state.origin?.latitude,
                originLon = state.origin?.longitude,
                onMapLoadFailed = { mapLoadFailed = true },
                onMapLoadSucceeded = { mapLoadFailed = false },
                onRestaurantSelected = { selectedMapKey = it },
                onMapCleared = { selectedMapKey = null },
            )
            if (mapLoadFailed) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                    EmptyState(icon = Icons.Rounded.Map, title = "Map unavailable right now", body = "The restaurant list still works offline.")
                }
            }
            AnimatedVisibility(
                visible = selectedMapRestaurant != null && !mapLoadFailed,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 16.dp),
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                selectedMapRestaurant?.let { restaurant ->
                    MapRestaurantPreview(
                        restaurant = restaurant,
                        distanceMiles = state.distances[restaurant.rmpKey],
                        now = state.now,
                        onClick = { onRestaurant(restaurant) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MapEmptyState() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(RmpTokens.PaperDeep),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(22.dp), tint = RmpTokens.InkMuted)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("No matches on the map", style = MaterialTheme.typography.titleSmall)
            Text("Clear or change filters on Home to see restaurants.", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted)
        }
    }
}

@Composable
private fun MapRestaurantPreview(
    restaurant: RmpRestaurant,
    distanceMiles: Double?,
    now: Instant,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val availability = availabilityOf(restaurant, now)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = RmpTokens.Ink,
        contentColor = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandCircle(restaurant, logo, size = 54)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(restaurant.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(restaurant.cuisineLabel, distanceMiles?.let(::formatMiles), availability?.short).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = RmpTokens.Butter,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("View details", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = RmpTokens.Butter)
        }
    }
}

@Composable
private fun RmpMap(
    mapView: MapView,
    restaurants: List<RmpRestaurant>,
    originLat: Double?,
    originLon: Double?,
    onMapLoadFailed: () -> Unit,
    onMapLoadSucceeded: () -> Unit,
    onRestaurantSelected: (String) -> Unit,
    onMapCleared: () -> Unit,
) {
    val styleUrl = BuildConfig.MAP_STYLE_URL_OVERRIDE.ifBlank {
        if (BuildConfig.MAPTILER_KEY.isNotBlank()) {
            "https://api.maptiler.com/maps/streets-v4/style.json?key=${BuildConfig.MAPTILER_KEY}"
        } else {
            "https://tiles.openfreemap.org/styles/liberty"
        }
    }
    var mapLoadResolved by remember(mapView, styleUrl) { mutableStateOf(false) }
    var mapLibreMap by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize().semantics {
            contentDescription = "Restaurant map. Tap a red pin to reveal a restaurant."
        },
    )
    DisposableEffect(mapView) {
        val listener = MapView.OnDidFailLoadingMapListener {
            mapLoadResolved = true
            onMapLoadFailed()
        }
        mapView.addOnDidFailLoadingMapListener(listener)
        onDispose { mapView.removeOnDidFailLoadingMapListener(listener) }
    }
    LaunchedEffect(mapView, styleUrl) {
        delay(10_000)
        if (!mapLoadResolved) onMapLoadFailed()
    }
    DisposableEffect(mapLibreMap, onRestaurantSelected, onMapCleared) {
        val liveMap = mapLibreMap ?: return@DisposableEffect onDispose { }
        val listener = MapLibreMap.OnMapClickListener { point ->
            val screenPoint = liveMap.projection.toScreenLocation(point)
            val pin = liveMap.queryRenderedFeatures(screenPoint, "rmp-pins").firstOrNull()
            val restaurantKey = pin?.getStringProperty("rmpKey")
            when {
                restaurantKey != null -> {
                    onRestaurantSelected(restaurantKey)
                    true
                }
                liveMap.queryRenderedFeatures(screenPoint, "rmp-clusters").isNotEmpty() -> {
                    onMapCleared()
                    liveMap.cameraPosition = CameraPosition.Builder()
                        .target(point)
                        .zoom((liveMap.cameraPosition.zoom + 1.75).coerceAtMost(18.0))
                        .build()
                    true
                }
                else -> {
                    onMapCleared()
                    false
                }
            }
        }
        liveMap.addOnMapClickListener(listener)
        onDispose { liveMap.removeOnMapClickListener(listener) }
    }
    LaunchedEffect(restaurants, originLat, originLon) {
        mapView.getMapAsync { map ->
            mapLibreMap = map
            val features = restaurants.map { restaurant ->
                Feature.fromGeometry(Point.fromLngLat(restaurant.longitude, restaurant.latitude)).apply {
                    addStringProperty("rmpKey", restaurant.rmpKey)
                    addStringProperty("name", restaurant.displayName)
                }
            }
            val options = GeoJsonOptions().withCluster(true).withClusterRadius(48).withClusterMaxZoom(14)
            map.setStyle(styleUrl) { style ->
                mapLoadResolved = true
                onMapLoadSucceeded()
                style.addSource(GeoJsonSource("rmp-restaurants", FeatureCollection.fromFeatures(features), options))
                val accent = AndroidColor.parseColor("#E8321C")
                val isCluster = Expression.has("point_count")
                // Single restaurants: a small pin with a white ring so it reads
                // over any street colour. Clusters: a larger disc with the count.
                style.addLayer(
                    CircleLayer("rmp-pins", "rmp-restaurants").withProperties(
                        circleColor(accent),
                        circleRadius(7f),
                        circleStrokeColor(AndroidColor.WHITE),
                        circleStrokeWidth(2.5f),
                    ).withFilter(Expression.not(isCluster)),
                )
                style.addLayer(
                    CircleLayer("rmp-clusters", "rmp-restaurants").withProperties(
                        circleColor(accent),
                        circleRadius(
                            Expression.step(
                                Expression.get("point_count"),
                                Expression.literal(15f),
                                Expression.stop(10, 19f),
                                Expression.stop(30, 24f),
                            ),
                        ),
                        circleStrokeColor(AndroidColor.WHITE),
                        circleStrokeWidth(3f),
                    ).withFilter(isCluster),
                )
                style.addLayer(
                    SymbolLayer("rmp-cluster-count", "rmp-restaurants").withProperties(
                        textField(Expression.toString(Expression.get("point_count_abbreviated"))),
                        textSize(13f),
                        textColor(AndroidColor.WHITE),
                        textFont(arrayOf("Noto Sans Bold")),
                        textAllowOverlap(true),
                        textIgnorePlacement(true),
                    ).withFilter(isCluster),
                )
                if (originLat != null && originLon != null) {
                    style.addSource(GeoJsonSource("user-location", Feature.fromGeometry(Point.fromLngLat(originLon, originLat))))
                    style.addLayer(
                        CircleLayer("user-location-circle", "user-location").withProperties(
                            circleColor(AndroidColor.parseColor("#1A73E8")),
                            circleRadius(9f),
                            circleStrokeColor(AndroidColor.WHITE),
                            circleStrokeWidth(3f),
                        ),
                    )
                }
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(originLat ?: 40.8448, originLon ?: -73.8648))
                    .zoom(if (originLat != null) 12.8 else 10.8)
                    .build()
            }
        }
    }
    LaunchedEffect(originLat, originLon) {
        if (originLat == null || originLon == null) return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder().target(LatLng(originLat, originLon)).zoom(12.8).build()
        }
    }
}

// ---------------------------------------------------------------------------
// Detail
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Detail
// ---------------------------------------------------------------------------

@Composable
private fun RestaurantDetail(
    restaurant: RmpRestaurant,
    favorite: Boolean,
    distanceMiles: Double?,
    now: Instant,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val availability = availabilityOf(restaurant, now)
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    val destination = "${restaurant.latitude},${restaurant.longitude}"
    LightStatusBarIcons()

    WarmGround {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                // Hero: the brand's colour, with the storefront card riding up over it.
                Box(Modifier.fillMaxWidth()) {
                    Box(Modifier.fillMaxWidth().height(250.dp).background(Brush.linearGradient(listOf(brand, darken(brand, 0.35f)))))
                    Box(Modifier.align(Alignment.Center).padding(bottom = 60.dp).size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)))
                    Column(Modifier.padding(top = 196.dp).padding(horizontal = 18.dp)) {
                        Surface(shape = RoundedCornerShape(28.dp), color = Color.White, shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    BrandCircle(restaurant, logo, size = 72)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(restaurant.displayName, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (distanceMiles != null) { Text(formatMiles(distanceMiles), style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted); Dot() }
                                            Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Tag("10% off meals", Tone.BUTTER)
                                    if (availability != null) Tag(availability.long, availability.tone)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = RmpTokens.Accent, modifier = Modifier.size(15.dp))
                                    Text("On the official NY Restaurant Meals Program list · verified ${restaurant.rmpVerifiedAt}", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted)
                                }
                                Hairline(Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuickAction(Modifier.weight(1f), Icons.Rounded.Directions, "Transit", primary = true) { openDirections(context, destination, "transit") }
                                    QuickAction(Modifier.weight(1f), Icons.Rounded.DirectionsWalk, "Walk") { openDirections(context, destination, "walking") }
                                    restaurant.phone?.let { phone -> QuickAction(Modifier.weight(1f), Icons.Rounded.Call, "Call") { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
                                    (restaurant.menuUrl ?: restaurant.website)?.let { url ->
                                        QuickAction(Modifier.weight(1f), if (restaurant.menuUrl != null) Icons.Rounded.MenuBook else Icons.Rounded.Language, if (restaurant.menuUrl != null) "Menu" else "Site") { openUrl(context, url) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                DetailCard("Hours") {
                    val hours = restaurant.hours
                    if (hours != null) {
                        val today = now.atZone(ZoneId.of(hours.timezone)).dayOfWeek
                        val everyDayIsAllDay = DayOfWeek.entries.all { isOpen24Hours(hours.periods(it)) }
                        if (everyDayIsAllDay) {
                            Surface(shape = RoundedCornerShape(14.dp), color = RmpTokens.OpenSoft, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Open 24 hours", style = MaterialTheme.typography.titleSmall, color = RmpTokens.Open, modifier = Modifier.weight(1f))
                                    Text("Every day", style = MaterialTheme.typography.labelLarge, color = RmpTokens.Open)
                                }
                            }
                        } else {
                            DayOfWeek.entries.forEach { day ->
                                val isToday = day == today
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isToday) RmpTokens.Ground else Color.Transparent).padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(day.getDisplayName(TextStyle.FULL, Locale.US), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, color = if (isToday) RmpTokens.Ink else RmpTokens.InkMuted, modifier = Modifier.width(104.dp))
                                    Text(
                                        formatPeriods(hours.periods(day)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isToday) RmpTokens.Ink else RmpTokens.InkMuted,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isToday) {
                                        Text(
                                            "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RmpTokens.Accent,
                                            modifier = Modifier.padding(start = 8.dp),
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            when (restaurant.hoursStatus) {
                                "conflicting" -> "Published hours conflict for this location. Call ahead before you go."
                                "stale" -> "Published hours may be out of date. Call ahead before you go."
                                else -> if (availability == null) "Hours aren’t listed for this location. Call ahead before you go." else "Weekly hours aren’t listed for this location."
                            },
                            style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted,
                        )
                    }
                }
            }
            item {
                DetailCard("Location") {
                    Text(restaurant.officialAddress.display(), style = MaterialTheme.typography.bodyLarge)
                    restaurant.currentAddress?.takeIf { it != restaurant.officialAddress }?.let { current ->
                        Surface(shape = RoundedCornerShape(14.dp), color = RmpTokens.WarnSoft, contentColor = RmpTokens.Warn, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Now operating at", style = MaterialTheme.typography.labelMedium)
                                Text(current.display(), style = MaterialTheme.typography.bodyMedium, color = RmpTokens.Ink)
                                Text("Directions here", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable { openDirections(context, current.display(), "transit") }.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
            if (restaurant.phone != null || restaurant.website != null || restaurant.menuUrl != null) {
                item {
                    // Kept named "Actions": AppAcceptanceTest asserts on this exact
                    // title for acceptance criterion 7, and those instrumented tests
                    // can only be re-run on a real device.
                    DetailCard("Actions") {
                        restaurant.phone?.let { phone -> ContactRow(Icons.Rounded.Call, phone) { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
                        restaurant.website?.let { url -> ContactRow(Icons.Rounded.Language, Uri.parse(url).host?.removePrefix("www.") ?: url) { openUrl(context, url) } }
                        restaurant.menuUrl?.let { url -> ContactRow(Icons.Rounded.MenuBook, "View menu") { openUrl(context, url) } }
                    }
                }
            }
        }
        // Floating controls over the banner
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            FloatingCircleButton(Icons.Rounded.ArrowBack, "Back", onBack)
            FloatingCircleButton(
                icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                description = if (favorite) "Remove from saved" else "Save",
                tint = if (favorite) RmpTokens.Accent else RmpTokens.Ink,
                onClick = onFavorite,
            )
        }
    }
}

/** Flip status-bar icons to light while this composable is on screen (the hero behind them is dark). */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = true }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 14.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun FloatingCircleButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(onClick = onClick, shape = CircleShape, color = Color.White, shadowElevation = 4.dp, modifier = Modifier.size(42.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, label: String, primary: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(shape = CircleShape, color = if (primary) RmpTokens.Accent else Color.White, shadowElevation = 5.dp, modifier = Modifier.size(54.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = if (primary) Color.White else RmpTokens.Ink)
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ContactRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurface)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = RmpTokens.InkFaint)
    }
}

// ---------------------------------------------------------------------------
// Info
// ---------------------------------------------------------------------------

@Composable
private fun InfoScreen(
    state: RmpUiState,
    onRefreshData: () -> Unit,
    onCheckAppUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val updatedAt = state.metadata[RmpRepository.KEY_GENERATED_AT]?.take(10)
    WarmGround(modifier) { LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("RMP Finder", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 6.dp)) }
        item {
            InfoCard(title = "Restaurant directory") {
                Text(
                    "${state.metadata[RmpRepository.KEY_RECORD_COUNT] ?: state.allRestaurants.size} verified locations" + (updatedAt?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(text = if (state.refreshing) "Updating…" else "Update list", loading = state.refreshing, onClick = onRefreshData, modifier = Modifier.fillMaxWidth())
                state.refreshMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            val checking = state.appUpdateState == AppUpdateState.Checking
            InfoCard(title = "App") {
                Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                when (val update = state.appUpdateState) {
                    is AppUpdateState.Available -> {
                        Text("RMP Finder ${update.version} is available.", style = MaterialTheme.typography.bodyMedium)
                        PrimaryButton(text = "Download ${update.version}", onClick = { openUrl(context, update.downloadUrl) }, modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.Current -> Text("You’re on the latest version.", style = MaterialTheme.typography.bodyMedium, color = RmpTokens.Open)
                    AppUpdateState.Failed -> Text("Couldn’t check for an update. Try again later.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    AppUpdateState.Idle, AppUpdateState.Checking -> Unit
                }
                TonalButton(text = if (checking) "Checking…" else "Check for update", loading = checking, onClick = onCheckAppUpdate, modifier = Modifier.fillMaxWidth())
                Text(
                    "Add to Obtainium",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { openObtainium(context) }.padding(vertical = 6.dp),
                )
            }
        }
        item {
            InfoCard(title = "How it works") {
                InfoLine("Restaurants are sorted by distance from where you are.")
                InfoLine("Every listing is on the official NY Restaurant Meals Program directory and gives 10% off meals.")
                InfoLine("Tap a restaurant for hours, contact info, and transit or walking directions.")
            }
        }
    }
} }

@Composable
private fun InfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 4.dp) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun InfoLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------------------
// Shared primitives
// ---------------------------------------------------------------------------

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    Button(
        onClick = onClick, enabled = !loading, modifier = modifier.height(50.dp), shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), disabledContentColor = Color.White,
        ),
    ) {
        if (loading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    Button(
        onClick = onClick, enabled = !loading, modifier = modifier.height(50.dp), shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = RmpTokens.PaperDeep, contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = RmpTokens.PaperDeep, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (loading) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
}

@Composable
private fun Dot() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(RmpTokens.InkFaint))
}

@Composable
private fun Pill(text: String, tone: Tone, solid: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (solid) Color.White else tone.container,
        contentColor = if (solid) Color(0xFF16130F) else tone.ink,
        shadowElevation = if (solid) 1.dp else 0.dp,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

// ---------------------------------------------------------------------------
// Brand tile + colour
// ---------------------------------------------------------------------------

/** The logo as a tile. On a banner it gets a white plate so any mark reads over any brand colour. */
@Composable
private fun BrandTile(restaurant: RmpRestaurant, logo: BrandLogo?, size: Int, onBanner: Boolean = false, ring: Boolean = false) {
    val shape = RoundedCornerShape((size * 0.24f).dp)
    Box(
        Modifier.size(size.dp)
            .then(if (ring) Modifier.shadow(6.dp, shape) else Modifier)
            .clip(shape)
            .background(Color.White)
            .then(if (ring || !onBanner) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            logo != null && logo.fullBleed -> Image(logo.bitmap, contentDescription = "${restaurant.displayName} logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            logo != null -> Image(logo.bitmap, contentDescription = "${restaurant.displayName} logo", modifier = Modifier.fillMaxSize().padding((size * 0.13f).dp), contentScale = ContentScale.Fit)
            else -> Text(
                restaurant.displayName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "R",
                style = if (size >= 70) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                color = fallbackBrandColor(restaurant.displayName),
            )
        }
    }
}

/** Round logo tile used on Home and Map: full-bleed icons crop to the circle; wordmarks sit inside on white. */
@Composable
private fun BrandCircle(restaurant: RmpRestaurant, logo: BrandLogo?, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Color.White).border(1.dp, RmpTokens.Hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            logo != null && logo.fullBleed -> Image(logo.bitmap, contentDescription = "${restaurant.displayName} logo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            logo != null -> Image(logo.bitmap, contentDescription = "${restaurant.displayName} logo", modifier = Modifier.fillMaxSize().padding((size * 0.17f).dp), contentScale = ContentScale.Fit)
            else -> Text(
                restaurant.displayName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "R",
                style = if (size >= 70) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                color = fallbackBrandColor(restaurant.displayName),
            )
        }
    }
}

private fun darken(color: Color, amount: Float): Color =
    Color(color.red * (1 - amount), color.green * (1 - amount), color.blue * (1 - amount), 1f)

/** A stable, pleasant hue for restaurants with no logo, so two cards next to each other never match by accident. */
private fun fallbackBrandColor(name: String): Color {
    val hue = (name.lowercase().hashCode().toUInt() % 360u).toFloat()
    return Color(AndroidColor.HSVToColor(floatArrayOf(hue, 0.62f, 0.62f)))
}

private class BrandLogo(val bitmap: ImageBitmap, val fullBleed: Boolean, val brandColor: Color)

/**
 * Bundled brand marks, decoded once and shared by every row that shows them.
 *
 * Browse is a 241-row list over at most a few dozen distinct logos, so decoding
 * per row would repeat the same work on every scroll pass. Marks are drawn at
 * 48dp to 96dp, so a full 512px source is also downsampled rather than held at
 * full resolution. The cache is bounded by the number of bundled assets.
 */
private object RestaurantLogos {
    private const val TARGET_PIXELS = 256
    private const val LOGO_MAP_ASSET = "restaurant-logo-map.json"
    private val cache = ConcurrentHashMap<String, Optional<BrandLogo>>()
    @Volatile private var mappingCache: Map<String, String>? = null

    fun forRestaurant(context: Context, restaurant: RmpRestaurant): BrandLogo? {
        val appContext = context.applicationContext
        val mapping = mapping(appContext)
        return RestaurantLogoResolver.assetPathsFor(restaurant, mapping).firstNotNullOfOrNull { path ->
            cache.computeIfAbsent(path) { Optional.ofNullable(decode(appContext, it)) }.orElse(null)
        }
    }

    private fun mapping(context: Context): Map<String, String> {
        mappingCache?.let { return it }
        return synchronized(this) {
            mappingCache ?: runCatching {
                context.assets.open(LOGO_MAP_ASSET).bufferedReader().use { reader ->
                    RestaurantLogoResolver.parseMapping(reader.readText())
                }
            }.getOrElse { emptyMap() }.also { mappingCache = it }
        }
    }

    private fun decode(context: Context, path: String): BrandLogo? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
        val bitmap = context.assets.open(path).use { stream -> BitmapFactory.decodeStream(stream, null, options) } ?: return@runCatching null
        BrandLogo(bitmap.asImageBitmap(), isFullBleed(bitmap), brandColorOf(bitmap))
    }.getOrNull()

    /** True when all four corners are painted, i.e. the file is an icon tile rather than a cut-out mark. */
    private fun isFullBleed(bitmap: Bitmap): Boolean {
        if (!bitmap.hasAlpha()) return true
        val w = bitmap.width - 1
        val h = bitmap.height - 1
        return listOf(0 to 0, w to 0, 0 to h, w to h).all { (x, y) -> AndroidColor.alpha(bitmap.getPixel(x, y)) > 200 }
    }

    /**
     * The dominant saturated colour of the mark, pulled toward a banner-friendly
     * lightness. Whites, greys and near-transparent pixels are ignored so a red
     * wordmark on a white tile still yields red.
     */
    private fun brandColorOf(bitmap: Bitmap): Color {
        val step = maxOf(1, bitmap.width / 48)
        val hsv = FloatArray(3)
        var x = 0.0; var y = 0.0; var weight = 0.0; var count = 0
        var sumS = 0.0; var sumV = 0.0
        for (py in 0 until bitmap.height step step) for (px in 0 until bitmap.width step step) {
            val p = bitmap.getPixel(px, py)
            if (AndroidColor.alpha(p) < 128) continue
            AndroidColor.colorToHSV(p, hsv)
            val s = hsv[1]; val v = hsv[2]
            if (s < 0.3f || v < 0.15f) continue
            val w = (s * v).toDouble()
            val rad = Math.toRadians(hsv[0].toDouble())
            x += Math.cos(rad) * w; y += Math.sin(rad) * w; weight += w
            sumS += s; sumV += v; count++
        }
        if (count == 0) return Color(0xFF2B2B2B)
        var hue = Math.toDegrees(Math.atan2(y, x)).toFloat()
        if (hue < 0) hue += 360f
        val sat = (sumS / count).toFloat().coerceIn(0.55f, 0.9f)
        val value = (sumV / count).toFloat().coerceIn(0.55f, 0.82f)
        return Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value)))
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= TARGET_PIXELS) sample *= 2
        return sample
    }
}

// ---------------------------------------------------------------------------
// Availability
// ---------------------------------------------------------------------------

private enum class Tone(val ink: Color, val container: Color) {
    OPEN(RmpTokens.Open, RmpTokens.OpenSoft),
    CLOSED_NOW(RmpTokens.InkMuted, RmpTokens.Paper),
    CLOSED(RmpTokens.Accent, RmpTokens.AccentSoft),
    WARN(RmpTokens.Warn, RmpTokens.WarnSoft),
    NEUTRAL(RmpTokens.Ink, RmpTokens.Paper),
    BUTTER(RmpTokens.ButterInk, RmpTokens.Butter),
}

/** [short] fits in a list row; [long] carries the extra word that removes ambiguity. */
private data class Availability(val short: String, val long: String, val tone: Tone)

/**
 * "Closed" from the business status and "Closed now" from the hours used to
 * render identically. They are different facts: one says the business is gone
 * or shut for a while, the other says it reopens tomorrow.
 */
private fun availabilityOf(restaurant: RmpRestaurant, now: Instant): Availability? = when (restaurant.businessStatus) {
    "closed", "likely_closed" -> Availability("Closed", "Permanently closed", Tone.CLOSED)
    "temporarily_closed" -> Availability("Temporarily closed", "Temporarily closed", Tone.WARN)
    else -> when (OpenNow.label(restaurant, now)) {
        "Open now" -> {
            val hours = restaurant.hours
            val todayPeriods = hours?.let { it.periods(now.atZone(ZoneId.of(it.timezone)).dayOfWeek) }.orEmpty()
            if (isOpen24Hours(todayPeriods)) {
                Availability("24 hr", "Open 24 hours", Tone.OPEN)
            } else {
                val until = closesAt(restaurant, now)
                Availability("Open", if (until != null) "Open · until $until" else "Open now", Tone.OPEN)
            }
        }
        "Closed now" -> Availability("Closed now", "Closed right now", Tone.CLOSED_NOW)
        else -> null
    }
}

private fun closesAt(restaurant: RmpRestaurant, now: Instant): String? {
    val hours = restaurant.hours ?: return null
    val zoned = now.atZone(ZoneId.of(hours.timezone))
    if (isOpen24Hours(hours.periods(zoned.dayOfWeek))) return null
    val minute = zoned.hour * 60 + zoned.minute
    val period = hours.periods(zoned.dayOfWeek).firstOrNull { p ->
        val end = if (p.close == "24:00") 24 * 60 else toMinutes(p.close)
        minute in toMinutes(p.open) until end
    } ?: return null
    return formatClock(period.close)
}

private fun toMinutes(value: String): Int {
    val parts = value.split(':')
    return parts[0].toInt() * 60 + parts[1].toInt()
}

internal fun isOpen24Hours(periods: List<TimePeriod>): Boolean =
    periods.size == 1 && periods.single().open == "00:00" && periods.single().close == "24:00"

internal fun formatPeriods(periods: List<TimePeriod>): String = when {
    periods.isEmpty() -> "Closed"
    isOpen24Hours(periods) -> "Open 24 hours"
    else -> periods.joinToString(", ") { "${formatClock(it.open)} – ${formatClock(it.close)}" }
}

/** "11:00" → "11 AM", "22:30" → "10:30 PM", "24:00" → "Midnight". */
private fun formatClock(value: String): String {
    if (value == "24:00" || value == "00:00") return "Midnight"
    val (h, m) = value.split(':').map { it.toInt() }
    val hour12 = when (h % 12) { 0 -> 12; else -> h % 12 }
    val suffix = if (h < 12) "AM" else "PM"
    return if (m == 0) "$hour12 $suffix" else "$hour12:${"%02d".format(m)} $suffix"
}

private fun formatMiles(miles: Double): String = if (miles < 10) "%.1f mi".format(miles) else "%.0f mi".format(miles)


// ---------------------------------------------------------------------------
// Intents
// ---------------------------------------------------------------------------

private fun openObtainium(context: Context) {
    val source = Uri.encode(AppUpdateChecker.REPOSITORY_URL)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("obtainium://add?url=$source"))
    if (!startActivitySafely(context, intent)) {
        openUrl(context, AppUpdateChecker.REPOSITORY_URL)
    }
}

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

private fun openDirections(context: Context, destination: String, mode: String) {
    val encoded = URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded&travelmode=$mode")
    // Address the Google Maps app directly so directions open there rather than
    // in a browser tab or an app chooser, and fall back to any other handler
    // when Maps is not installed.
    if (!startActivitySafely(context, Intent(Intent.ACTION_VIEW, uri).setPackage(GOOGLE_MAPS_PACKAGE))) {
        startActivitySafely(context, Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun openUrl(context: Context, url: String) = openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

private fun openIntent(context: Context, intent: Intent) {
    startActivitySafely(context, intent)
}

/**
 * Starts [intent] and reports whether anything handled it.
 *
 * resolveActivity is not usable here: from Android 11 it returns null for any
 * app the manifest does not declare in <queries>, so a check that passes on an
 * older phone turns a working button into one that silently does nothing.
 */
private fun startActivitySafely(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}
