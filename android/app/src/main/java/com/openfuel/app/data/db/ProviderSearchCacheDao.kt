package com.openfuel.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProviderSearchCacheDao {
    @Query("SELECT * FROM provider_search_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun getByKey(cacheKey: String): ProviderSearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ProviderSearchCacheEntity)

    @Query("DELETE FROM provider_search_cache WHERE cacheKey = :cacheKey")
    suspend fun deleteByKey(cacheKey: String)

    @Query("DELETE FROM provider_search_cache WHERE expiresAtEpochMs <= :nowEpochMs")
    suspend fun deleteExpired(nowEpochMs: Long): Int

    @Query("DELETE FROM provider_search_cache WHERE cacheVersion != :expectedVersion")
    suspend fun deleteByVersionMismatch(expectedVersion: Int): Int

    @Query("SELECT COUNT(*) FROM provider_search_cache")
    suspend fun countAll(): Int
}
