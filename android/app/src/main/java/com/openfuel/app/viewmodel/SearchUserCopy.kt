package com.openfuel.app.viewmodel

import com.openfuel.app.domain.search.OnlineCandidateCompleteness
import com.openfuel.app.domain.search.OnlineCandidateSelectionReason

internal object SearchUserCopy {
    const val ONLINE_SEARCH_DISABLED =
        "Online search is turned off. Enable it in Settings to continue."

    const val ONLINE_SEARCH_QUERY_REQUIRED = "Enter a search term to look up online."

    const val ONLINE_SEARCH_FAILED_GENERIC = "Online search failed. Check connection and try again."

    const val ONLINE_SOURCE_NEEDS_SETUP_SINGLE = "Source needs setup. See statuses below."
    const val ONLINE_SOURCE_NEEDS_SETUP_MULTIPLE = "Some sources need setup. See statuses below."

    const val ONLINE_SOURCE_FAILED_SINGLE = "A source failed. See statuses below."
    const val ONLINE_SOURCE_FAILED_MULTIPLE = "Some sources failed. See statuses below."

    const val BARCODE_LOOKUP_DISABLED =
        "Online search is turned off. Enable it in Settings to use barcode lookup."

    const val BARCODE_NO_MATCH = "No matching food found for barcode."
    const val BARCODE_PARTIAL_RESULT = "Some providers were unavailable. Showing available match."
    const val BARCODE_USDA_SETUP_REQUIRED =
        "USDA provider is not configured. Add USDA_API_KEY in local.properties."

    const val NO_CONNECTION = "No connection."
    const val SERVICE_ERROR = "Service error."
    const val TIMEOUT = "Timed out (check connection)."
    const val LOOKUP_FAILED_RETRY = "Lookup failed. Check connection and retry."

    const val ONLINE_SOURCE_LABEL_PREFIX = "Source"
    const val ONLINE_COMPLETENESS_LABEL_PREFIX = "Completeness"
    const val ONLINE_REVIEW_REQUIRED = "Needs review"
    const val ONLINE_REVIEW_REQUIRED_HINT = "Serving details may be unclear. Review before logging."

    fun completenessLabel(completeness: OnlineCandidateCompleteness): String {
        return when (completeness) {
            OnlineCandidateCompleteness.COMPLETE -> "Complete"
            OnlineCandidateCompleteness.PARTIAL -> "Partial"
            OnlineCandidateCompleteness.LIMITED -> "Limited"
        }
    }

    fun whyThisResult(selectionReason: OnlineCandidateSelectionReason): String {
        return when (selectionReason) {
            OnlineCandidateSelectionReason.SINGLE_SOURCE_RESULT ->
                "Why this result: only source match."
            OnlineCandidateSelectionReason.BARCODE_MATCH ->
                "Why this result: exact barcode match."
            OnlineCandidateSelectionReason.BEST_MATCH_ACROSS_SOURCES ->
                "Why this result: best match across sources."
            OnlineCandidateSelectionReason.MOST_COMPLETE_NUTRITION ->
                "Why this result: most complete nutrition data."
            OnlineCandidateSelectionReason.PREFERRED_SOURCE ->
                "Why this result: preferred source in a tie."
            OnlineCandidateSelectionReason.DETERMINISTIC_TIE_BREAK ->
                "Why this result: deterministic tie-break."
        }
    }
}
