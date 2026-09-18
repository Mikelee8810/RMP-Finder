package com.mike.rmpfinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mike.rmpfinder.data.OpenNow
import com.mike.rmpfinder.data.RmpRepository
import com.mike.rmpfinder.data.RmpRestaurant
import com.mike.rmpfinder.data.UpdateResult
import com.mike.rmpfinder.data.distanceMiles
import com.mike.rmpfinder.update.AppUpdateChecker
import com.mike.rmpfinder.update.AppUpdateInstaller
import com.mike.rmpfinder.widget.NearbyWidget
import com.mike.rmpfinder.update.AppUpdateState
import com.mike.rmpfinder.reviews.ReviewsFetcher
import com.mike.rmpfinder.reviews.ReviewsState
import com.mike.rmpfinder.reviews.Rating
import com.mike.rmpfinder.reviews.RatingsStore
import com.mike.rmpfinder.data.RmpAddress
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GeoPoint(val latitude: Double, val longitude: Double, val source: String)

data class BrowseFilters(
    val query: String = "",
    val borough: String? = null,
    val category: String? = null,
    val openOnly: Boolean = false,
    val favoritesOnly: Boolean = false,
    val attentionOnly: Boolean = false,
    /** Google rating floor, e.g. 4.0 or 4.5. */
    val minRating: Double? = null,
    /** Google price levels 1..4 to keep; empty means any. */
    val priceLevels: Set<Int> = emptySet(),
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
    val appUpdateState: AppUpdateState = AppUpdateState.Idle,
    val ratings: Map<String, Rating> = emptyMap(),
    /** The last few things typed into search, newest first. */
    val recentSearches: List<String> = emptyList(),
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
    val appUpdateState: AppUpdateState,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RmpRepository = (application as RmpFinderApplication).repository
    private val filters = MutableStateFlow(BrowseFilters())
    private val prefs = application.getSharedPreferences("rmp_prefs", android.content.Context.MODE_PRIVATE)
    private val recentSearches = MutableStateFlow(prefs.getString(PREF_RECENTS, "").orEmpty().split('\n').filter { it.isNotBlank() })
    private val origin = MutableStateFlow<GeoPoint?>(null)
    private val refreshing = MutableStateFlow(false)
    private val refreshMessage = MutableStateFlow<String?>(null)
    private val appUpdateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    private val appUpdateChecker = AppUpdateChecker()
    private val appUpdateInstaller = AppUpdateInstaller(application)
    /** True while the "there's an update" pop-up should be on screen. */
    val updatePromptOpen: StateFlow<Boolean> get() = updatePrompt
    private val updatePrompt = MutableStateFlow(false)
    private val reviewsFetcher = ReviewsFetcher()
    private val ratingsStore = RatingsStore(application, reviewsFetcher)
    private val reviewsByKey = MutableStateFlow<Map<String, ReviewsState>>(emptyMap())
    /** Reviews for whichever restaurant is open; keyed so going back and forth never refetches. */
    val reviews: StateFlow<Map<String, ReviewsState>> = reviewsByKey
    val reviewsConfigured: Boolean get() = reviewsFetcher.isConfigured

    fun loadReviews(restaurant: RmpRestaurant, address: RmpAddress) {
        if (!reviewsFetcher.isConfigured) return
        if (reviewsByKey.value[restaurant.rmpKey] is ReviewsState.Loaded) return
        reviewsByKey.value = reviewsByKey.value + (restaurant.rmpKey to ReviewsState.Loading)
        viewModelScope.launch {
            val summaries = reviewsFetcher.fetch(restaurant, address)
            reviewsByKey.value = reviewsByKey.value + (restaurant.rmpKey to if (summaries.isEmpty()) ReviewsState.Unavailable else ReviewsState.Loaded(summaries))
        }
    }
    private val clock = flow {
        while (true) {
            emit(Instant.now())
            delay(60_000)
        }
    }

    private val baseData = combine(repository.restaurants, repository.favorites, repository.metadata) { restaurants, favorites, metadata ->
        BaseData(restaurants, favorites, metadata)
    }

    private val selection = combine(filters, origin, refreshing, refreshMessage, appUpdateState) { filters, origin, refreshing, message, appUpdate ->
        Selection(filters, origin, refreshing, message, appUpdate)
    }

    init {
        // Quiet check on launch: a newer release opens the update pop-up once
        // per version; a failed or up-to-date check shows nothing.
        viewModelScope.launch {
            val result = appUpdateChecker.check()
            if (result is AppUpdateState.Available && appUpdateState.value == AppUpdateState.Idle) {
                appUpdateState.value = result
                if (prefs.getString(PREF_UPDATE_DISMISSED, null) != result.version) updatePrompt.value = true
            }
        }
        // Ratings for the whole list, once, in the background; the list shows
        // them as they land and the filters start working when they are in.
        viewModelScope.launch {
            repository.restaurants.collect { restaurants ->
                if (restaurants.isNotEmpty()) { ratingsStore.fill(restaurants); return@collect }
            }
        }
    }

    val uiState: StateFlow<RmpUiState> = combine(baseData, selection, clock, ratingsStore.ratings, recentSearches) { base, selection, now, ratings, recents ->
        val (visible, distances) = filterRestaurants(base.restaurants, base.favorites, selection.filters, selection.origin, now, ratings)
        RmpUiState(
            ratings = ratings,
            recentSearches = recents,
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
            appUpdateState = selection.appUpdateState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RmpUiState())

    fun setQuery(value: String) = filters.update { copy(query = value) }
    fun setBorough(value: String?) = filters.update { copy(borough = if (borough == value) null else value) }
    fun setCategory(value: String?) = filters.update { copy(category = if (category == value) null else value) }
    fun toggleOpenOnly() = filters.update { copy(openOnly = !openOnly) }
    fun toggleFavoritesOnly() = filters.update { copy(favoritesOnly = !favoritesOnly) }
    fun toggleAttentionOnly() = filters.update { copy(attentionOnly = !attentionOnly) }
    fun setMinRating(value: Double?) = filters.update { copy(minRating = if (minRating == value) null else value) }
    fun togglePriceLevel(level: Int) = filters.update { copy(priceLevels = if (level in priceLevels) priceLevels - level else priceLevels + level) }
    fun clearFilters() { filters.value = BrowseFilters() }

    /** Remember what was searched so it can be tapped again next time. */
    fun rememberSearch(raw: String) {
        val term = raw.trim()
        if (term.length < 2) return
        val next = (listOf(term) + recentSearches.value.filterNot { it.equals(term, ignoreCase = true) }).take(MAX_RECENTS)
        recentSearches.value = next
        prefs.edit().putString(PREF_RECENTS, next.joinToString("\n")).apply()
    }

    fun clearRecentSearches() {
        recentSearches.value = emptyList()
        prefs.edit().remove(PREF_RECENTS).apply()
    }

    fun setUserLocation(latitude: Double, longitude: Double) {
        origin.value = GeoPoint(latitude, longitude, "My location")
        viewModelScope.launch { NearbyWidget.refresh(getApplication(), latitude, longitude) }
        // Turn the fix into a street name so you can see the GPS is right.
        viewModelScope.launch {
            val label = reverseGeocode(latitude, longitude) ?: return@launch
            if (origin.value?.latitude == latitude && origin.value?.longitude == longitude) {
                origin.value = GeoPoint(latitude, longitude, label)
            }
        }
    }

    /** "2366 Grand Concourse, Bronx" from Android's own address lookup; null when it has no answer. */
    private suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.US)
            if (!android.location.Geocoder.isPresent()) return@runCatching null
            @Suppress("DEPRECATION")
            val address = geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() ?: return@runCatching null
            val street = listOfNotNull(address.subThoroughfare, address.thoroughfare).joinToString(" ").ifBlank { null }
            val area = address.subLocality ?: address.locality
            listOfNotNull(street, area).joinToString(", ").ifBlank { null }
        }.getOrNull()
    }

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
                is UpdateResult.Updated -> "Restaurant list updated (${result.count} locations)."
                is UpdateResult.Current -> "Restaurant list is up to date."
                is UpdateResult.NeedsReview -> "A new restaurant list is available but needs review before it can be applied."
                is UpdateResult.Failed -> "Couldn’t update the restaurant list. Try again later."
            }
            refreshing.value = false
        }
    }

    fun checkAppUpdate() {
        if (appUpdateState.value == AppUpdateState.Checking || appUpdateState.value is AppUpdateState.Downloading) return
        viewModelScope.launch {
            appUpdateState.value = AppUpdateState.Checking
            appUpdateState.value = appUpdateChecker.check()
        }
    }

    /** Pulls the new APK down inside the app, then opens Android's install sheet. */
    fun downloadAppUpdate() {
        val available = when (val current = appUpdateState.value) {
            is AppUpdateState.Available -> current
            is AppUpdateState.DownloadFailed -> current.available
            is AppUpdateState.Ready -> { appUpdateInstaller.install(current.apk); return }
            else -> return
        }
        viewModelScope.launch {
            appUpdateState.value = AppUpdateState.Downloading(available.version, null)
            try {
                val apk = appUpdateInstaller.download(available.downloadUrl, available.version) { progress ->
                    appUpdateState.value = AppUpdateState.Downloading(available.version, progress)
                }
                appUpdateState.value = AppUpdateState.Ready(available.version, apk)
                appUpdateInstaller.install(apk)
            } catch (_: Exception) {
                appUpdateState.value = AppUpdateState.DownloadFailed(available)
            }
        }
    }

    /** Closes the pop-up and keeps it closed for this version; Info still offers the update. */
    fun dismissAppUpdate() {
        updatePrompt.value = false
        val version = when (val current = appUpdateState.value) {
            is AppUpdateState.Available -> current.version
            is AppUpdateState.Downloading -> current.version
            is AppUpdateState.Ready -> current.version
            is AppUpdateState.DownloadFailed -> current.available.version
            else -> null
        }
        if (version != null) prefs.edit().putString(PREF_UPDATE_DISMISSED, version).apply()
    }

    private inline fun MutableStateFlow<BrowseFilters>.update(block: BrowseFilters.() -> BrowseFilters) {
        value = value.block()
    }

    companion object {
        private const val PREF_RECENTS = "recent_searches"
        private const val MAX_RECENTS = 6
        private const val PREF_UPDATE_DISMISSED = "update_dismissed_version"
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
    ratings: Map<String, Rating> = emptyMap(),
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
        .filter { filters.category == null || filters.category in it.cuisineCategories }
        .filter { !filters.openOnly || OpenNow.isOpen(it, now) }
        .filter { !filters.favoritesOnly || it.rmpKey in favorites }
        .filter { !filters.attentionOnly || it.needsAttention }
        .filter { filters.minRating == null || (ratings[it.rmpKey]?.rating ?: 0.0) >= filters.minRating }
        .filter { filters.priceLevels.isEmpty() || ratings[it.rmpKey]?.priceLevel in filters.priceLevels }
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
    val names = buildList {
        add(restaurant.officialName)
        restaurant.currentName?.let(::add)
        addAll(restaurant.aliases)
    }
    if (names.any { it.contains("Kentucky Fried Chicken", ignoreCase = true) }) {
        add("KFC")
    }
    add(restaurant.officialAddress.display())
    restaurant.currentAddress?.display()?.let(::add)
    add(restaurant.borough)
    add(restaurant.zip)
    addAll(restaurant.cuisineCategories)
}.joinToString(" ").lowercase()
