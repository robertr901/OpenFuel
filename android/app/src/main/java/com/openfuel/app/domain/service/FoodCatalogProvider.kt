package com.openfuel.app.domain.service

import com.openfuel.app.data.remote.UserInitiatedNetworkToken
import com.openfuel.app.domain.model.RemoteFoodCandidate

interface FoodCatalogProvider {
    suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate>

    suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate?
}
