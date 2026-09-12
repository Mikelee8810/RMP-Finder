package com.mike.rmpfinder.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryAcceptanceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun invalidDownloadedDatasetCannotReplaceInstalledDirectory() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RmpDatabase::class.java).build()
        try {
            val repository = RmpRepository(database, context.assets)
            repository.ensureBundledData()
            assertEquals(241, database.dao().restaurantCount())
            val beforeKeys = database.dao().restaurantKeys().toSet()
            val manifest = context.assets.open("manifest.json").use { it.readBytes() }
            val corrupted = context.assets.open("restaurants.json").use { it.readBytes() } + byteArrayOf('\n'.code.toByte())

            val failure = runCatching { repository.installDownloadedUpdate(manifest, corrupted) }.exceptionOrNull()

            assertTrue(failure?.message?.contains("checksum") == true)
            assertEquals(241, database.dao().restaurantCount())
            assertEquals(beforeKeys, database.dao().restaurantKeys().toSet())
        } finally {
            database.close()
        }
    }

    @Test
    fun newerBundledDatasetReplacesAnOlderDirectoryAndKeepsFavorites() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, RmpDatabase::class.java).build()
        try {
            val repository = RmpRepository(database, context.assets)
            repository.ensureBundledData()
            val savedKey = database.dao().restaurantKeys().first()
            database.dao().addFavorite(FavoriteEntity(savedKey))
            database.dao().putMetadata(MetadataEntity(RmpRepository.KEY_DATASET_VERSION, "1"))

            repository.ensureBundledData()

            assertEquals(241, database.dao().restaurantCount())
            assertEquals("2", database.dao().metadataValue(RmpRepository.KEY_DATASET_VERSION))
            assertTrue(savedKey in database.dao().observeFavoriteKeys().first())
        } finally {
            database.close()
        }
    }
}
