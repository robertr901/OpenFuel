package com.openfuel.app.domain.intelligence

interface IntelligenceService {
    fun parseFoodText(input: String): FoodTextIntent

    fun normaliseSearchQuery(input: String): String
}
