package com.openfuel.app.data.repository

import com.openfuel.app.data.db.FoodDao
import com.openfuel.app.data.mappers.toDomain
import com.openfuel.app.data.mappers.toEntity
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.repository.FoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodRepositoryImpl(
    private val foodDao: FoodDao,
) : FoodRepository {
    override suspend fun upsertFood(foodItem: FoodItem) {
        val normalizedBarcode = foodItem.barcode?.trim()?.takeIf { it.isNotBlank() }
        val existing = normalizedBarcode?.let { barcode ->
            foodDao.getFoodByBarcode(barcode)
        }
        val mergedFood = if (existing == null) {
            foodItem.copy(barcode = normalizedBarcode)
        } else {
            foodItem.copy(
                id = existing.id,
                barcode = existing.barcode,
                isFavorite = existing.isFavorite,
                isReportedIncorrect = existing.isReportedIncorrect,
                createdAt = existing.createdAt,
            )
        }
        foodDao.upsertFood(mergedFood.toEntity())
    }

    override suspend fun getFoodById(id: String): FoodItem? {
        return foodDao.getFoodById(id)?.toDomain()
    }

    override suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) {
            return null
        }
        return foodDao.getFoodByBarcode(normalizedBarcode)?.toDomain()
    }

    override suspend fun setFavorite(foodId: String, isFavorite: Boolean) {
        foodDao.updateFavorite(id = foodId, isFavorite = isFavorite)
    }

    override suspend fun setReportedIncorrect(foodId: String, isReportedIncorrect: Boolean) {
        foodDao.updateReportedIncorrect(
            id = foodId,
            isReportedIncorrect = isReportedIncorrect,
        )
    }

    override fun favoriteFoods(limit: Int): Flow<List<FoodItem>> {
        return foodDao.observeFavoriteFoods(limit)
            .map { foods -> foods.map { it.toDomain() } }
    }

    override fun recentLoggedFoods(limit: Int): Flow<List<FoodItem>> {
        return foodDao.observeRecentLoggedFoods(limit)
            .map { foods -> foods.map { it.toDomain() } }
    }

    override fun allFoods(query: String): Flow<List<FoodItem>> {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isBlank()) {
            foodDao.observeAllFoods()
        } else {
            foodDao.observeAllFoodsBySearch(escapeLikeQuery(trimmedQuery))
        }.map { foods -> foods.map { it.toDomain() } }
    }

    override fun recentFoods(limit: Int): Flow<List<FoodItem>> {
        return foodDao.observeRecentFoods(limit)
            .map { foods -> foods.map { it.toDomain() } }
    }

    override fun searchFoods(query: String, limit: Int): Flow<List<FoodItem>> {
        val trimmedQuery = query.trim()
        return if (trimmedQuery.isBlank()) {
            foodDao.observeRecentFoods(limit)
        } else {
            foodDao.observeFoodsBySearch(escapeLikeQuery(trimmedQuery), limit)
        }.map { foods -> foods.map { it.toDomain() } }
    }
}

internal fun escapeLikeQuery(query: String): String {
    val escaped = StringBuilder(query.length + 8)
    query.forEach { char ->
        when (char) {
            '%', '_', '\\' -> {
                escaped.append('\\')
                escaped.append(char)
            }
            else -> escaped.append(char)
        }
    }
    return escaped.toString()
}
