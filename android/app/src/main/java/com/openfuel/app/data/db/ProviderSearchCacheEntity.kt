package com.openfuel.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "provider_search_cache",
    indices = [
        Index(value = ["expiresAtEpochMs"]),
        Index(value = ["providerId", "requestType"]),
    ],
)
data class ProviderSearchCacheEntity(
    @PrimaryKey val cacheKey: String,
    val providerId: String,
    val requestType: String,
    val normalizedInput: String,
    val cacheVersion: Int,
    val payloadJson: String,
    val cachedAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val itemCount: Int,
)
