package com.openfuel.app.viewmodel

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
}
