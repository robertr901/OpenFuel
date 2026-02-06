package com.openfuel.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.openfuel.app.domain.model.FoodUnit
import com.openfuel.app.domain.model.MealType
import com.openfuel.app.domain.model.displayName

@Composable
fun MealTypeDropdown(
    selected: MealType,
    onSelected: (MealType) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownSelector(
        label = "Meal",
        selectedLabel = selected.displayName(),
        items = MealType.values().toList(),
        modifier = modifier,
        itemLabel = { it.displayName() },
        onSelected = onSelected,
    )
}

@Composable
fun UnitDropdown(
    selected: FoodUnit,
    onSelected: (FoodUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownSelector(
        label = "Unit",
        selectedLabel = selected.name.lowercase().replaceFirstChar { it.titlecase() },
        items = FoodUnit.values().toList(),
        modifier = modifier,
        itemLabel = { it.name.lowercase().replaceFirstChar { char -> char.titlecase() } },
        onSelected = onSelected,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T> DropdownSelector(
    label: String,
    selectedLabel: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
