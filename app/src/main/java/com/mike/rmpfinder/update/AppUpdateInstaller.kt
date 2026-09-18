package com.mike.rmpfinder.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pulls a release APK into the app's own cache and hands it to Android's
 * package installer, so an update never leaves the app for the browser.
 */
class AppUpdateInstaller(private val context: Context) {
    private val dir get() = File(context.cacheDir, "updates")

    /** Downloads [url] to the cache, reporting 0..1 (or null when GitHub sends no length). */
    suspend fun download(url: String, version: String, onProgress: (Float?) -> Unit): File = withContext(Dispatchers.IO) {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "RMP-Finder-$version.apk")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "RMP-Finder")
        try {
            val code = connection.responseCode
            require(code in 200..299) { "HTTP $code" }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(if (total > 0) (copied.toFloat() / total).coerceIn(0f, 1f) else null)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        require(target.length() > 0) { "Empty download" }
        target
    }

    /** Opens the standard Android install sheet for [apk]. */
    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}
