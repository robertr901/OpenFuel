package com.openfuel.app.data.remote

import com.openfuel.app.domain.model.RemoteFoodCandidate
import com.openfuel.app.domain.service.FoodCatalogProvider

class UsdaFoodDataCentralCatalogProvider(
    private val dataSource: UsdaFoodDataSource,
) : FoodCatalogProvider {
    override suspend fun search(
        query: String,
        token: UserInitiatedNetworkToken,
    ): List<RemoteFoodCandidate> {
        return dataSource.searchByText(
            query = query,
            token = token,
        )
    }

    override suspend fun lookupBarcode(
        barcode: String,
        token: UserInitiatedNetworkToken,
    ): RemoteFoodCandidate? {
        return dataSource.lookupByBarcode(
            barcode = barcode,
            token = token,
        )
    }
}
