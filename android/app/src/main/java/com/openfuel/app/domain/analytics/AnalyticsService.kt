package com.openfuel.app.domain.analytics

import kotlinx.coroutines.flow.StateFlow

interface AnalyticsService {
    fun track(
        name: ProductEventName,
        properties: Map<String, String> = emptyMap(),
    )

    val events: StateFlow<List<ProductEvent>>
}

object NoOpAnalyticsService : AnalyticsService {
    private val empty = kotlinx.coroutines.flow.MutableStateFlow<List<ProductEvent>>(emptyList())

    override fun track(
        name: ProductEventName,
        properties: Map<String, String>,
    ) = Unit

    override val events: StateFlow<List<ProductEvent>> = empty
}
