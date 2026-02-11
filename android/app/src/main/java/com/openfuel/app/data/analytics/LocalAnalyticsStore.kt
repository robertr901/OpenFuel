package com.openfuel.app.data.analytics

import com.openfuel.app.domain.analytics.AnalyticsService
import com.openfuel.app.domain.analytics.ProductEvent
import com.openfuel.app.domain.analytics.ProductEventName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only product analytics store.
 *
 * This store is in-memory and does not upload, persist, or expose user-entered content.
 */
class LocalAnalyticsStore(
    private val nowEpochMillis: () -> Long = { System.currentTimeMillis() },
) : AnalyticsService {
    private val _events = MutableStateFlow<List<ProductEvent>>(emptyList())
    override val events: StateFlow<List<ProductEvent>> = _events.asStateFlow()

    override fun track(
        name: ProductEventName,
        properties: Map<String, String>,
    ) {
        val sanitized = sanitizeProperties(properties) ?: return
        _events.value = (_events.value + ProductEvent(
            name = name,
            properties = sanitized,
            occurredAtEpochMs = nowEpochMillis(),
        )).takeLast(MAX_EVENTS)
    }

    private fun sanitizeProperties(
        properties: Map<String, String>,
    ): Map<String, String>? {
        if (properties.isEmpty()) return emptyMap()
        if (!properties.keys.all { it in ALLOWED_KEYS }) return null
        if (properties.keys.any { it in SENSITIVE_KEYS }) return null
        if (!properties.all { (key, value) -> isValidValue(key, value) }) return null
        return properties
    }

    private fun isValidValue(key: String, value: String): Boolean {
        return when (key) {
            "screen" -> value in SCREENS
            "surface" -> value in SURFACES
            "source_type" -> value in SOURCE_TYPES
            "meal_type" -> value in MEAL_TYPES
            "result" -> value in RESULTS
            "latency_bucket_ms" -> value in LATENCY_BUCKETS
            "count_bucket" -> value in COUNT_BUCKETS
            "session_index" -> value.toIntOrNull()?.let { it >= 0 } == true
            "is_pro" -> value == "true" || value == "false"
            "flag_variant" -> value in FLAG_VARIANTS
            "reminder_state" -> value in REMINDER_STATES
            "quiet_hours_enabled" -> value == "true" || value == "false"
            else -> false
        }
    }

    private companion object {
        private const val MAX_EVENTS = 500

        private val ALLOWED_KEYS = setOf(
            "screen",
            "surface",
            "source_type",
            "meal_type",
            "result",
            "latency_bucket_ms",
            "count_bucket",
            "session_index",
            "is_pro",
            "flag_variant",
            "reminder_state",
            "quiet_hours_enabled",
        )

        private val SENSITIVE_KEYS = setOf(
            "query",
            "food_name",
            "brand",
            "barcode",
            "notes",
            "error_details",
            "provider_raw_message",
            "export_filename",
            "export_content",
        )

        private val SCREENS = setOf("today", "add_food", "settings", "insights", "paywall")
        private val SURFACES = setOf(
            "home_card",
            "quick_add",
            "search",
            "online",
            "paywall",
            "settings",
            "insights",
        )
        private val SOURCE_TYPES = setOf(
            "local_search",
            "recent",
            "favourite",
            "quick_add",
            "barcode",
            "online_saved",
        )
        private val MEAL_TYPES = setOf("breakfast", "lunch", "dinner", "snack")
        private val RESULTS = setOf("success", "cancelled", "error")
        private val LATENCY_BUCKETS = setOf("lt_1s", "1_to_5s", "gt_5s")
        private val COUNT_BUCKETS = setOf("0", "1", "2", "3_plus")
        private val FLAG_VARIANTS = setOf("control", "fast_log_v1")
        private val REMINDER_STATES = setOf("shown", "dismissed", "acted")
    }
}
