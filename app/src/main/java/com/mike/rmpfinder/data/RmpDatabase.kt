package com.mike.rmpfinder.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "restaurants")
data class RestaurantEntity(
    @PrimaryKey val rmpKey: String,
    val officialName: String,
    val currentName: String?,
    val borough: String,
    val zip: String,
    val officialAddress: String,
    val currentAddress: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val website: String?,
    val menuUrl: String?,
    val imageUrl: String?,
    val businessStatus: String,
    val hoursStatus: String,
    val rmpVerifiedAt: String,
    val businessCheckedAt: String?,
    val hasConflict: Boolean,
    val searchText: String,
    val rawJson: String,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(@PrimaryKey val rmpKey: String)

@Entity(tableName = "metadata")
data class MetadataEntity(@PrimaryKey val key: String, val value: String)

@Dao
interface RmpDao {
    @Query("SELECT * FROM restaurants")
    fun observeRestaurants(): Flow<List<RestaurantEntity>>

    @Query("SELECT COUNT(*) FROM restaurants")
    suspend fun restaurantCount(): Int

    @Query("SELECT rmpKey FROM restaurants")
    suspend fun restaurantKeys(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestaurants(restaurants: List<RestaurantEntity>)

    @Query("DELETE FROM restaurants")
    suspend fun deleteRestaurants()

    @Query("SELECT rmpKey FROM favorites")
    fun observeFavoriteKeys(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE rmpKey = :rmpKey")
    suspend fun removeFavorite(rmpKey: String)

    @Query("SELECT * FROM metadata")
    fun observeMetadata(): Flow<List<MetadataEntity>>

    @Query("SELECT value FROM metadata WHERE `key` = :key LIMIT 1")
    suspend fun metadataValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMetadata(value: MetadataEntity)
}

@Database(
    entities = [RestaurantEntity::class, FavoriteEntity::class, MetadataEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RmpDatabase : RoomDatabase() {
    abstract fun dao(): RmpDao

    companion object {
        fun create(context: Context): RmpDatabase = Room.databaseBuilder(
            context.applicationContext,
            RmpDatabase::class.java,
            "rmp-finder.db",
        ).build()
    }
}
