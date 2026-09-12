package com.mike.rmpfinder.update

import com.mike.rmpfinder.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class Current(val version: String) : AppUpdateState
    data class Available(
        val version: String,
        val downloadUrl: String,
        val releaseUrl: String,
    ) : AppUpdateState
    data object Failed : AppUpdateState
}

class AppUpdateChecker {
    suspend fun check(currentVersion: String = BuildConfig.VERSION_NAME): AppUpdateState = withContext(Dispatchers.IO) {
        try {
            val connection = URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "RMP-Finder/$currentVersion")
            val body = try {
                val code = connection.responseCode
                require(code in 200..299) { "HTTP $code" }
                connection.inputStream.bufferedReader().use { it.readText() }
            } finally {
                connection.disconnect()
            }

            evaluateRelease(currentVersion, JSONObject(body))
        } catch (_: Exception) {
            AppUpdateState.Failed
        }
    }

    companion object {
        const val REPOSITORY_URL = "https://github.com/Mikelee8810/RMP-Finder"
        const val RELEASES_URL = "$REPOSITORY_URL/releases/latest"
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/Mikelee8810/RMP-Finder/releases/latest"

        internal fun evaluateRelease(currentVersion: String, release: JSONObject): AppUpdateState {
            val tag = release.getString("tag_name")
            val latestVersion = tag.removePrefix("v")
            if (compareVersions(latestVersion, currentVersion) <= 0) {
                return AppUpdateState.Current(currentVersion)
            }

            val releaseUrl = release.getString("html_url")
            val assets = release.getJSONArray("assets")
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }
            return AppUpdateState.Available(
                version = latestVersion,
                downloadUrl = apkUrl ?: releaseUrl,
                releaseUrl = releaseUrl,
            )
        }

        internal fun compareVersions(left: String, right: String): Int {
            val a = parseVersion(left)
            val b = parseVersion(right)
            for (index in 0..2) {
                val comparison = a[index].compareTo(b[index])
                if (comparison != 0) return comparison
            }
            return 0
        }

        private fun parseVersion(value: String): List<Int> {
            val parts = value.removePrefix("v").split('.')
            require(parts.size == 3) { "Unsupported version $value" }
            return parts.map { it.toInt() }
        }
    }
}
