package com.openfuel.app.data.remote

data class ProviderAvailability(
    val enabled: Boolean,
    val statusReason: String,
)

fun resolveOpenFoodFactsAvailability(
    forceDeterministicProvidersOnly: Boolean,
    providerEnabledByFlag: Boolean,
): ProviderAvailability {
    if (forceDeterministicProvidersOnly) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled in deterministic test mode.",
        )
    }
    if (!providerEnabledByFlag) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled by local provider flag.",
        )
    }
    return ProviderAvailability(
        enabled = true,
        statusReason = "Configured.",
    )
}

fun resolveUsdaAvailability(
    forceDeterministicProvidersOnly: Boolean,
    providerEnabledByFlag: Boolean,
    apiKey: String,
): ProviderAvailability {
    if (!providerEnabledByFlag) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled by local provider flag.",
        )
    }
    if (apiKey.isBlank()) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "USDA API key missing. Add USDA_API_KEY in local.properties.",
        )
    }
    if (forceDeterministicProvidersOnly) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled in deterministic test mode.",
        )
    }
    return ProviderAvailability(
        enabled = true,
        statusReason = "Configured.",
    )
}

fun resolveNutritionixAvailability(
    forceDeterministicProvidersOnly: Boolean,
    providerEnabledByFlag: Boolean,
    appId: String,
    apiKey: String,
): ProviderAvailability {
    if (!providerEnabledByFlag) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled by local provider flag.",
        )
    }
    if (appId.isBlank() || apiKey.isBlank()) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Nutritionix credentials missing. Add NUTRITIONIX_APP_ID and NUTRITIONIX_API_KEY in local.properties.",
        )
    }
    if (forceDeterministicProvidersOnly) {
        return ProviderAvailability(
            enabled = false,
            statusReason = "Disabled in deterministic test mode.",
        )
    }
    return ProviderAvailability(
        enabled = true,
        statusReason = "Configured.",
    )
}
