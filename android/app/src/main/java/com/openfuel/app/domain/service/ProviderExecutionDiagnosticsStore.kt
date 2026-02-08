package com.openfuel.app.domain.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProviderExecutionSnapshot(
    val report: ProviderExecutionReport,
)

interface ProviderExecutionDiagnosticsStore {
    val latestExecution: StateFlow<ProviderExecutionSnapshot?>

    fun record(report: ProviderExecutionReport)
}

class InMemoryProviderExecutionDiagnosticsStore : ProviderExecutionDiagnosticsStore {
    private val latest = MutableStateFlow<ProviderExecutionSnapshot?>(null)

    override val latestExecution: StateFlow<ProviderExecutionSnapshot?> = latest.asStateFlow()

    override fun record(report: ProviderExecutionReport) {
        latest.value = ProviderExecutionSnapshot(report = report)
    }
}
