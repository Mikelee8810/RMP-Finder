package com.mike.rmpfinder.update

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mike.rmpfinder.RmpFinderApplication
import com.mike.rmpfinder.data.UpdateResult
import java.time.Duration
import java.util.concurrent.TimeUnit

class UpdateWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as RmpFinderApplication
        return when (app.repository.checkForUpdates()) {
            is UpdateResult.Failed -> Result.retry()
            else -> Result.success()
        }
    }
}

object UpdateScheduler {
    private const val UNIQUE_WORK = "rmp-weekly-dataset-check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(7, TimeUnit.DAYS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofHours(1))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
