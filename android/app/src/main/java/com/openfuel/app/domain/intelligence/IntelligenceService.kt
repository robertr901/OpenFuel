package com.openfuel.app.domain.intelligence

interface IntelligenceService {
    /**
     * Deterministically parses free-form food text into structured preview items.
     * Implementations must be non-throwing and return explicit warnings when input is ambiguous.
     */
    fun parseFoodText(input: String): FoodTextIntent

    /**
     * Produces a stable query-safe text form for UI search fields.
     * Implementations must be non-throwing and deterministic for the same input.
     */
    fun normaliseSearchQuery(input: String): String
}
