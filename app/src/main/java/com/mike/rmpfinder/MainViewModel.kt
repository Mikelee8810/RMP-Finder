package com.mike.rmpfinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mike.rmpfinder.data.OpenNow
import com.mike.rmpfinder.data.RmpRepository
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.UpdateResult
import com.mike.rmpfinder.data.distanceMiles
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GeoPoint(val latitude: Double, val longitude: Double, val source: String)

data class BrowseFilters(
    val query: String = "",
    val borough: String? = null,
    val openOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val attentionOnly: Boolean = false,
)

data class RmpUiState(
    val allRestaurants: List<RmpRestaurant> = emptyList(),
    val visibleRestaurants: List<RmpRestaurant> = emptyList(),
    val favoriteKeys: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val filters: BrowseFilters = BrowseFilters(),
    val origin: GeoPoint? = null,
    val distances: Map<String, Double> = emptyMap(),
    val now: Instant = Instant.now(),
    val refreshing: Boolean = false,
    val refreshMessage: String? = null,
)

private data class BaseData(
    val restaurants: List<RmpRestaurant>,
    val favorites: Set<String>,
    val metadata: Map<String, String>,
)

private data class Selection(
    val filters: BrowseFilters,
    val origin: GeoPoint?,
    val refreshing: Boolean,
    val refreshMessage: String?,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RmpRepository = (application as RmpFinderApplication).repository
    private val filters = MutableStateFlow(BrowseFilters())
    private val origin = MutableStateFlow<GeoPoint?>(null)
    private val refreshing = MutableStateFlow(false)
    private val refreshMessage = MutableStateFlow<String?>(null)
    private val clock = flow {
        while (true) {
            emit(Instant.now())
            delay(60_000)
        }
    }

    private val baseData = combine(repository.restaurants, repository.favorites, repository.metadata) { restaurants, favorites, metadata ->
        BaseData(restaurants, favorites, metadata)
    }

    private val selection = combine(filters, origin, refreshing, refreshMessage) { filters, origin, refreshing, message ->
        Selection(filters, origin, refreshing, message)
    }

    val uiState: StateFlow<RmpUiState> = combine(baseData, selection, clock) { base, selection, now ->
        val (visible, distances) = filterRestaurants(base.restaurants, base.favorites, selection.filters, selection.origin, now)
        RmpUiState(
            allRestaurants = base.restaurants,
            visibleRestaurants = visible,
            favoriteKeys = base.favorites,
            metadata = base.metadata,
            filters = selection.filters,
            origin = selection.origin,
            distances = distances,
            now = now,
            refreshing = selection.refreshing,
            refreshMessage = selection.refreshMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RmpUiState())

    fun setQuery(value: String) = filters.update { copy(query = value) }
    fun setBorough(value: String?) = filters.update { copy(borough = value) }
    fun toggleOpenOnly() = filters.update { copy(openOnly = !openOnly) }
    fun toggleFavoritesOnly() = filters.update { copy(favoritesOnly = !favoritesOnly) }
    fun toggleAttentionOnly() = filters.update { copy(attentionOnly = !attentionOnly) }
    fun clearFilters() { filters.value = BrowseFilters() }

    fun setUserLocation(latitude: Double, longitude: Double) {
        origin.value = GeoPoint(latitude, longitude, "My location")
    }

    fun setMapPoint(latitude: Double, longitude: Double) {
        origin.value = GeoPoint(latitude, longitude, "Map point")
    }

    fun clearOrigin() { origin.value = null }

    fun toggleFavorite(restaurant: RmpRestaurant) {
        viewModelScope.launch {
            repository.setFavorite(restaurant.rmpKey, restaurant.rmpKey !in uiState.value.favoriteKeys)
        }
    }

    fun refreshData() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            refreshMessage.value = null
            refreshMessage.value = when (val result = repository.checkForUpdates()) {
                is UpdateResult.Updated -> "Updated to dataset ${result.version} (${result.count} locations)"
                is UpdateResult.Current -> "Dataset ${result.version} is current"
                is UpdateResult.NeedsReview -> "Update held: ${result.missingKeys.size} existing RMP locations need review"
                is UpdateResult.Failed -> "Update failed: ${result.message}"
            }
            refreshing.value = false
        }
    }

    private inline fun MutableStateFlow<BrowseFilters>.update(block: BrowseFilters.() -> BrowseFilters) {
        value = value.block()
    }

    companion object {
        val BOROUGHS = listOf("Bronx", "Brooklyn", "Manhattan", "Queens", "Staten Island", "Westchester")
        internal val BOROUGH_ORDER = BOROUGHS.withIndex().associate { it.value to it.index }
    }
}

internal fun filterRestaurants(
    restaurants: List<RmpRestaurant>,
    favorites: Set<String>,
    filters: BrowseFilters,
    origin: GeoPoint?,
    now: Instant,
): Pair<List<RmpRestaurant>, Map<String, Double>> {
    val distances = origin?.let { point ->
        restaurants.associate { restaurant ->
            restaurant.rmpKey to distanceMiles(point.latitude, point.longitude, restaurant.latitude, restaurant.longitude)
        }
    }.orEmpty()
    val query = filters.query.trim().lowercase()
    val visible = restaurants.asSequence()
        .filter { query.isEmpty() || restaurantSearchText(it).contains(query) }
        .filter { filters.borough == null || it.borough == filters.borough }
        .filter { !filters.openOnly || OpenNow.isOpen(it, now) }
        .filter { !filters.favoritesOnly || it.rmpKey in favorites }
        .filter { !filters.attentionOnly || it.needsAttention }
        .sortedWith(
            if (origin != null) {
                compareBy<RmpRestaurant> { distances[it.rmpKey] ?: Double.MAX_VALUE }.thenBy { it.displayName.lowercase() }
            } else {
                compareBy<RmpRestaurant> { MainViewModel.BOROUGH_ORDER[it.borough] ?: 99 }.thenBy { it.displayName.lowercase() }
            },
        )
        .toList()
    return visible to distances
}

private fun restaurantSearchText(restaurant: RmpRestaurant): String = buildList {
    add(restaurant.officialName)
    restaurant.currentName?.let(::add)
    addAll(restaurant.aliases)
    add(restaurant.officialAddress.display())
    restaurant.currentAddress?.display()?.let(::add)
    add(restaurant.borough)
    add(restaurant.zip)
}.joinToString(" ").lowercase()
