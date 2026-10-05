package com.mike.rmpfinder.update

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.mike.rmpfinder.widget.NearbyWidget
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the Glance home-screen widget every 15 minutes:
 * updates open/closed restaurant statuses against the live clock,
 * and refreshes distance calculations using the latest known FusedLocation.
 */
class WidgetRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val hasLocationPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasLocationPerm) {
                runCatching {
                    val client = LocationServices.getFusedLocationProviderClient(applicationContext)
                    val loc = com.google.android.gms.tasks.Tasks.await(client.lastLocation, 5, TimeUnit.SECONDS)
                    if (loc != null) {
                        applicationContext.getSharedPreferences(NearbyWidget.PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putString(NearbyWidget.KEY_LAT, loc.latitude.toString())
                            .putString(NearbyWidget.KEY_LON, loc.longitude.toString())
                            .apply()
                    }
                }
            }

            NearbyWidget().updateAll(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object WidgetRefreshScheduler {
    private const val UNIQUE_WORK = "rmp-periodic-widget-refresh"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(5))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
