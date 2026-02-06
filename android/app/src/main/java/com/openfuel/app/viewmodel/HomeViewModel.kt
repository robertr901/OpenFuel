package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MacroTotals
import com.openfuel.app.domain.model.DailyGoal
import com.openfuel.app.domain.model.MealEntry
import com.openfuel.app.domain.model.MealEntryWithFood
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.repository.GoalsRepository
import com.openfuel.app.domain.repository.LogRepository
import com.openfuel.app.domain.util.EntryValidation
import com.openfuel.app.domain.util.MealTotalsCalculator
import com.openfuel.app.domain.util.UnitConversion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val logRepository: LogRepository,
    private val goalsRepository: GoalsRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now(zoneId))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val uiState: StateFlow<HomeUiState> = selectedDate
        .flatMapLatest { date ->
            combine(
                logRepository.entriesForDate(date, zoneId),
                goalsRepository.goalForDate(date),
            ) { entries, goal ->
                buildUiState(date, entries, goal)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(
                date = _selectedDate.value,
                totals = MacroTotals.Zero,
                goal = null,
                meals = MealType.values().map { mealType ->
                    MealSectionUi(mealType = mealType, entries = emptyList(), totals = MacroTotals.Zero)
                },
            ),
        )

    fun goToPreviousDay() {
        _selectedDate.update { current -> current.minusDays(1) }
    }

    fun goToNextDay() {
        _selectedDate.update { current -> current.plusDays(1) }
    }

    fun updateEntry(
        entry: MealEntryUi,
        quantity: Double,
        unit: FoodUnit,
        mealType: MealType,
    ) {
        if (!EntryValidation.isValidQuantity(quantity)) {
            return
        }
        viewModelScope.launch {
            logRepository.updateMealEntry(
                MealEntry(
                    id = entry.id,
                    timestamp = entry.timestamp,
                    mealType = mealType,
                    foodItemId = entry.foodId,
                    quantity = quantity,
                    unit = unit,
                ),
            )
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            logRepository.deleteMealEntry(entryId)
        }
    }

    private fun buildUiState(
        date: LocalDate,
        entries: List<MealEntryWithFood>,
        goal: DailyGoal?,
    ): HomeUiState {
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
                        timestamp = entryWithFood.entry.timestamp,
                        mealType = entryWithFood.entry.mealType,
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
            date = date,
            totals = totals,
            goal = goal,
            meals = meals,
        )
    }
}

data class HomeUiState(
    val date: LocalDate,
    val totals: MacroTotals,
    val goal: DailyGoal?,
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
    val timestamp: Instant,
    val mealType: MealType,
    val name: String,
    val quantity: Double,
    val unit: FoodUnit,
    val caloriesKcal: Double,
)
