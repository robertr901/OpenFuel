package com.openfuel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfuel.app.domain.model.FoodItem
import com.openfuel.app.domain.repository.FoodRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FoodLibraryViewModel(
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<FoodLibraryUiState> = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            foodRepository.allFoods(query)
                .map { foods ->
                    FoodLibraryUiState(
                        searchQuery = query,
                        foods = foods,
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodLibraryUiState(
                searchQuery = "",
                foods = emptyList(),
            ),
        )

    fun updateSearchQuery(query: String) {
        searchQuery.update { query }
    }
}

data class FoodLibraryUiState(
    val searchQuery: String,
    val foods: List<FoodItem>,
)
