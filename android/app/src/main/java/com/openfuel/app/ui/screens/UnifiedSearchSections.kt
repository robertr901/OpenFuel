package com.openfuel.app.ui.screens

import com.openfuel.app.domain.search.SearchSourceFilter

internal enum class UnifiedSearchSectionType {
    LOCAL,
    ONLINE,
}

internal data class UnifiedSearchSection(
    val type: UnifiedSearchSectionType,
    val title: String,
    val subtitle: String,
    val headerTestTag: String,
)

internal fun buildUnifiedSearchSections(sourceFilter: SearchSourceFilter): List<UnifiedSearchSection> {
    val sections = mutableListOf<UnifiedSearchSection>()
    if (sourceFilter != SearchSourceFilter.ONLINE_ONLY) {
        sections += UnifiedSearchSection(
            type = UnifiedSearchSectionType.LOCAL,
            title = "Local results",
            subtitle = "Instant matches from foods already on this device.",
            headerTestTag = "add_food_unified_local_section",
        )
    }
    if (sourceFilter != SearchSourceFilter.LOCAL_ONLY) {
        sections += UnifiedSearchSection(
            type = UnifiedSearchSectionType.ONLINE,
            title = "Online results",
            subtitle = "Fetched from enabled online catalogs when you tap Search online.",
            headerTestTag = "add_food_unified_online_section",
        )
    }
    return sections
}
