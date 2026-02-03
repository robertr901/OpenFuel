package com.openfuel.app.data.db

import androidx.room.TypeConverter
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun mealTypeToString(value: MealType?): String? = value?.name

    @TypeConverter
    fun stringToMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun foodUnitToString(value: FoodUnit?): String? = value?.name

    @TypeConverter
    fun stringToFoodUnit(value: String?): FoodUnit? = value?.let { FoodUnit.valueOf(it) }
}
