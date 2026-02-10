package com.openfuel.sharedcore.normalization

import com.openfuel.sharedcore.model.CoreRemoteFoodCandidate
import java.util.Locale

enum class CoreProviderRequestType {
    TEXT_SEARCH,
    BARCODE_LOOKUP,
}

fun buildProviderDedupeKey(candidate: CoreRemoteFoodCandidate): String {
    val barcode = normalizeProviderBarcode(candidate.barcode)
    if (barcode != null) {
        return "barcode:$barcode"
    }
    val name = normalizeProviderText(candidate.name)
    val brand = normalizeProviderText(candidate.brand.orEmpty())
    val servingSize = normalizeProviderText(candidate.servingSize.orEmpty())
    val sourceScopedIdentity = "source:${candidate.source.lowercase(Locale.ROOT)}|${normalizeProviderText(candidate.sourceId)}"
    if (name.isBlank()) {
        return sourceScopedIdentity
    }
    if (brand.isBlank() && servingSize.isBlank()) {
        return "$sourceScopedIdentity|$name"
    }
    return "text:$name|$brand|$servingSize"
}

fun buildProviderCacheKey(
    providerId: String,
    requestType: CoreProviderRequestType,
    rawInput: String,
): String {
    val normalizedProviderId = normalizeProviderText(providerId)
    val normalizedInput = when (requestType) {
        CoreProviderRequestType.TEXT_SEARCH -> normalizeProviderText(rawInput)
        CoreProviderRequestType.BARCODE_LOOKUP -> normalizeProviderBarcode(rawInput).orEmpty()
    }
    return "$normalizedProviderId|${requestType.name}|$normalizedInput"
}

fun normalizeProviderText(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace("\\s+".toRegex(), " ")
}

fun normalizeProviderBarcode(value: String?): String? {
    return value?.trim()?.takeIf { it.isNotEmpty() }
}
