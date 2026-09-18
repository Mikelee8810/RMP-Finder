@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mike.rmpfinder.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.LocationOn
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import com.mike.rmpfinder.data.WeeklyHours
import com.mike.rmpfinder.update.AppUpdateChecker
import com.mike.rmpfinder.update.AppUpdateState
import com.mike.rmpfinder.reviews.ReviewsState
import com.mike.rmpfinder.reviews.Rating
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.layout.offset
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconSize
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow

private enum class MainTab { HOME, MAP, SAVED, INFO }

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

@Composable
fun RmpFinderRoot(
    viewModel: MainViewModel,
    mapView: MapView,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    darkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    initialKey: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SystemBarIcons(light = !darkMode)
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(initialKey) { if (initialKey != null) selectedKey = initialKey }
    val selected = state.allRestaurants.firstOrNull { it.rmpKey == selectedKey }

    BackHandler(enabled = selected != null || tab != MainTab.HOME) {
        if (selected != null) selectedKey = null else tab = MainTab.HOME
    }

    // The prompt is about permission, not about a fix having landed yet: once
    // location is granted the app opens straight to Home (sorted by borough
    // until GPS answers, same as with no location at all) instead of
    // re-showing "Turn on location" on every cold start while GPS warms up.
    if (!locationGranted) {
        LocationRequiredScreen(onRequestLocation, onOpenLocationSettings)
        return
    }

    if (selected != null) {
        val reviews by viewModel.reviews.collectAsStateWithLifecycle()
        RestaurantDetail(
            restaurant = selected,
            favorite = selected.rmpKey in state.favoriteKeys,
            distanceMiles = state.distances[selected.rmpKey],
            now = state.now,
            reviews = reviews[selected.rmpKey] ?: if (viewModel.reviewsConfigured) ReviewsState.Loading else ReviewsState.Unavailable,
            onLoadReviews = { address -> viewModel.loadReviews(selected, address) },
            onBack = { selectedKey = null },
            onFavorite = { viewModel.toggleFavorite(selected) },
        )
        return
    }

    val promptOpen by viewModel.updatePromptOpen.collectAsStateWithLifecycle()
    if (promptOpen) UpdatePrompt(state.appUpdateState, viewModel::downloadAppUpdate, viewModel::dismissAppUpdate)

    // Edge to edge: every screen paints under the status bar and under the
    // floating dock, so there is no hard line at either end of the phone.
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (tab) {
            MainTab.HOME -> HomeScreen(state, viewModel, { selectedKey = it.rmpKey })
            MainTab.MAP -> MapScreen(state, viewModel, mapView, { selectedKey = it.rmpKey })
            MainTab.SAVED -> SavedScreen(state, viewModel, { selectedKey = it.rmpKey })
            MainTab.INFO -> InfoScreen(state, viewModel::refreshData, viewModel::checkAppUpdate, viewModel::downloadAppUpdate, darkMode, onToggleDarkMode)
        }
        RmpBottomNavigation(tab = tab, onTab = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

@Composable
private fun RmpBottomNavigation(tab: MainTab, onTab: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    // A floating crimson dock with a butter pill that slides to the active tab,
    // a glassy top highlight and a warm glow underneath so it floats.
    val tabs = MainTab.entries
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 12.dp)) {
        Box(
            Modifier.matchParentSize().padding(horizontal = 22.dp).offset(y = 12.dp).blur(26.dp, BlurredEdgeTreatment.Unbounded)
                .background(RmpTokens.Accent.copy(alpha = 0.55f), RoundedCornerShape(50)),
        )
        Surface(
            modifier = Modifier.fillMaxWidth().height(68.dp),
            shape = RoundedCornerShape(50),
            color = RmpTokens.Dock,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.10f))))
                    .padding(6.dp),
            ) {
                val slot = maxWidth / tabs.size
                val pillX by animateDpAsState(slot * tabs.indexOf(tab), spring(dampingRatio = 0.72f, stiffness = 420f), label = "dockPill")
                Box(
                    Modifier.offset(x = pillX).width(slot).fillMaxHeight().clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(listOf(Color(0xFFFFDC6E), RmpTokens.Butter))),
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    NavItem(tab == MainTab.HOME, Icons.Rounded.Home, Icons.Outlined.Home, "Home") { onTab(MainTab.HOME) }
                    NavItem(tab == MainTab.MAP, Icons.Rounded.Map, Icons.Outlined.Map, "Map") { onTab(MainTab.MAP) }
                    NavItem(tab == MainTab.SAVED, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, "Saved") { onTab(MainTab.SAVED) }
                    NavItem(tab == MainTab.INFO, Icons.Rounded.Person, Icons.Outlined.Person, "Info") { onTab(MainTab.INFO) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, activeIcon: ImageVector, idleIcon: ImageVector, label: String, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) RmpTokens.ButterInk else Color.White.copy(alpha = 0.72f), tween(220), label = "navTint")
    val pop by animateFloatAsState(if (selected) 1.12f else 1f, spring(dampingRatio = 0.45f, stiffness = 650f), label = "navPop")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(50))
            .selectable(selected = selected, interactionSource = interaction, indication = null, onClick = onClick, role = Role.Tab)
            .pressScale(interaction),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (selected) activeIcon else idleIcon, contentDescription = label, tint = tint,
            modifier = Modifier.size(24.dp).graphicsLayer { scaleX = pop; scaleY = pop },
        )
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

private data class Cuisine(val label: String, val category: String, val emoji: String)

private val QuickCuisines = listOf(
    Cuisine("Pizza", "Pizza", "\uD83C\uDF55"),
    Cuisine("Chicken", "Chicken & Wings", "\uD83C\uDF57"),
    Cuisine("Burgers", "Burgers & Fast Food", "\uD83C\uDF54"),
    Cuisine("Caribbean", "Caribbean", "\uD83C\uDF34"),
    Cuisine("Latin", "Latin American", "\uD83C\uDF5B"),
    Cuisine("Chinese", "Chinese", "\uD83E\uDD61"),
    Cuisine("Seafood", "Seafood", "\uD83E\uDD90"),
    Cuisine("Café", "Café & Bakery", "\u2615"),
    Cuisine("Mexican", "Mexican", "\uD83C\uDF2E"),
    Cuisine("Halal", "Mediterranean & Halal", "\uD83E\uDD59"),
    Cuisine("Breakfast", "Breakfast & Diner", "\uD83E\uDD5E"),
    Cuisine("Deli", "Deli & Sandwiches", "\uD83E\uDD6A"),
)

/** The warm ground every screen sits on: apricot at the top fading to cream. */
@Composable
private fun WarmGround(modifier: Modifier = Modifier, underStatusBar: Boolean = false, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(RmpTokens.GroundTop, RmpTokens.Ground, RmpTokens.Ground)))) {
        Box(if (underStatusBar) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding()) { content() }
    }
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
    val filtersActive = filters.query.isNotBlank() || filters.openOnly || filters.favoritesOnly || filters.borough != null || filters.category != null || filters.minRating != null || filters.priceLevels.isNotEmpty()
    val hasRatings = state.ratings.isNotEmpty()
    val openNearby = remember(state.visibleRestaurants, state.now, filtersActive) {
        if (filtersActive) emptyList() else state.visibleRestaurants.filter { OpenNow.isOpen(it, state.now) }.take(10)
    }
    val online = isOnline()
    // Opening a place from a search is the search paying off, so keep the term.
    val open: (RmpRestaurant) -> Unit = { r -> if (filters.query.isNotBlank()) viewModel.rememberSearch(filters.query); onRestaurant(r) }

    WarmGround(modifier) {
        if (state.allRestaurants.isEmpty()) { LoadingList(); return@WarmGround }
        // First paint fades up from the ground instead of popping in.
        val shown = remember { MutableTransitionState(false).apply { targetState = true } }
        AnimatedVisibility(visibleState = shown, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 12 }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
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
                        Box(Modifier.weight(1f)) { SearchField(value = filters.query, onValueChange = viewModel::setQuery, onSearch = { viewModel.rememberSearch(filters.query) }) }
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
                if (!online) {
                    OfflineBanner(Modifier.padding(horizontal = 22.dp).padding(top = 12.dp))
                }
                if (filters.query.isBlank() && state.recentSearches.isNotEmpty()) {
                    // What was searched before, one tap away.
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.recentSearches.forEach { term -> FilterChipPill(selected = false, label = term, icon = Icons.Rounded.History, onClick = { viewModel.setQuery(term) }) }
                        Text("Clear", style = MaterialTheme.typography.labelLarge, color = RmpTokens.InkMuted, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { viewModel.clearRecentSearches() }.padding(horizontal = 8.dp, vertical = 8.dp))
                    }
                }
                AnimatedVisibility(visible = showBoroughs, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChipPill(selected = filters.openOnly, label = "Open now", onClick = viewModel::toggleOpenOnly)
                            MainViewModel.BOROUGHS.forEach { borough ->
                                FilterChipPill(selected = filters.borough == borough, label = borough, onClick = { viewModel.setBorough(borough) })
                            }
                        }
                        if (hasRatings) {
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                StarRatingPicker(selected = filters.minRating, onSelect = viewModel::setMinRating)
                                Box(Modifier.width(1.dp).height(22.dp).background(RmpTokens.Hairline))
                                listOf(1, 2, 3).forEach { level ->
                                    FilterChipPill(selected = level in filters.priceLevels, label = "$".repeat(level), onClick = { viewModel.togglePriceLevel(level) })
                                }
                            }
                        }
                    }
                }
            }
            openNearby.firstOrNull()?.let { nearest ->
                item {
                    NearestOpenHero(nearest, state.distances[nearest.rmpKey], state.now, open, modifier = Modifier.padding(horizontal = 22.dp).padding(top = 18.dp))
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
                        openNearby.forEach { r -> OpenCard(r, state.distances[r.rmpKey], state.now, open, rating = state.ratings[r.rmpKey]) }
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
                    Box(Modifier.animateItem().padding(horizontal = 22.dp, vertical = 5.dp)) {
                        StoreRow(restaurant, state.distances[restaurant.rmpKey], restaurant.rmpKey in state.favoriteKeys, state.now, open, onFavorite = { viewModel.toggleFavorite(restaurant) }, rating = state.ratings[restaurant.rmpKey])
                    }
                }
            }
        }
        }
    }
}

/** Squashes a tile slightly while the finger is down, so taps feel physical. */
@Composable
private fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 900f), label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** The one card that answers "where can I eat right now": the closest open spot. */
@Composable
private fun NearestOpenHero(restaurant: RmpRestaurant, distanceMiles: Double?, now: Instant, onClick: (RmpRestaurant) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    val availability = availabilityOf(restaurant, now)
    val destination = "${restaurant.latitude},${restaurant.longitude}"
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = { onClick(restaurant) },
        interactionSource = interaction,
        shape = RoundedCornerShape(28.dp),
        color = RmpTokens.Dock,
        contentColor = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth().pressScale(interaction),
    ) {
        // Tomato-to-crimson, no brand wash: an orange logo over red turns to mud.
        Box(Modifier.background(Brush.linearGradient(listOf(RmpTokens.Accent, RmpTokens.Dock)))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(RmpTokens.Open))
                    Text("NEAREST OPEN", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp, color = LocalContentColor.current.copy(alpha = 0.75f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BrandCircle(restaurant, logo, size = 64)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(restaurant.displayName, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(distanceMiles?.let(::formatMiles), availability?.short).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                }
                Surface(
                    onClick = { openDirections(context, destination, "transit") },
                    shape = RoundedCornerShape(50),
                    color = RmpTokens.Butter,
                    contentColor = RmpTokens.ButterInk,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take me there", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** True while the phone has a network that can reach the internet. */
@Composable
private fun isOnline(): Boolean {
    val context = LocalContext.current
    val cm = remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    fun current() = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    var online by remember { mutableStateOf(current()) }
    DisposableEffect(cm) {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { online = current() }
            override fun onLost(network: Network) { online = current() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { online = current() }
        }
        cm.registerDefaultNetworkCallback(cb)
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    return online
}

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = RmpTokens.WarnSoft, contentColor = RmpTokens.Warn) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("You're offline · showing the saved list. Map and reviews need a connection.", style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Every saved place, closest first, as its own tab. */
@Composable
private fun SavedScreen(state: RmpUiState, viewModel: MainViewModel, onRestaurant: (RmpRestaurant) -> Unit, modifier: Modifier = Modifier) {
    val saved = remember(state.allRestaurants, state.favoriteKeys, state.distances) {
        state.allRestaurants.filter { it.rmpKey in state.favoriteKeys }
            .sortedWith(compareBy<RmpRestaurant> { state.distances[it.rmpKey] ?: Double.MAX_VALUE }.thenBy { it.displayName.lowercase() })
    }
    val online = isOnline()
    WarmGround(modifier) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            item {
                Column(Modifier.padding(horizontal = 22.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Saved", style = MaterialTheme.typography.displaySmall)
                    Text(
                        if (saved.isEmpty()) "Tap the heart on any place to keep it here." else "${saved.size} place${if (saved.size == 1) "" else "s"} · closest first",
                        style = MaterialTheme.typography.bodyLarge, color = RmpTokens.InkMuted,
                    )
                }
                if (!online) OfflineBanner(Modifier.padding(horizontal = 22.dp).padding(top = 14.dp))
                Spacer(Modifier.height(14.dp))
            }
            if (saved.isEmpty()) {
                item { EmptyState(icon = Icons.Rounded.FavoriteBorder, title = "Nothing saved yet", body = "Your saved spots show up here, even without a connection.") }
            } else {
                items(saved, key = { it.rmpKey }) { restaurant ->
                    Box(Modifier.animateItem().padding(horizontal = 22.dp, vertical = 5.dp)) {
                        StoreRow(restaurant, state.distances[restaurant.rmpKey], true, state.now, onRestaurant, onFavorite = { viewModel.toggleFavorite(restaurant) }, rating = state.ratings[restaurant.rmpKey])
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
private fun SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit = {}) {
    Surface(shape = RoundedCornerShape(50), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = RmpTokens.InkMuted, modifier = Modifier.size(22.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text("Search for “pizza”", style = MaterialTheme.typography.bodyLarge, color = RmpTokens.InkFaint, maxLines = 1)
                BasicTextField(
                    value = value, onValueChange = onValueChange, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = RmpTokens.Ink),
                    cursorBrush = SolidColor(RmpTokens.Accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
        Surface(shape = CircleShape, color = if (selected) RmpTokens.Ink else RmpTokens.Card, contentColor = if (selected) RmpTokens.Card else RmpTokens.Ink, shadowElevation = if (selected) 0.dp else 4.dp, modifier = Modifier.size(62.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(cuisine.emoji, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = null, letterSpacing = 0.sp), fontSize = 30.sp)
            }
        }
        Text(cuisine.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false, color = if (selected) RmpTokens.Accent else RmpTokens.Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold)
    }
}

/** Horizontal card for places open right now. */
@Composable
private fun OpenCard(restaurant: RmpRestaurant, distanceMiles: Double?, now: Instant, onClick: (RmpRestaurant) -> Unit, rating: Rating? = null) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    val availability = availabilityOf(restaurant, now)
    val interaction = remember { MutableInteractionSource() }
    Surface(onClick = { onClick(restaurant) }, interactionSource = interaction, shape = RoundedCornerShape(24.dp), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 8.dp, modifier = Modifier.width(168.dp).pressScale(interaction)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val photo = remember(restaurant.rmpKey) { RestaurantPhotos.forRestaurant(context, restaurant) }
            Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(brand.copy(alpha = 0.18f), brand.copy(alpha = 0.45f)))), contentAlignment = Alignment.Center) {
                if (photo != null) {
                    Image(photo.bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
                }
                BrandCircle(restaurant, logo, size = if (photo != null) 52 else 68)
                distanceMiles?.let { miles ->
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = RoundedCornerShape(50),
                        color = RmpTokens.Card.copy(alpha = 0.92f),
                        contentColor = RmpTokens.Ink,
                    ) {
                        Text(formatMiles(miles), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), maxLines = 1, softWrap = false)
                    }
                }
            }
            Text(restaurant.displayName, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (rating?.rating != null) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFE0A800), modifier = Modifier.size(13.dp))
                    Text("%.1f".format(rating.rating), style = MaterialTheme.typography.labelMedium)
                    rating.priceLevel?.let { Dot(); Text("$".repeat(it), style = MaterialTheme.typography.labelMedium, color = RmpTokens.Open) }
                } else {
                    Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Tag(availability?.long ?: "Hours unavailable", availability?.tone ?: Tone.NEUTRAL)
        }
    }
}

@Composable
private fun StarRatingPicker(selected: Double?, onSelect: (Double?) -> Unit) {
    // Tap a star to mean "this many and up"; tapping the already-selected star clears it.
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            val filled = selected != null && star <= selected
            Icon(
                if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = "$star star${if (star == 1) "" else "s"} and up",
                tint = if (filled) Color(0xFFE0A800) else RmpTokens.InkFaint,
                modifier = Modifier.size(26.dp).clip(CircleShape).selectable(
                    selected = filled,
                    onClick = { onSelect(if (selected == star.toDouble()) null else star.toDouble()) },
                    role = Role.Checkbox,
                ).padding(1.dp),
            )
        }
        if (selected != null) {
            Text(
                "${selected.toInt()}+",
                style = MaterialTheme.typography.labelLarge,
                color = RmpTokens.Ink,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun FilterChipPill(selected: Boolean, label: String, onClick: () -> Unit, icon: ImageVector? = null, trailing: ImageVector? = null) {
    Surface(
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.Checkbox),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.onSurface else RmpTokens.Card,
        contentColor = if (selected) RmpTokens.Card else MaterialTheme.colorScheme.onSurface,
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
private fun StoreRow(restaurant: RmpRestaurant, distanceMiles: Double?, favorite: Boolean, now: Instant, onClick: (RmpRestaurant) -> Unit, onFavorite: () -> Unit, rating: Rating? = null) {
    val context = LocalContext.current
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val availability = availabilityOf(restaurant, now)
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    val interaction = remember { MutableInteractionSource() }
    Surface(onClick = { onClick(restaurant) }, interactionSource = interaction, shape = RoundedCornerShape(24.dp), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth().pressScale(interaction)) {
        Box(
            Modifier.background(
                // A wash of the brand's colour bleeding in from the logo side, so the
                // list reads as a row of storefronts rather than identical cards.
                Brush.horizontalGradient(0f to brand.copy(alpha = 0.22f), 0.45f to brand.copy(alpha = 0.06f), 1f to Color.Transparent),
            ),
        ) {
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(66.dp).clip(CircleShape).background(brand), contentAlignment = Alignment.Center) {
                    BrandCircle(restaurant, logo, size = 60)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(restaurant.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((restaurant.currentAddress ?: restaurant.officialAddress).line1, style = MaterialTheme.typography.bodySmall, color = RmpTokens.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (distanceMiles != null) { Text(formatMiles(distanceMiles), style = MaterialTheme.typography.labelMedium, color = RmpTokens.Ink); Dot() }
                        Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    }
                    if (rating?.rating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFE0A800), modifier = Modifier.size(14.dp))
                            Text("%.1f".format(rating.rating), style = MaterialTheme.typography.labelMedium, color = RmpTokens.Ink)
                            rating.ratingCount?.let { Text("($it)", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted) }
                            rating.priceLevel?.let { Dot(); Text("$".repeat(it), style = MaterialTheme.typography.labelMedium, color = RmpTokens.Open) }
                        }
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
    viewModel: MainViewModel,
    mapView: MapView,
    onRestaurant: (RmpRestaurant) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mapLoadFailed by remember(mapView) { mutableStateOf(false) }
    var selectedMapKey by rememberSaveable { mutableStateOf<String?>(null) }
    var jumpTarget by remember { mutableStateOf<Pair<LatLng, Double>?>(null) }
    var jumpLabel by rememberSaveable { mutableStateOf<String?>(null) }
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
                                    }
                                    if (availability != null) {
                                        Row(Modifier.padding(top = 2.dp)) { Tag(availability.short, availability.tone) }
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
                jumpTarget = jumpTarget,
            )
            // Jump chips: hop the camera to a borough, or back to you, without scrolling.
            Row(
                Modifier.align(Alignment.TopStart).fillMaxWidth().statusBarsPadding().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChipPill(selected = state.filters.openOnly, label = "Open now", icon = Icons.Rounded.Schedule, onClick = viewModel::toggleOpenOnly)
                if (state.origin != null) {
                    FilterChipPill(selected = jumpLabel == null, label = "Me", icon = Icons.Rounded.NearMe, onClick = {
                        jumpLabel = null; jumpTarget = LatLng(state.origin.latitude, state.origin.longitude) to 12.8
                    })
                }
                BoroughJumps.forEach { (name, target) ->
                    FilterChipPill(selected = jumpLabel == name, label = name, onClick = { jumpLabel = name; jumpTarget = target to 11.6 })
                }
            }
            if (mapLoadFailed) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                    EmptyState(icon = Icons.Rounded.Map, title = "Map unavailable right now", body = "The restaurant list still works offline.")
                }
            }
            AnimatedVisibility(
                visible = selectedMapRestaurant != null && !mapLoadFailed,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 16.dp),
                enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
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
        color = RmpTokens.Dock,
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

/** Round logo badge size for map pins: legible at a glance, small enough that dense blocks still cluster. */
private const val MARKER_DIAMETER_DP = 44

/** Where each borough's chip lands the camera: the borough's centre point. */
private val BoroughJumps = listOf(
    "Bronx" to LatLng(40.8448, -73.8648),
    "Brooklyn" to LatLng(40.6782, -73.9442),
    "Manhattan" to LatLng(40.7831, -73.9712),
    "Queens" to LatLng(40.7282, -73.7949),
)

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
    jumpTarget: Pair<LatLng, Double>? = null,
) {
    val dark = RmpTokens.dark
    val styleUrl = BuildConfig.MAP_STYLE_URL_OVERRIDE.ifBlank {
        if (BuildConfig.MAPTILER_KEY.isNotBlank()) {
            "https://api.maptiler.com/maps/streets-v4/style.json?key=${BuildConfig.MAPTILER_KEY}"
        } else if (dark) {
            // OpenFreeMap Fiord: a dark map whose roads still read at night.
            "https://tiles.openfreemap.org/styles/fiord"
        } else {
            "https://tiles.openfreemap.org/styles/liberty"
        }
    }
    var mapLoadResolved by remember(mapView, styleUrl) { mutableStateOf(false) }
    var mapLibreMap by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    val context = LocalContext.current

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize().semantics {
            contentDescription = "Restaurant map. Tap a pin to reveal a restaurant."
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
    val markerDiameterPx = remember(context) { (MARKER_DIAMETER_DP * context.resources.displayMetrics.density).roundToInt() }
    LaunchedEffect(restaurants, originLat, originLon) {
        // Building ~200 composited logo bitmaps touches disk (asset decode) and
        // canvas drawing, so it happens off the main thread before the style
        // (which must be touched from the map's own callback) needs them.
        val markerBitmaps = withContext(Dispatchers.Default) {
            restaurants.associate { it.rmpKey to RestaurantLogos.markerBitmap(context, it, markerDiameterPx) }
        }
        mapView.getMapAsync { map ->
            mapLibreMap = map
            val features = restaurants.map { restaurant ->
                Feature.fromGeometry(Point.fromLngLat(restaurant.longitude, restaurant.latitude)).apply {
                    addStringProperty("rmpKey", restaurant.rmpKey)
                    addStringProperty("name", restaurant.displayName)
                    addStringProperty("iconId", restaurant.rmpKey)
                }
            }
            val options = GeoJsonOptions().withCluster(true).withClusterRadius(48).withClusterMaxZoom(14)
            map.setStyle(styleUrl) { style ->
                mapLoadResolved = true
                onMapLoadSucceeded()
                style.addSource(GeoJsonSource("rmp-restaurants", FeatureCollection.fromFeatures(features), options))
                markerBitmaps.forEach { (iconId, bitmap) -> style.addImage(iconId, bitmap) }
                val accent = AndroidColor.parseColor("#E8321C")
                val isCluster = Expression.has("point_count")
                // Single restaurants: the restaurant's own logo in a small round
                // badge, so the map reads at a glance instead of as plain dots.
                // Clusters: a larger disc with the count.
                style.addLayer(
                    SymbolLayer("rmp-pins", "rmp-restaurants").withProperties(
                        iconImage(Expression.get("iconId")),
                        iconSize(1f),
                        iconAllowOverlap(true),
                        iconIgnorePlacement(true),
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
                    // The ring that breathes outward from the dot; its radius and fade are driven below.
                    style.addLayer(
                        CircleLayer("user-location-pulse", "user-location").withProperties(
                            circleColor(AndroidColor.parseColor("#1A73E8")),
                            circleRadius(9f),
                            circleOpacity(0.4f),
                        ),
                    )
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
    LaunchedEffect(jumpTarget) {
        val (target, zoom) = jumpTarget ?: return@LaunchedEffect
        mapView.getMapAsync { map -> map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom), 700) }
    }
    // "You are here" pulse: grow the ring from the dot and fade it out, on a loop.
    LaunchedEffect(mapLibreMap, originLat, originLon) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (originLat == null || originLon == null) return@LaunchedEffect
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                val t = ((now - start) / 1_800_000_000.0 % 1.0).toFloat()
                (map.style?.getLayer("user-location-pulse") as? CircleLayer)?.setProperties(
                    circleRadius(9f + 26f * t),
                    circleOpacity(0.45f * (1f - t)),
                )
            }
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
    reviews: ReviewsState,
    onLoadReviews: (RmpAddress) -> Unit,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val availability = availabilityOf(restaurant, now)
    val logo = remember(restaurant.rmpKey) { RestaurantLogos.forRestaurant(context, restaurant) }
    val brand = logo?.brandColor ?: fallbackBrandColor(restaurant.displayName)
    // A business that moved is routed by its current address text: the geocode
    // on the record belongs to the official RMP location.
    val whereItIs = restaurant.currentAddress ?: restaurant.officialAddress
    val destination = if (restaurant.currentAddress != null && restaurant.currentAddress != restaurant.officialAddress) whereItIs.display() else "${restaurant.latitude},${restaurant.longitude}"
    LightStatusBarIcons()
    LaunchedEffect(restaurant.rmpKey) { onLoadReviews(whereItIs) }
    // The headline numbers from whichever source answered first with a rating.
    val headline = (reviews as? ReviewsState.Loaded)?.summaries?.firstOrNull { it.rating != null }

    val photo = remember(restaurant.rmpKey) { RestaurantPhotos.forRestaurant(context, restaurant) }
    val blurb = remember(restaurant.rmpKey) { RestaurantBlurbs.forRestaurant(context, restaurant) }
    WarmGround(underStatusBar = true) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
            item {
                // Hero: the storefront photo when we have one (brand colour block if
                // not), with the info card riding up over it.
                Box(Modifier.fillMaxWidth()) {
                    if (photo != null) {
                        Image(photo.bitmap, contentDescription = "${restaurant.displayName} photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(300.dp))
                        Box(Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.35f), 0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f))))
                        if (photo.attribution.isNotBlank()) {
                            Text(
                                "Photo: ${photo.attribution}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 258.dp, end = 18.dp).widthIn(max = 220.dp),
                            )
                        }
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(300.dp)
                                .background(Brush.linearGradient(listOf(brand, darken(brand, 0.45f))))
                                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(0f, 0f), radius = 900f)),
                        )
                        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.TopEnd) {
                            Box(Modifier.offset(x = 70.dp, y = 50.dp).rotate(-10f).alpha(0.30f)) {
                                BrandCircle(restaurant, logo, size = 230)
                            }
                        }
                    }
                    Column(Modifier.padding(top = 276.dp).padding(horizontal = 18.dp)) {
                        Surface(shape = RoundedCornerShape(28.dp), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    BrandCircle(restaurant, logo, size = 72)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(restaurant.displayName, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (distanceMiles != null) { Text(formatMiles(distanceMiles), style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted); Dot() }
                                            Text(restaurant.cuisineLabel, style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        }
                                        if (headline != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Rounded.Star, contentDescription = null, tint = RmpTokens.Butter, modifier = Modifier.size(16.dp))
                                                Text("%.1f".format(headline.rating), style = MaterialTheme.typography.labelLarge)
                                                headline.ratingCount?.let { Text("($it)", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted) }
                                                headline.priceLevel?.let { Dot(); Text("$".repeat(it), style = MaterialTheme.typography.labelLarge, color = RmpTokens.Open) }
                                            }
                                        }
                                    }
                                }
                                blurb?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted) }
                                // The address, right up top: it is the first thing you need.
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RmpTokens.Paper)
                                        .clickable { openUrl(context, mapsSearchUrl(restaurant, whereItIs)) }.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = RmpTokens.Accent, modifier = Modifier.size(20.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(listOfNotNull(whereItIs.line1, whereItIs.line2).joinToString(", "), style = MaterialTheme.typography.titleSmall)
                                        Text("${whereItIs.city}, ${whereItIs.state} ${whereItIs.zip}", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = RmpTokens.InkFaint)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Tag("10% off meals", Tone.BUTTER)
                                    if (availability != null) Tag(availability.long, availability.tone)
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
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (isToday) RmpTokens.PaperDeep else Color.Transparent).padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(day.getDisplayName(TextStyle.FULL, Locale.US), style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, color = if (isToday) RmpTokens.Ink else RmpTokens.InkMuted, modifier = Modifier.width(104.dp))
                                    Text(
                                        displayPeriods(hours, day),
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
                    // Where the business is today. The official RMP address stays in
                    // the dataset for the record but is not what you navigate to.
                    Text(whereItIs.display(), style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted)
                    ContactRow(Icons.Rounded.Map, "Open in Google Maps") { openUrl(context, mapsSearchUrl(restaurant, whereItIs)) }
                }
            }
            run {
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
            item { ReviewsCard(reviews, onOpen = { url -> openUrl(context, url) }, fallbackGoogle = mapsSearchUrl(restaurant, whereItIs)) }
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

@Composable
private fun ReviewsCard(state: ReviewsState, onOpen: (String) -> Unit, fallbackGoogle: String) {
    DetailCard("Reviews") {
        when (state) {
            ReviewsState.Loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = RmpTokens.Accent)
                Text("Pulling the latest reviews…", style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted)
            }
            ReviewsState.Idle, ReviewsState.Unavailable -> {
                Text("Read what people are saying on the apps you already use.", style = MaterialTheme.typography.bodyMedium, color = RmpTokens.InkMuted)
                ContactRow(Icons.Rounded.Star, "Google reviews & rating") { onOpen(fallbackGoogle) }
            }
            is ReviewsState.Loaded -> state.summaries.forEachIndexed { index, summary ->
                if (index > 0) Hairline(Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(summary.source, style = MaterialTheme.typography.titleMedium)
                    summary.rating?.let { rating ->
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = RmpTokens.Butter, modifier = Modifier.size(16.dp))
                        Text("%.1f".format(rating), style = MaterialTheme.typography.labelLarge)
                    }
                    summary.ratingCount?.let { Text("$it reviews", style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkMuted) }
                    summary.priceLevel?.let { Spacer(Modifier.weight(1f)); Text("$".repeat(it), style = MaterialTheme.typography.labelLarge, color = RmpTokens.Open) }
                }
                summary.reviews.take(2).forEach { review ->
                    Surface(shape = RoundedCornerShape(14.dp), color = RmpTokens.PaperDeep, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(review.author, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                Text("★".repeat(review.rating.coerceIn(0, 5)), style = MaterialTheme.typography.labelMedium, color = RmpTokens.Butter.let { Color(0xFFE0A800) })
                                Text(review.when_, style = MaterialTheme.typography.bodySmall, color = RmpTokens.InkFaint)
                            }
                            Text(review.text, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Text(
                    "See all on ${summary.source}",
                    style = MaterialTheme.typography.labelLarge, color = RmpTokens.Accent,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onOpen(summary.listingUrl) }.padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Status and nav bar icons follow the theme: dark icons on the cream ground, light icons on the near-black one. */
@Composable
private fun SystemBarIcons(light: Boolean) {
    val view = LocalView.current
    val ground = RmpTokens.Ground
    val top = RmpTokens.GroundTop
    DisposableEffect(view, light, ground, top) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = light
        controller.isAppearanceLightNavigationBars = light
        // Bars stay see-through so the screen's own colour runs edge to edge.
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        onDispose {}
    }
}

/** Flip status-bar icons to light while this composable is on screen (the hero behind them is dark). */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val wasLight = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = wasLight }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 14.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun FloatingCircleButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(onClick = onClick, shape = CircleShape, color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 4.dp, modifier = Modifier.size(42.dp)) {
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
        Surface(shape = CircleShape, color = if (primary) RmpTokens.Accent else RmpTokens.Card, contentColor = if (primary) Color.White else RmpTokens.Ink, shadowElevation = 5.dp, modifier = Modifier.size(54.dp)) {
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
    onDownloadAppUpdate: () -> Unit,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val updatedAt = state.metadata[RmpRepository.KEY_GENERATED_AT]?.take(10)
    WarmGround(modifier) { LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 120.dp),
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
                        PrimaryButton(text = "Update to ${update.version}", onClick = onDownloadAppUpdate, modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.Downloading -> UpdateProgress(update)
                    is AppUpdateState.Ready -> {
                        Text("${update.version} is downloaded.", style = MaterialTheme.typography.bodyMedium)
                        PrimaryButton(text = "Install", onClick = onDownloadAppUpdate, modifier = Modifier.fillMaxWidth())
                    }
                    is AppUpdateState.DownloadFailed -> {
                        Text("The download didn’t finish.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        PrimaryButton(text = "Try again", onClick = onDownloadAppUpdate, modifier = Modifier.fillMaxWidth())
                        TonalButton(text = "Open in browser", onClick = { openUrl(context, update.available.downloadUrl) }, modifier = Modifier.fillMaxWidth())
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
            InfoCard(title = "Appearance") {
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(role = Role.Switch, onClick = onToggleDarkMode).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Dark mode", style = MaterialTheme.typography.bodyLarge)
                        Text(if (darkMode) "On" else "Off · light is the default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(
                        checked = darkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = RmpTokens.Accent, checkedThumbColor = Color.White),
                    )
                }
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
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = RmpTokens.Card, contentColor = RmpTokens.Ink, shadowElevation = 4.dp) {
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

/** Download bar for an in-app update: a moving bar until GitHub tells us the size, then a real one. */
@Composable
private fun UpdateProgress(update: AppUpdateState.Downloading) {
    val progress = update.progress
    Text(
        if (progress == null) "Downloading ${update.version}…" else "Downloading ${update.version}… ${(progress * 100).toInt()}%",
        style = MaterialTheme.typography.bodyMedium,
    )
    if (progress == null) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = RmpTokens.AccentSoft)
    } else {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, trackColor = RmpTokens.AccentSoft)
    }
}

/**
 * The launch-time "there's a new version" pop-up. Update downloads inside the
 * app and then opens Android's own install sheet; Later hides it for this version.
 */
@Composable
private fun UpdatePrompt(update: AppUpdateState, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    val version = when (update) {
        is AppUpdateState.Available -> update.version
        is AppUpdateState.Downloading -> update.version
        is AppUpdateState.Ready -> update.version
        is AppUpdateState.DownloadFailed -> update.available.version
        else -> return
    }
    val busy = update is AppUpdateState.Downloading
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = RmpTokens.Card,
        titleContentColor = RmpTokens.Ink,
        textContentColor = RmpTokens.InkMuted,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Update available", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (update) {
                    is AppUpdateState.Downloading -> UpdateProgress(update)
                    is AppUpdateState.Ready -> Text("$version is downloaded. Tap Install to finish.", style = MaterialTheme.typography.bodyMedium)
                    is AppUpdateState.DownloadFailed -> Text("The download didn’t finish. Try again?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    else -> Text("RMP Finder $version is ready. You’re on ${BuildConfig.VERSION_NAME}.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate, enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    when (update) {
                        is AppUpdateState.Ready -> "Install"
                        is AppUpdateState.DownloadFailed -> "Try again"
                        is AppUpdateState.Downloading -> "Downloading…"
                        else -> "Update"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Later", color = RmpTokens.InkMuted, style = MaterialTheme.typography.labelLarge) } },
    )
}

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

internal class BrandLogo(val bitmap: ImageBitmap, val fullBleed: Boolean, val brandColor: Color)

/**
 * Bundled brand marks, decoded once and shared by every row that shows them.
 *
 * Browse is a 241-row list over at most a few dozen distinct logos, so decoding
 * per row would repeat the same work on every scroll pass. Marks are drawn at
 * 48dp to 96dp, so a full 512px source is also downsampled rather than held at
 * full resolution. The cache is bounded by the number of bundled assets.
 */
/** One bundled storefront photo per restaurant, fetched at build time by tools/build_photos.py. */
internal class RestaurantPhoto(val bitmap: ImageBitmap, val attribution: String)

internal object RestaurantPhotos {
    private const val MAP_ASSET = "restaurant-photo-map.json"
    private const val DIRECTORY = "restaurant-photos"
    private val cache = ConcurrentHashMap<String, Optional<RestaurantPhoto>>()
    @Volatile private var mapping: Map<String, Pair<String, String>>? = null

    fun forRestaurant(context: Context, restaurant: RmpRestaurant): RestaurantPhoto? {
        val app = context.applicationContext
        val entry = mapping(app)[restaurant.rmpKey] ?: return null
        return cache.computeIfAbsent(restaurant.rmpKey) {
            Optional.ofNullable(
                runCatching {
                    app.assets.open("$DIRECTORY/${entry.first}").use { BitmapFactory.decodeStream(it) }?.let { RestaurantPhoto(it.asImageBitmap(), entry.second) }
                }.getOrNull(),
            )
        }.orElse(null)
    }

    private fun mapping(context: Context): Map<String, Pair<String, String>> = mapping ?: runCatching {
        val root = Json.parseToJsonElement(context.assets.open(MAP_ASSET).bufferedReader().readText()).jsonObject
        root.mapValues { (_, v) ->
            val o = v.jsonObject
            o["file"]!!.jsonPrimitive.content to (o["attribution"]?.jsonPrimitive?.content ?: "")
        }
    }.getOrDefault(emptyMap()).also { mapping = it }
}

/** Google's one-line description of each place, frozen by tools/build_blurbs.py. */
internal object RestaurantBlurbs {
    @Volatile private var map: Map<String, String>? = null
    fun forRestaurant(context: Context, restaurant: RmpRestaurant): String? = (map ?: runCatching {
        Json.parseToJsonElement(context.applicationContext.assets.open("restaurant-blurb-map.json").bufferedReader().readText()).jsonObject
            .mapValues { it.value.jsonPrimitive.content }
    }.getOrDefault(emptyMap()).also { map = it })[restaurant.rmpKey]
}

internal object RestaurantLogos {
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

    private val markerCache = ConcurrentHashMap<String, Bitmap>()

    /**
     * The same brand mark shown everywhere else in the app, composited into a
     * round map-pin badge: white plate + hairline ring, full-bleed marks cropped
     * to fill it, wordmarks padded to fit, and the same lettered fallback used
     * on rows with no bundled logo — so a pin and its detail card always agree.
     */
    fun markerBitmap(context: Context, restaurant: RmpRestaurant, diameterPx: Int): Bitmap =
        markerCache.computeIfAbsent("${restaurant.rmpKey}@$diameterPx") {
            buildMarkerBitmap(context, restaurant, diameterPx)
        }

    private fun buildMarkerBitmap(context: Context, restaurant: RmpRestaurant, diameterPx: Int): Bitmap {
        val logo = forRestaurant(context, restaurant)
        val size = diameterPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        val center = size / 2f
        val radius = size / 2f

        canvas.save()
        canvas.clipPath(Path().apply { addCircle(center, center, radius, Path.Direction.CW) })
        val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (logo != null) AndroidColor.WHITE else fallbackBrandColor(restaurant.displayName).toArgb()
        }
        canvas.drawCircle(center, center, radius, plate)

        when {
            logo != null && logo.fullBleed -> drawScaledBitmap(canvas, logo.bitmap.asAndroidBitmap(), size, pad = 0f)
            logo != null -> drawScaledBitmap(canvas, logo.bitmap.asAndroidBitmap(), size, pad = size * 0.17f)
            else -> {
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = size * 0.46f
                    typeface = Typeface.DEFAULT_BOLD
                }
                val letter = restaurant.displayName.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "R"
                val metrics = textPaint.fontMetrics
                canvas.drawText(letter, center, center - (metrics.ascent + metrics.descent) / 2f, textPaint)
            }
        }
        canvas.restore()

        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size * 0.045f
            color = if (logo != null) LightPalette.Hairline.toArgb() else AndroidColor.WHITE
        }
        canvas.drawCircle(center, center, radius - ring.strokeWidth / 2f, ring)
        return bitmap
    }

    /** Mirrors [BrandCircle]'s Crop-vs-Fit choice, but onto a raw canvas for the map's bitmap icon. */
    private fun drawScaledBitmap(canvas: AndroidCanvas, source: Bitmap, size: Int, pad: Float) {
        val available = size - 2 * pad
        val scale = if (pad > 0f) {
            minOf(available / source.width, available / source.height)
        } else {
            maxOf(size / source.width.toFloat(), size / source.height.toFloat())
        }
        val dx = (size - source.width * scale) / 2f
        val dy = (size - source.height * scale) / 2f
        val matrix = Matrix().apply { setScale(scale, scale); postTranslate(dx, dy) }
        canvas.drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
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

private enum class Tone { OPEN, CLOSED_NOW, CLOSED, WARN, NEUTRAL, BUTTER }

private val Tone.ink: Color
    @Composable @androidx.compose.runtime.ReadOnlyComposable get() = when (this) {
        Tone.OPEN -> RmpTokens.Open
        Tone.CLOSED_NOW -> RmpTokens.InkMuted
        Tone.CLOSED -> RmpTokens.Accent
        Tone.WARN -> RmpTokens.Warn
        Tone.NEUTRAL -> RmpTokens.Ink
        Tone.BUTTER -> if (RmpTokens.dark) RmpTokens.Butter else RmpTokens.ButterInk
    }

private val Tone.container: Color
    @Composable @androidx.compose.runtime.ReadOnlyComposable get() = when (this) {
        Tone.OPEN -> RmpTokens.OpenSoft
        Tone.CLOSED_NOW -> RmpTokens.PaperDeep
        Tone.CLOSED -> RmpTokens.AccentSoft
        Tone.WARN -> RmpTokens.WarnSoft
        Tone.NEUTRAL -> RmpTokens.PaperDeep
        Tone.BUTTER -> RmpTokens.ButterSoft
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
                val minutesLeft = minutesUntilClose(restaurant, now)
                if (until != null && minutesLeft != null && minutesLeft <= CLOSING_SOON_MINUTES) {
                    Availability("Closes $until", "Closing soon · $until", Tone.WARN)
                } else {
                    Availability("Open", if (until != null) "Open · until $until" else "Open now", Tone.OPEN)
                }
            }
        }
        "Closed now" -> Availability("Closed now", "Closed right now", Tone.CLOSED_NOW)
        else -> null
    }
}

/** Under this many minutes to close, "Open" turns into an orange "Closes 9 PM" warning. */
private const val CLOSING_SOON_MINUTES = 45

/** Minutes until the current open period ends, or null when not open / open all day. */
private fun minutesUntilClose(restaurant: RmpRestaurant, now: Instant): Int? {
    val hours = restaurant.hours ?: return null
    val zoned = now.atZone(ZoneId.of(hours.timezone))
    if (isOpen24Hours(hours.periods(zoned.dayOfWeek))) return null
    val minute = zoned.hour * 60 + zoned.minute
    val period = hours.periods(zoned.dayOfWeek).firstOrNull { p ->
        val end = if (p.close == "24:00") 24 * 60 else toMinutes(p.close)
        minute in toMinutes(p.open) until end
    } ?: return null
    var end = if (period.close == "24:00") 24 * 60 else toMinutes(period.close)
    if (period.close == "24:00") {
        val stub = hours.periods(zoned.dayOfWeek.plus(1)).firstOrNull { it.open == "00:00" && it.close != "24:00" }
        if (stub != null) end += toMinutes(stub.close)
    }
    return end - minute
}

internal fun closesAt(restaurant: RmpRestaurant, now: Instant): String? {
    val hours = restaurant.hours ?: return null
    val zoned = now.atZone(ZoneId.of(hours.timezone))
    if (isOpen24Hours(hours.periods(zoned.dayOfWeek))) return null
    val minute = zoned.hour * 60 + zoned.minute
    val period = hours.periods(zoned.dayOfWeek).firstOrNull { p ->
        val end = if (p.close == "24:00") 24 * 60 else toMinutes(p.close)
        minute in toMinutes(p.open) until end
    } ?: return null
    if (period.close == "24:00") {
        val stub = hours.periods(zoned.dayOfWeek.plus(1)).firstOrNull { it.open == "00:00" && it.close != "24:00" }
        if (stub != null) return formatClock(stub.close)
    }
    return formatClock(period.close)
}

private fun toMinutes(value: String): Int {
    val parts = value.split(':')
    return parts[0].toInt() * 60 + parts[1].toInt()
}

internal fun isOpen24Hours(periods: List<TimePeriod>): Boolean =
    periods.size == 1 && periods.single().open == "00:00" && periods.single().close == "24:00"

/**
 * The dataset splits an overnight span at midnight: "10:00–24:00" today and
 * "00:00–05:00" tomorrow. People read that as "10 AM – 5 AM", so the display
 * joins the two halves on the day the span starts and hides the stub on the
 * day it ends.
 */
internal fun displayPeriods(hours: WeeklyHours, day: DayOfWeek): String {
    val today = hours.periods(day)
    if (today.isEmpty()) return "Closed"
    if (isOpen24Hours(today)) return "Open 24 hours"
    val tomorrowStub = hours.periods(day.plus(1)).firstOrNull { it.open == "00:00" && it.close != "24:00" }
    val yesterdayRunsOver = hours.periods(day.minus(1)).lastOrNull()?.close == "24:00"
    val shown = today.filterNot { yesterdayRunsOver && it.open == "00:00" && it.close != "24:00" }
    if (shown.isEmpty()) return "Closed"
    return shown.joinToString(", ") { p ->
        val close = if (p.close == "24:00" && tomorrowStub != null) tomorrowStub.close else p.close
        "${formatClock(p.open)} – ${formatClock(close)}"
    }
}

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
    // Obtainium's own app uses this exact "add/<encoded-url>" path form when it
    // builds add-app links for itself (see MainActivity.transformShareIntent in
    // its source); that is a stronger guarantee of compatibility than the
    // "add?url=" query form its wiki lists as merely "equivalent".
    val source = Uri.encode(AppUpdateChecker.REPOSITORY_URL)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("obtainium://add/$source"))
    if (!startActivitySafely(context, intent)) {
        openUrl(context, AppUpdateChecker.REPOSITORY_URL)
    }
}

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

/** Google Maps place search: opens the listing with its rating, reviews and price level. */
private fun mapsSearchUrl(restaurant: RmpRestaurant, address: RmpAddress): String =
    "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode("${restaurant.displayName} ${address.display()}", StandardCharsets.UTF_8.toString())

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
