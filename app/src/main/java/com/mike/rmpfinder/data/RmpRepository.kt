package com.mike.rmpfinder.data

import android.content.res.AssetManager
import androidx.room.withTransaction
import com.mike.rmpfinder.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RmpRepository(
    private val database: RmpDatabase,
    private val assets: AssetManager,
) {
    private val dao = database.dao()

    val restaurants: Flow<List<RmpRestaurant>> = dao.observeRestaurants().map { entities ->
        entities.map { DatasetCodec.parseRestaurant(it.rawJson) }
    }

    val favorites: Flow<Set<String>> = dao.observeFavoriteKeys().map { it.toSet() }
    val metadata: Flow<Map<String, String>> = dao.observeMetadata().map { rows -> rows.associate { it.key to it.value } }

    suspend fun ensureBundledData() = withContext(Dispatchers.IO) {
        val datasetBytes = assets.open("restaurants.json").use { it.readBytes() }
        val manifestBytes = assets.open("manifest.json").use { it.readBytes() }
        val existingKeys = dao.restaurantKeys().toSet()
        val validated = DatasetValidator.validate(manifestBytes, datasetBytes, BuildConfig.VERSION_CODE, existingKeys)
        val installedVersion = dao.metadataValue(KEY_DATASET_VERSION)?.toIntOrNull() ?: 0
        if (existingKeys.isNotEmpty() && installedVersion >= validated.manifest.datasetVersion) return@withContext
        // A bundled upgrade must observe the same removal safeguard as a network
        // update. It keeps saved places intact while refusing a partial directory.
        if (validated.missingExistingKeys.isNotEmpty()) return@withContext
        database.withTransaction {
            if (existingKeys.isNotEmpty()) dao.deleteRestaurants()
            dao.insertRestaurants(validated.entities)
            saveManifestMetadata(validated.manifest)
            dao.putMetadata(MetadataEntity(KEY_LAST_SUCCESS, Instant.now().toString()))
        }
    }

    suspend fun setFavorite(rmpKey: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(FavoriteEntity(rmpKey)) else dao.removeFavorite(rmpKey)
    }

    suspend fun shouldRunAppStartCheck(nowMs: Long = System.currentTimeMillis()): Boolean {
        val last = dao.metadataValue(KEY_LAST_CHECK_MS)?.toLongOrNull() ?: return true
        return nowMs - last >= DAY_MS
    }

    suspend fun checkForUpdates(): UpdateResult = withContext(Dispatchers.IO) {
        dao.putMetadata(MetadataEntity(KEY_LAST_CHECK_MS, System.currentTimeMillis().toString()))
        dao.putMetadata(MetadataEntity(KEY_LAST_CHECK, Instant.now().toString()))
        try {
            val manifestBytes = download(MANIFEST_URL)
            val manifest = DatasetCodec.parseManifest(manifestBytes)
            require(manifest.schemaVersion == SUPPORTED_SCHEMA_VERSION) { "Unsupported dataset schema" }
            require(manifest.minimumAppVersion <= BuildConfig.VERSION_CODE) { "This dataset needs a newer app version" }
            requireSafeDatasetUrl(manifest.datasetUrl)
            val installedVersion = dao.metadataValue(KEY_DATASET_VERSION)?.toIntOrNull() ?: 0
            if (manifest.datasetVersion <= installedVersion) {
                return@withContext UpdateResult.Current(installedVersion)
            }
            val datasetBytes = download(manifest.datasetUrl)
            installDownloadedUpdate(manifestBytes, datasetBytes)
        } catch (error: Exception) {
            val message = error.message ?: "Update failed"
            dao.putMetadata(MetadataEntity(KEY_LAST_ERROR, message))
            UpdateResult.Failed(message)
        }
    }

    internal suspend fun installDownloadedUpdate(manifestBytes: ByteArray, datasetBytes: ByteArray): UpdateResult {
        val oldKeys = dao.restaurantKeys().toSet()
        val validated = DatasetValidator.validate(manifestBytes, datasetBytes, BuildConfig.VERSION_CODE, oldKeys)
        if (validated.missingExistingKeys.isNotEmpty()) {
            dao.putMetadata(MetadataEntity(KEY_LAST_ERROR, "Update paused: ${validated.missingExistingKeys.size} RMP locations are missing from the new list"))
            return UpdateResult.NeedsReview(validated.missingExistingKeys)
        }
        database.withTransaction {
            dao.deleteRestaurants()
            dao.insertRestaurants(validated.entities)
            saveManifestMetadata(validated.manifest)
            dao.putMetadata(MetadataEntity(KEY_LAST_SUCCESS, Instant.now().toString()))
            dao.putMetadata(MetadataEntity(KEY_LAST_ERROR, ""))
        }
        return UpdateResult.Updated(validated.manifest.datasetVersion, validated.restaurants.size)
    }

    private suspend fun saveManifestMetadata(manifest: DatasetManifest) {
        dao.putMetadata(MetadataEntity(KEY_DATASET_VERSION, manifest.datasetVersion.toString()))
        dao.putMetadata(MetadataEntity(KEY_GENERATED_AT, manifest.generatedAt))
        dao.putMetadata(MetadataEntity(KEY_RECORD_COUNT, manifest.recordCount.toString()))
        dao.putMetadata(MetadataEntity(KEY_DISCOUNT, manifest.discountPercent.toString()))
    }

    private fun download(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "RMP-Finder/${BuildConfig.VERSION_NAME}")
        return try {
            val code = connection.responseCode
            require(code in 200..299) { "HTTP $code while checking for updates" }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun requireSafeDatasetUrl(url: String) {
        val uri = URI(url)
        require(uri.scheme == "https") { "Dataset URL must use HTTPS" }
        require(uri.host == "raw.githubusercontent.com") { "Unexpected dataset host" }
        require(uri.path == "/Mikelee8810/RMP-Finder/main/data/restaurants.json") { "Unexpected dataset path" }
    }

    companion object {
        const val MANIFEST_URL = "https://raw.githubusercontent.com/Mikelee8810/RMP-Finder/main/data/manifest.json"
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val KEY_DATASET_VERSION = "dataset_version"
        const val KEY_GENERATED_AT = "generated_at"
        const val KEY_RECORD_COUNT = "record_count"
        const val KEY_DISCOUNT = "discount_percent"
        const val KEY_LAST_CHECK = "last_check"
        const val KEY_LAST_CHECK_MS = "last_check_ms"
        const val KEY_LAST_SUCCESS = "last_success"
        const val KEY_LAST_ERROR = "last_error"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
