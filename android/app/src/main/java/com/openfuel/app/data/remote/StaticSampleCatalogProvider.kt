package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.model.RemoteFoodSource
import com.openfuel.app.domain.service.FoodCatalogProvider
import java.util.Locale

class StaticSampleCatalogProvider : FoodCatalogProvider {
    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        val normalized = normalize(query)
        return when {
            normalized.contains("oat") -> listOf(
                sampleCandidate(
                    sourceId = "sample-oatmeal-1",
                    barcode = "0001112223333",
                    name = "Sample Oatmeal",
                    brand = "OpenFuel Samples",
                    calories = 367.0,
                    protein = 13.0,
                    carbs = 67.0,
                    fat = 6.5,
                    servingSize = "40 g",
                ),
                sampleCandidate(
                    sourceId = "sample-oat-milk-2",
                    barcode = "0001112223334",
                    name = "Sample Oat Milk",
                    brand = "OpenFuel Samples",
                    calories = 46.0,
                    protein = 1.0,
                    carbs = 6.0,
                    fat = 1.5,
                    servingSize = "100 ml",
                ),
            )

            normalized.contains("banana") -> listOf(
                sampleCandidate(
                    sourceId = "sample-banana-1",
                    barcode = "0001112223335",
                    name = "Sample Banana",
                    brand = "OpenFuel Samples",
                    calories = 89.0,
                    protein = 1.1,
                    carbs = 22.8,
                    fat = 0.3,
                    servingSize = "100 g",
                ),
            )

            else -> emptyList()
        }
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return when (barcode.trim()) {
            "0001112223333" -> sampleCandidate(
                sourceId = "sample-oatmeal-1",
                barcode = "0001112223333",
                name = "Sample Oatmeal",
                brand = "OpenFuel Samples",
                calories = 367.0,
                protein = 13.0,
                carbs = 67.0,
                fat = 6.5,
                servingSize = "40 g",
            )

            "0001112223335" -> sampleCandidate(
                sourceId = "sample-banana-1",
                barcode = "0001112223335",
                name = "Sample Banana",
                brand = "OpenFuel Samples",
                calories = 89.0,
                protein = 1.1,
                carbs = 22.8,
                fat = 0.3,
                servingSize = "100 g",
            )

            else -> null
        }
    }

    private fun sampleCandidate(
        sourceId: String,
        barcode: String,
        name: String,
        brand: String,
        calories: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        servingSize: String,
    ): RemoteFoodCandidate {
        return RemoteFoodCandidate(
            source = RemoteFoodSource.STATIC_SAMPLE,
            sourceId = sourceId,
            barcode = barcode,
            name = name,
            brand = brand,
            caloriesKcalPer100g = calories,
            proteinGPer100g = protein,
            carbsGPer100g = carbs,
            fatGPer100g = fat,
            servingSize = servingSize,
        )
    }

    private fun normalize(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }
}
