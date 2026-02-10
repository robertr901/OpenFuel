package com.openfuel.app.domain.search

import com.openfuel.sharedcore.normalization.buildNormalizedSqlLikePattern as coreBuildNormalizedSqlLikePattern
import com.openfuel.sharedcore.normalization.normalizeSearchQuery as coreNormalizeSearchQuery

fun normalizeSearchQuery(input: String): String {
    return coreNormalizeSearchQuery(input)
}

fun buildNormalizedSqlLikePattern(normalizedQuery: String): String {
    return coreBuildNormalizedSqlLikePattern(normalizedQuery)
}
