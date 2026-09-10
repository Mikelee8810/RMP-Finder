package com.mike.rmpfinder

import android.app.Application
import com.mike.rmpfinder.data.RmpDatabase
import com.mike.rmpfinder.data.RmpRepository
import com.mike.rmpfinder.update.UpdateScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RmpFinderApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: RmpDatabase by lazy { RmpDatabase.create(this) }
    val repository: RmpRepository by lazy { RmpRepository(database, assets) }

    override fun onCreate() {
        super.onCreate()
        UpdateScheduler.schedule(this)
        appScope.launch {
            repository.ensureBundledData()
            if (repository.shouldRunAppStartCheck()) repository.checkForUpdates()
        }
    }
}
