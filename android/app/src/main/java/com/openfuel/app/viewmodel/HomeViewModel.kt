package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.util.MealTotalsCalculator
import com.openfuel.app.domain.util.UnitConversion
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val logRepository: LogRepository,
) : ViewModel() {
    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = logRepository.entriesForDate(today)
        .map { entries -> buildUiState(entries) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                date = today,
                totals = MacroTotals.Zero,
                meals = MealType.values().map { mealType ->
                    MealSectionUi(mealType = mealType, entries = emptyList(), totals = MacroTotals.Zero)
                },
            ),
        )

    private fun buildUiState(entries: List<MealEntryWithFood>): HomeUiState {
        val totals = MealTotalsCalculator.totalsFor(entries).totals
        val grouped = entries.groupBy { it.entry.mealType }
        val meals = MealType.values().map { mealType ->
            val mealEntries = grouped[mealType].orEmpty()
            val mealTotals = MealTotalsCalculator.totalsFor(mealEntries).totals
            MealSectionUi(
                mealType = mealType,
                entries = mealEntries.map { entryWithFood ->
                    val macros = UnitConversion.scaleMacros(
                        foodItem = entryWithFood.food,
                        quantity = entryWithFood.entry.quantity,
                        unit = entryWithFood.entry.unit,
                    )
                    MealEntryUi(
                        id = entryWithFood.entry.id,
                        foodId = entryWithFood.entry.foodItemId,
                        name = entryWithFood.food.name,
                        quantity = entryWithFood.entry.quantity,
                        unit = entryWithFood.entry.unit,
                        caloriesKcal = macros.caloriesKcal,
                    )
                },
                totals = mealTotals,
            )
        }
        return HomeUiState(
            date = today,
            totals = totals,
            meals = meals,
        )
    }
}

data class HomeUiState(
    val date: LocalDate,
    val totals: MacroTotals,
    val meals: List<MealSectionUi>,
)

data class MealSectionUi(
    val mealType: MealType,
    val entries: List<MealEntryUi>,
    val totals: MacroTotals,
)

data class MealEntryUi(
    val id: String,
    val foodId: String,
    val name: String,
    val quantity: Double,
    val unit: FoodUnit,
    val caloriesKcal: Double,
)
