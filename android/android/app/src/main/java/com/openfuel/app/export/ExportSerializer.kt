package com.openfuel.app.export

import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.model.MealEntry
import java.time.format.DateTimeFormatter

class ExportSerializer {
    private val instantFormatter = DateTimeFormatter.ISO_INSTANT
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun serialize(snapshot: ExportSnapshot): String {
        return buildString {
            append('{')
            appendKey("schemaVersion").append(snapshot.schemaVersion)
            append(',')
            appendKey("appVersion").appendJsonString(snapshot.appVersion)
            append(',')
            appendKey("exportedAt").appendJsonString(instantFormatter.format(snapshot.exportedAt))
            append(',')
            appendKey("foods")
            appendFoods(snapshot.foods)
            append(',')
            appendKey("mealEntries")
            appendMealEntries(snapshot.mealEntries)
            append(',')
            appendKey("dailyGoals")
            appendDailyGoals(snapshot.dailyGoals)
            append('}')
        }
    }

    private fun StringBuilder.appendFoods(foods: List<FoodItem>) {
        append('[')
        foods.forEachIndexed { index, food ->
            if (index > 0) append(',')
            append('{')
            appendKey("id").appendJsonString(food.id)
            append(',')
            appendKey("name").appendJsonString(food.name)
            append(',')
            appendKey("brand")
            if (food.brand == null) {
                append("null")
            } else {
                appendJsonString(food.brand)
            }
            append(',')
            appendKey("calories_kcal").appendNumber(food.caloriesKcal)
            append(',')
            appendKey("protein_g").appendNumber(food.proteinG)
            append(',')
            appendKey("carbs_g").appendNumber(food.carbsG)
            append(',')
            appendKey("fat_g").appendNumber(food.fatG)
            append(',')
            appendKey("createdAt").appendJsonString(instantFormatter.format(food.createdAt))
            append('}')
        }
        append(']')
    }

    private fun StringBuilder.appendMealEntries(entries: List<MealEntry>) {
        append('[')
        entries.forEachIndexed { index, entry ->
            if (index > 0) append(',')
            append('{')
            appendKey("id").appendJsonString(entry.id)
            append(',')
            appendKey("timestamp").appendJsonString(instantFormatter.format(entry.timestamp))
            append(',')
            appendKey("mealType").appendJsonString(entry.mealType.name)
            append(',')
            appendKey("foodItemId").appendJsonString(entry.foodItemId)
            append(',')
            appendKey("quantity").appendNumber(entry.quantity)
            append(',')
            appendKey("unit").appendJsonString(entry.unit.name)
            append('}')
        }
        append(']')
    }

    private fun StringBuilder.appendDailyGoals(goals: List<DailyGoal>) {
        append('[')
        goals.forEachIndexed { index, goal ->
            if (index > 0) append(',')
            append('{')
            appendKey("date").appendJsonString(dateFormatter.format(goal.date))
            append(',')
            appendKey("calories_kcal_target").appendNumber(goal.caloriesKcalTarget)
            append(',')
            appendKey("protein_g_target").appendNumber(goal.proteinGTarget)
            append(',')
            appendKey("carbs_g_target").appendNumber(goal.carbsGTarget)
            append(',')
            appendKey("fat_g_target").appendNumber(goal.fatGTarget)
            append('}')
        }
        append(']')
    }

    private fun StringBuilder.appendKey(key: String): StringBuilder {
        return append('"').append(escapeJson(key)).append("\":")
    }

    private fun StringBuilder.appendJsonString(value: String): StringBuilder {
        return append('"').append(escapeJson(value)).append('"')
    }

    private fun StringBuilder.appendNumber(value: Double): StringBuilder {
        val sanitized = if (value.isNaN() || value.isInfinite()) 0.0 else value
        return append(sanitized.toString())
    }

    private fun escapeJson(value: String): String {
        val escaped = StringBuilder(value.length + 16)
        value.forEach { ch ->
            when (ch) {
                '\\' -> escaped.append("\\\\")
                '"' -> escaped.append("\\\"")
                '\n' -> escaped.append("\\n")
                '\r' -> escaped.append("\\r")
                '\t' -> escaped.append("\\t")
                else -> escaped.append(ch)
            }
        }
        return escaped.toString()
    }
}
