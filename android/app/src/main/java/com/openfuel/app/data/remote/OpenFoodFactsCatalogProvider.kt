package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.service.FoodCatalogProvider

class OpenFoodFactsCatalogProvider(
    private val remoteFoodDataSource: RemoteFoodDataSource,
) : FoodCatalogProvider {
    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        return remoteFoodDataSource.searchByText(query = query, token = token)
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return remoteFoodDataSource.lookupByBarcode(barcode = barcode, token = token)
    }
}
