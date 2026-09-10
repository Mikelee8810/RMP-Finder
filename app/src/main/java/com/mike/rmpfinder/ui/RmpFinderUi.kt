@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mike.rmpfinder.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mike.rmpfinder.BuildConfig
import com.mike.rmpfinder.MainViewModel
import com.mike.rmpfinder.RmpUiState
import com.mike.rmpfinder.data.OpenNow
import com.mike.rmpfinder.data.RmpAddress
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.RmpRepository
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private enum class MainTab { BROWSE, MAP, SETTINGS }

@Composable
fun RmpFinderRoot(
    viewModel: MainViewModel,
    mapView: MapView,
    onRequestLocation: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.BROWSE) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.allRestaurants.firstOrNull { it.rmpKey == selectedKey }

    if (selected != null) {
        RestaurantDetail(
            restaurant = selected,
            favorite = selected.rmpKey in state.favoriteKeys,
            onBack = { selectedKey = null },
            onFavorite = { viewModel.toggleFavorite(selected) },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RMP Finder", style = MaterialTheme.typography.titleLarge)
                        Text("NY Restaurant Meals Program", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (state.origin != null) {
                        AssistChip(onClick = { viewModel.clearOrigin() }, label = { Text(state.origin!!.source) }, leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp)) })
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == MainTab.BROWSE, onClick = { tab = MainTab.BROWSE }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Browse") })
                NavigationBarItem(selected = tab == MainTab.MAP, onClick = { tab = MainTab.MAP }, icon = { Icon(Icons.Default.Map, null) }, label = { Text("Map") })
                NavigationBarItem(selected = tab == MainTab.SETTINGS, onClick = { tab = MainTab.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Info") })
            }
        },
    ) { padding ->
        when (tab) {
            MainTab.BROWSE -> BrowseScreen(state, viewModel, onRequestLocation, { selectedKey = it.rmpKey }, Modifier.padding(padding))
            MainTab.MAP -> MapScreen(state, mapView, onRequestLocation, { lat, lon -> viewModel.setMapPoint(lat, lon) }, { selectedKey = it.rmpKey }, Modifier.padding(padding))
            MainTab.SETTINGS -> InfoScreen(state, viewModel::refreshData, Modifier.padding(padding))
        }
    }
}

@Composable
private fun BrowseScreen(
    state: RmpUiState,
    viewModel: MainViewModel,
    onRequestLocation: () -> Unit,
    onRestaurant: (RmpRestaurant) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = state.filters.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Search name, address, ZIP, borough") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.origin != null, onClick = onRequestLocation, label = { Text("Nearby") }, leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp)) })
                FilterChip(selected = state.filters.openOnly, onClick = viewModel::toggleOpenOnly, label = { Text("Open now") })
                FilterChip(selected = state.filters.favoritesOnly, onClick = viewModel::toggleFavoritesOnly, label = { Text("Favorites") })
                FilterChip(selected = state.filters.attentionOnly, onClick = viewModel::toggleAttentionOnly, label = { Text("Needs attention") }, leadingIcon = { Icon(Icons.Default.FilterAlt, null, Modifier.size(18.dp)) })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.filters.borough == null, onClick = { viewModel.setBorough(null) }, label = { Text("All") })
                MainViewModel.BOROUGHS.forEach { borough ->
                    FilterChip(selected = state.filters.borough == borough, onClick = { viewModel.setBorough(borough) }, label = { Text(borough) })
                }
            }
            Text("${state.visibleRestaurants.size} of ${state.allRestaurants.size} RMP locations", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.allRestaurants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (state.visibleRestaurants.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No matching locations")
                    OutlinedButton(onClick = viewModel::clearFilters) { Text("Clear filters") }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleRestaurants, key = { it.rmpKey }) { restaurant ->
                    RestaurantCard(restaurant, state, onRestaurant)
                }
            }
        }
    }
}

@Composable
private fun RestaurantCard(restaurant: RmpRestaurant, state: RmpUiState, onClick: (RmpRestaurant) -> Unit) {
    ElevatedCard(onClick = { onClick(restaurant) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(restaurant.displayName, style = MaterialTheme.typography.titleMedium)
                    if (restaurant.currentName != null && restaurant.currentName != restaurant.officialName) {
                        Text("RMP listing: ${restaurant.officialName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.distances[restaurant.rmpKey]?.let { Text("%.1f mi".format(it), style = MaterialTheme.typography.labelLarge) }
            }
            Text(restaurant.officialAddress.display(), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) { Text("RMP verified") }
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSurface) { Text("10% off") }
                Text(OpenNow.label(restaurant, state.now), style = MaterialTheme.typography.labelMedium)
            }
            Text("Business: ${restaurant.businessStatus.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (restaurant.needsAttention) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text("⚠ Check details before traveling", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
private fun MapScreen(
    state: RmpUiState,
    mapView: MapView,
    onRequestLocation: () -> Unit,
    onMapPoint: (Double, Double) -> Unit,
    onRestaurant: (RmpRestaurant) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        sheetPeekHeight = 190.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.58f)) {
                Text(
                    if (state.origin == null) "Nearby list · Bronx-first" else "Nearest to ${state.origin.source}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.visibleRestaurants.take(20), key = { it.rmpKey }) { restaurant ->
                        Card(modifier = Modifier.fillMaxWidth().clickable { onRestaurant(restaurant) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(restaurant.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(restaurant.officialAddress.display(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                state.distances[restaurant.rmpKey]?.let { Text("%.1f mi".format(it)) }
                            }
                        }
                    }
                }
            }
        },
    ) { sheetPadding ->
        Box(Modifier.fillMaxSize().padding(sheetPadding)) {
            if (BuildConfig.MAPTILER_KEY.isBlank()) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Map, null, Modifier.size(44.dp))
                        Text("Map tiles need your MapTiler key", style = MaterialTheme.typography.titleMedium)
                        Text("The restaurant list still works offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                RmpMap(mapView, state.allRestaurants, state.origin?.latitude, state.origin?.longitude, onMapPoint)
            }
            Row(Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestLocation) { Icon(Icons.Default.LocationOn, null); Text(" My location") }
            }
        }
    }
}

@Composable
private fun RmpMap(
    mapView: MapView,
    restaurants: List<RmpRestaurant>,
    originLat: Double?,
    originLon: Double?,
    onMapPoint: (Double, Double) -> Unit,
) {
    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
    LaunchedEffect(restaurants) {
        if (restaurants.isEmpty()) return@LaunchedEffect
        mapView.getMapAsync { map ->
            val features = restaurants.map { restaurant ->
                Feature.fromGeometry(Point.fromLngLat(restaurant.longitude, restaurant.latitude)).apply {
                    addStringProperty("rmpKey", restaurant.rmpKey)
                    addStringProperty("name", restaurant.displayName)
                }
            }
            val options = GeoJsonOptions().withCluster(true).withClusterRadius(44).withClusterMaxZoom(14)
            val styleUrl = "https://api.maptiler.com/maps/streets-v4/style.json?key=${BuildConfig.MAPTILER_KEY}"
            map.setStyle(styleUrl) { style ->
                style.addSource(GeoJsonSource("rmp-restaurants", FeatureCollection.fromFeatures(features), options))
                style.addLayer(
                    CircleLayer("rmp-circles", "rmp-restaurants").withProperties(
                        circleColor(AndroidColor.parseColor("#1F6B4A")),
                        circleRadius(8f),
                    ),
                )
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(originLat ?: 40.8448, originLon ?: -73.8648))
                    .zoom(if (originLat != null) 12.8 else 10.8)
                    .build()
                map.addOnMapClickListener { point ->
                    onMapPoint(point.latitude, point.longitude)
                    false
                }
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

@Composable
private fun RestaurantDetail(
    restaurant: RmpRestaurant,
    favorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(restaurant.displayName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite") } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(OpenNow.label(restaurant), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge { Text("RMP verified ${restaurant.rmpVerifiedAt}") }
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSurface) { Text("10% meal discount") }
                    }
                    if (restaurant.needsAttention) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Column(Modifier.padding(12.dp)) {
                                Text("⚠ Check before you travel", style = MaterialTheme.typography.titleSmall)
                                if (restaurant.conflictFlags.isNotEmpty()) Text(restaurant.conflictFlags.joinToString { it.replace('_', ' ') }, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item {
                DetailSection("Official RMP record") {
                    Text(restaurant.officialName, style = MaterialTheme.typography.titleMedium)
                    Text(restaurant.officialAddress.display())
                    Text("OTDA verification: ${restaurant.rmpVerifiedAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DirectionButtons(context, restaurant.officialAddress, "Official RMP address")
                }
            }
            if (restaurant.currentName != null || restaurant.currentAddress != null || restaurant.businessCheckedAt != null) {
                item {
                    DetailSection("Current business information") {
                        restaurant.currentName?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                        restaurant.currentAddress?.let { address ->
                            Text(address.display())
                            if (address != restaurant.officialAddress) DirectionButtons(context, address, "Current business address")
                        }
                        Text("Status: ${restaurant.businessStatus.replace('_', ' ')}")
                        restaurant.businessCheckedAt?.let { Text("Checked: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item {
                DetailSection("Hours") {
                    Text(OpenNow.label(restaurant), style = MaterialTheme.typography.titleMedium)
                    Text("Hours quality: ${restaurant.hoursStatus.replace('_', ' ')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    restaurant.businessCheckedAt?.let { Text("Hours checked: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (restaurant.hoursStatus !in setOf("verified", "usable")) {
                        Text("These hours are not used for Open now.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    restaurant.hours?.let { hours ->
                        val labels = listOf("Mon" to hours.monday, "Tue" to hours.tuesday, "Wed" to hours.wednesday, "Thu" to hours.thursday, "Fri" to hours.friday, "Sat" to hours.saturday, "Sun" to hours.sunday)
                        labels.forEach { (day, periods) ->
                            Text("$day  ${if (periods.isEmpty()) "Closed" else periods.joinToString { "${it.open}–${it.close}" }}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (restaurant.phone != null || restaurant.website != null || restaurant.menuUrl != null) {
                item {
                    DetailSection("Actions") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            restaurant.phone?.let { phone -> ActionButton(Icons.Default.Phone, "Call") { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
                            restaurant.website?.let { url -> ActionButton(Icons.Default.Language, "Website") { openUrl(context, url) } }
                            restaurant.menuUrl?.let { url -> ActionButton(Icons.Default.MenuBook, "Menu") { openUrl(context, url) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun DirectionButtons(context: Context, address: RmpAddress, label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { openDirections(context, address, "transit") }) { Icon(Icons.Default.Subway, null); Text(" Transit") }
        OutlinedButton(onClick = { openDirections(context, address, "walking") }) { Icon(Icons.Default.DirectionsWalk, null); Text(" Walk") }
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) { Icon(icon, null); Text(" $label") }
}

@Composable
private fun InfoScreen(state: RmpUiState, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Installed RMP directory", style = MaterialTheme.typography.titleMedium)
                    Text("Dataset version ${state.metadata[RmpRepository.KEY_DATASET_VERSION] ?: "—"}")
                    Text("${state.metadata[RmpRepository.KEY_RECORD_COUNT] ?: state.allRestaurants.size} locations")
                    Text("Generated ${state.metadata[RmpRepository.KEY_GENERATED_AT]?.take(10) ?: "—"}")
                    state.metadata[RmpRepository.KEY_LAST_SUCCESS]?.let { Text("Last successful update: ${it.take(19).replace('T', ' ')}") }
                    state.metadata[RmpRepository.KEY_LAST_CHECK]?.let { Text("Last check: ${it.take(19).replace('T', ' ')}") }
                    state.metadata[RmpRepository.KEY_LAST_ERROR]?.takeIf { it.isNotBlank() }?.let { Text("Last update issue: $it", color = MaterialTheme.colorScheme.error) }
                    Button(onClick = onRefresh, enabled = !state.refreshing) {
                        if (state.refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, null)
                        Text(if (state.refreshing) " Checking…" else " Check for updates")
                    }
                    state.refreshMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("What the badges mean", style = MaterialTheme.typography.titleMedium)
                    Text("RMP verified means OTDA lists that exact location for the Restaurant Meals Program.")
                    Text("Open now is only shown when the current business status and structured hours are safe to use.")
                    Text("Conflict warnings keep OTDA facts separate from current business information so a moved or rebranded store cannot silently replace the official RMP address.")
                }
            }
        }
    }
}

private fun openDirections(context: Context, address: RmpAddress, mode: String) {
    val destination = URLEncoder.encode(address.display(), StandardCharsets.UTF_8.toString())
    openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=$mode")))
}

private fun openUrl(context: Context, url: String) = openIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

private fun openIntent(context: Context, intent: Intent) {
    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
}
