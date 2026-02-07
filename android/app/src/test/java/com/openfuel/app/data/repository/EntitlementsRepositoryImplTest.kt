package com.openfuel.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class EntitlementsRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun isPro_defaultsToFalse() = runTest {
        val repository = createRepository(backgroundScope)

        assertFalse(repository.isPro.first())
    }

    @Test
    fun setIsPro_updatesStoredValue() = runTest {
        val repository = createRepository(backgroundScope)

        repository.setIsPro(true)
        assertTrue(repository.isPro.first())

        repository.setIsPro(false)
        assertFalse(repository.isPro.first())
    }

    private fun createRepository(scope: CoroutineScope): EntitlementsRepositoryImpl {
        val storageFile = tempFolder.newFile("entitlements.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { storageFile },
        )
        return EntitlementsRepositoryImpl(dataStore)
    }
}
