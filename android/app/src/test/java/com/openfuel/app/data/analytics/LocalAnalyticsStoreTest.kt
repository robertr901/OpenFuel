package com.openfuel.app.data.analytics

import com.openfuel.app.domain.analytics.ProductEventName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAnalyticsStoreTest {
    @Test
    fun track_acceptsOnlySchemaCompliantEventProperties() {
        val store = LocalAnalyticsStore(nowEpochMillis = { 1_000L })

        store.track(
            ProductEventName.LOGGING_COMPLETED,
            mapOf(
                "screen" to "add_food",
                "surface" to "search",
                "source_type" to "quick_add",
                "result" to "success",
                "latency_bucket_ms" to "lt_1s",
                "session_index" to "0",
            ),
        )

        assertEquals(1, store.events.value.size)
        assertEquals(ProductEventName.LOGGING_COMPLETED, store.events.value.first().name)
    }

    @Test
    fun track_rejectsSensitiveOrUnknownFields() {
        val store = LocalAnalyticsStore()

        store.track(
            ProductEventName.LOGGING_STARTED,
            mapOf(
                "screen" to "add_food",
                "query" to "banana",
            ),
        )
        store.track(
            ProductEventName.LOGGING_STARTED,
            mapOf(
                "screen" to "add_food",
                "custom_key" to "value",
            ),
        )

        assertTrue(store.events.value.isEmpty())
    }

    @Test
    fun track_rejectsInvalidEnumLikeValues() {
        val store = LocalAnalyticsStore()

        store.track(
            ProductEventName.RETENTION_FASTLOG_REMINDER_SHOWN,
            mapOf(
                "screen" to "today",
                "surface" to "home_card",
                "reminder_state" to "unexpected",
            ),
        )

        assertTrue(store.events.value.isEmpty())
    }
}

