package com.openfuel.app.data.mappers

import com.openfuel.app.data.db.DailyGoalEntity
import com.openfuel.app.data.db.FoodItemEntity
import com.openfuel.app.data.db.MealEntryEntity
import com.openfuel.app.data.db.MealEntryWithFoodEntity
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood

fun FoodItemEntity.toDomain(): FoodItem = FoodItem(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    caloriesKcal = caloriesKcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    isFavorite = isFavorite,
    isReportedIncorrect = isReportedIncorrect,
    createdAt = createdAt,
)

fun FoodItem.toEntity(): FoodItemEntity = FoodItemEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    caloriesKcal = caloriesKcal,
    proteinG = proteinG,
    carbsG = carbsG,
    fatG = fatG,
    isFavorite = isFavorite,
    isReportedIncorrect = isReportedIncorrect,
    createdAt = createdAt,
)

fun MealEntryEntity.toDomain(): MealEntry = MealEntry(
    id = id,
    timestamp = timestamp,
    mealType = mealType,
    foodItemId = foodItemId,
    quantity = quantity,
    unit = unit,
)

fun MealEntry.toEntity(): MealEntryEntity = MealEntryEntity(
    id = id,
    timestamp = timestamp,
    mealType = mealType,
    foodItemId = foodItemId,
    quantity = quantity,
    unit = unit,
)

fun MealEntryWithFoodEntity.toDomain(): MealEntryWithFood = MealEntryWithFood(
    entry = entry.toDomain(),
    food = food.toDomain(),
)

fun DailyGoalEntity.toDomain(): DailyGoal = DailyGoal(
    date = date,
    caloriesKcalTarget = caloriesKcalTarget,
    proteinGTarget = proteinGTarget,
    carbsGTarget = carbsGTarget,
    fatGTarget = fatGTarget,
)

fun DailyGoal.toEntity(): DailyGoalEntity = DailyGoalEntity(
    date = date,
    caloriesKcalTarget = caloriesKcalTarget,
    proteinGTarget = proteinGTarget,
    carbsGTarget = carbsGTarget,
    fatGTarget = fatGTarget,
)
