@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mike.rmpfinder.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.delay
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
                FilterChip(selected = state.filters.attentionOnly, onClick = viewModel::toggleAttentionOnly, label = { Text("Status notes") }, leadingIcon = { Icon(Icons.Default.FilterAlt, null, Modifier.size(18.dp)) })
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
                RestaurantBrandMark(restaurant = restaurant, size = 52)
                Spacer(Modifier.size(12.dp))
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
                Text(availabilityLabel(restaurant, state.now), style = MaterialTheme.typography.labelMedium)
            }
            Text("Business: ${businessStatusLabel(restaurant.businessStatus)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listingNotes(restaurant).firstOrNull()?.let { note ->
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var mapLoadFailed by remember(mapView) { mutableStateOf(false) }

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
                RmpMap(
                    mapView = mapView,
                    restaurants = state.allRestaurants,
                    originLat = state.origin?.latitude,
                    originLon = state.origin?.longitude,
                    onMapPoint = onMapPoint,
                    onMapLoadFailed = { mapLoadFailed = true },
                    onMapLoadSucceeded = { mapLoadFailed = false },
                )
                if (mapLoadFailed) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Map, null, Modifier.size(44.dp))
                            Text("Map unavailable right now", style = MaterialTheme.typography.titleMedium)
                            Text("The restaurant list still works offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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
    onMapLoadFailed: () -> Unit,
    onMapLoadSucceeded: () -> Unit,
) {
    val styleUrl = BuildConfig.MAP_STYLE_URL_OVERRIDE.ifBlank {
        "https://api.maptiler.com/maps/streets-v4/style.json?key=${BuildConfig.MAPTILER_KEY}"
    }
    var mapLoadResolved by remember(mapView, styleUrl) { mutableStateOf(false) }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
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
            map.setStyle(styleUrl) { style ->
                mapLoadResolved = true
                onMapLoadSucceeded()
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        RestaurantBrandMark(restaurant = restaurant, size = 76)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(restaurant.displayName, style = MaterialTheme.typography.headlineSmall)
                            Text(restaurantCategory(restaurant), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(availabilityLabel(restaurant), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Badge { Text("RMP verified ${restaurant.rmpVerifiedAt}") }
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSurface) { Text("10% meal discount") }
                    }
                    val notes = listingNotes(restaurant)
                    if (notes.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Listing notes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                notes.forEach { note ->
                                    Text("• $note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
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
                    DirectionButtons(
                        context = context,
                        address = restaurant.officialAddress,
                        label = "Official RMP address",
                        // The reviewed geocode is more precise than the address
                        // text, and it describes the official RMP location.
                        point = "${restaurant.latitude},${restaurant.longitude}",
                    )
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
                        Text("Status: ${businessStatusLabel(restaurant.businessStatus)}")
                        restaurant.businessCheckedAt?.let { Text("Checked: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item {
                DetailSection("Hours") {
                    Text(availabilityLabel(restaurant), style = MaterialTheme.typography.titleMedium)
                    Text("Hours: ${hoursStatusLabel(restaurant.hoursStatus)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    restaurant.businessCheckedAt?.let { Text("Hours checked: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (restaurant.hoursStatus !in setOf("verified", "usable")) {
                        Text("Open now can’t be confirmed from the available hours.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    // Kept named "Actions": AppAcceptanceTest asserts on this exact
                    // title for acceptance criterion 7, and those instrumented tests
                    // can only be re-run on a real device.
                    DetailSection("Actions") {
                        // The number itself is worth showing, not just a button:
                        // it can be read aloud or copied without placing a call.
                        restaurant.phone?.let { phone -> Text(phone, style = MaterialTheme.typography.bodyLarge) }
                        restaurant.website?.let { url ->
                            Text(
                                text = Uri.parse(url).host?.removePrefix("www.") ?: url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { openUrl(context, url) },
                            )
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            restaurant.phone?.let { phone -> ActionButton(Icons.Default.Phone, "Call") { openIntent(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
                            restaurant.website?.let { url -> ActionButton(Icons.Default.Language, "Website") { openUrl(context, url) } }
                            restaurant.menuUrl?.let { url -> ActionButton(Icons.Default.MenuBook, "Menu") { openUrl(context, url) } }
                        }
                    }
                }
            }
            if (restaurant.sources.isNotEmpty()) {
                item {
                    DetailSection("Where this comes from") {
                        // Every record carries its provenance. Showing it is the
                        // point of keeping official and current facts apart: it
                        // says which claim rests on OTDA and which on a later check.
                        restaurant.sources.forEachIndexed { index, source ->
                            if (index > 0) HorizontalDivider()
                            Text(sourceRoleLabel(source.role), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(source.kind, style = MaterialTheme.typography.bodyMedium)
                            Text("Checked ${source.checkedAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            source.url?.let { url ->
                                Text(
                                    text = Uri.parse(url).host?.removePrefix("www.") ?: url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { openUrl(context, url) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestaurantBrandMark(restaurant: RmpRestaurant, size: Int) {
    val context = LocalContext.current
    val logo = remember(restaurant.website) { RestaurantLogos.forWebsite(context, restaurant.website) }
    val shape = RoundedCornerShape((size * 0.24f).dp)

    Surface(
        modifier = Modifier.size(size.dp).clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        if (logo != null) {
            Image(
                bitmap = logo,
                contentDescription = "${restaurant.displayName} logo",
                modifier = Modifier.fillMaxSize().padding((size * 0.10f).dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            RestaurantBrandFallback(restaurant, size)
        }
    }
}

@Composable
private fun RestaurantBrandFallback(restaurant: RmpRestaurant, size: Int) {
    val category = restaurantCategory(restaurant)
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = restaurant.displayName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "R",
            style = if (size >= 70) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/**
 * Bundled brand marks, decoded once and shared by every row that shows them.
 *
 * Browse is a 241-row list over at most a few dozen distinct logos, so decoding
 * per row would repeat the same work on every scroll pass. Marks are drawn at
 * 52dp and 76dp, so a full 512px source is also downsampled rather than held at
 * full resolution. The cache is bounded by the number of bundled assets.
 */
private object RestaurantLogos {
    private const val TARGET_PIXELS = 256
    private val cache = ConcurrentHashMap<String, Optional<ImageBitmap>>()

    fun forWebsite(context: Context, website: String?): ImageBitmap? {
        val domain = assetDomain(website) ?: return null
        val appContext = context.applicationContext
        return cache.computeIfAbsent(domain) { Optional.ofNullable(decode(appContext, it)) }.orElse(null)
    }

    private fun assetDomain(website: String?): String? = website
        ?.let(Uri::parse)
        ?.host
        ?.lowercase()
        ?.removePrefix("www.")
        ?.replace(Regex("[^a-z0-9.-]+"), "-")
        ?.trim('-', '.')
        ?.takeIf { it.isNotBlank() }

    private fun decode(context: Context, domain: String): ImageBitmap? = runCatching {
        val path = "restaurant-logos/$domain.png"
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
        }
    }.getOrNull()

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= TARGET_PIXELS) sample *= 2
        return sample
    }
}

private fun restaurantCategory(restaurant: RmpRestaurant): String {
    val name = "${restaurant.displayName} ${restaurant.officialName}".lowercase()
    return when {
        listOf("pizza", "pizzeria").any(name::contains) -> "Pizza"
        listOf("burger", "mcdonald", "wendy", "white castle").any(name::contains) -> "Burgers"
        listOf("popeye", "kfc", "chicken", "wing").any(name::contains) -> "Chicken"
        listOf("dunkin", "coffee", "cafe", "café", "bakery").any(name::contains) -> "Café"
        listOf("chinese", "wok", "noodle", "dumpling").any(name::contains) -> "Chinese"
        listOf("jerk", "caribbean", "kairibbean").any(name::contains) -> "Caribbean"
        listOf("seafood", "fish", "crab").any(name::contains) -> "Seafood"
        listOf("deli", "sandwich", "subway").any(name::contains) -> "Deli"
        listOf("shawarma", "mediterranean", "halal").any(name::contains) -> "Mediterranean"
        listOf("taco", "mexican", "burrito").any(name::contains) -> "Mexican"
        listOf("juice", "smoothie").any(name::contains) -> "Juice"
        else -> "Restaurant"
    }
}

private fun availabilityLabel(restaurant: RmpRestaurant, instant: Instant = Instant.now()): String =
    OpenNow.label(restaurant, instant)

private fun sourceRoleLabel(role: String): String = when (role) {
    "rmp_eligibility" -> "RMP eligibility"
    "business_status" -> "Business status"
    "hours" -> "Hours"
    "phone" -> "Phone"
    "website" -> "Website"
    "address" -> "Address"
    "coordinates" -> "Map location"
    "image" -> "Image"
    else -> role.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun businessStatusLabel(status: String): String = when (status) {
    "likely_open" -> "Likely open"
    "temporarily_closed" -> "Temporarily closed"
    "likely_closed" -> "Likely closed"
    "rebranded" -> "Operating under a new name"
    "conflicting" -> "Varies by source"
    "unknown" -> "Not confirmed"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun hoursStatusLabel(status: String): String = when (status) {
    "verified" -> "Verified"
    "usable" -> "Confirmed"
    "stale" -> "Not recently confirmed"
    "partial" -> "Partially confirmed"
    "conflicting" -> "Varies by source"
    "unknown" -> "Not confirmed"
    else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

private fun listingNotes(restaurant: RmpRestaurant): List<String> {
    val notes = linkedSetOf<String>()

    when (restaurant.businessStatus) {
        "temporarily_closed" -> notes += "This business is reported temporarily closed."
        "likely_closed" -> notes += "Recent business information suggests this location may be closed."
        "closed" -> notes += "This business is reported closed."
        "moved" -> notes += restaurant.currentAddress?.let { "The business appears to have moved to ${it.display()}." } ?: "The business appears to have moved."
        "rebranded" -> notes += restaurant.currentName?.let { "The business now appears to operate as $it." } ?: "The business appears to operate under a new name."
        "conflicting" -> notes += "Current business status differs across sources."
        "unknown" -> notes += "Current business status has not been confirmed."
    }

    restaurant.conflictFlags.forEach { flag ->
        when (flag) {
            "name_mismatch" -> notes += restaurant.currentName?.let { "Current business name differs from the RMP listing: $it." } ?: "Current business name differs from the RMP listing."
            "address_mismatch" -> notes += restaurant.currentAddress?.let { "Current business address differs from the RMP listing: ${it.display()}." } ?: "Current business address differs from the RMP listing."
            "moved" -> if (restaurant.businessStatus != "moved") notes += "Current sources indicate the business moved."
            "rebranded" -> if (restaurant.businessStatus != "rebranded") notes += "Current sources indicate the business name changed."
            "hours_conflict" -> notes += "Published hours differ across sources."
            "status_conflict" -> if (restaurant.businessStatus != "conflicting") notes += "Business status differs across sources."
            "phone_conflict" -> notes += "Published phone number differs across sources."
        }
    }

    when (restaurant.hoursStatus) {
        "stale" -> notes += "Hours have not been confirmed recently."
        "partial" -> notes += "Only part of the weekly hours are confirmed."
        "conflicting" -> notes += "Published hours differ across sources."
        "unknown" -> notes += "Current hours have not been confirmed."
    }

    return notes.toList()
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
private fun DirectionButtons(context: Context, address: RmpAddress, label: String, point: String? = null) {
    // A business that has moved is routed by its own address text: the geocode
    // on the record belongs to the official RMP location, not to that address.
    val destination = point ?: address.display()
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { openDirections(context, destination, "transit") }) { Icon(Icons.Default.Subway, null); Text(" Transit") }
        OutlinedButton(onClick = { openDirections(context, destination, "walking") }) { Icon(Icons.Default.DirectionsWalk, null); Text(" Walk") }
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
                    state.metadata[RmpRepository.KEY_LAST_ERROR]?.takeIf { it.isNotBlank() }?.let { Text("Latest update status: $it", color = MaterialTheme.colorScheme.error) }
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
