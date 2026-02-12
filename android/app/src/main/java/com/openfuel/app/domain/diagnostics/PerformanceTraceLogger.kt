package com.openfuel.app.domain.diagnostics

import com.openfuel.app.BuildConfig

interface PerformanceTraceLogger {
    fun record(
        section: String,
        durationMs: Long,
        result: String,
    )
}

object NoOpPerformanceTraceLogger : PerformanceTraceLogger {
    override fun record(
        section: String,
        durationMs: Long,
        result: String,
    ) = Unit
}

class LocalDebugPerformanceTraceLogger(
    private val debugEnabled: Boolean = BuildConfig.DEBUG,
    private val sink: (String) -> Unit = ::println,
) : PerformanceTraceLogger {
    override fun record(
        section: String,
        durationMs: Long,
        result: String,
    ) {
        if (!debugEnabled) return
        val sanitizedDurationMs = durationMs.coerceAtLeast(0L)
        sink(
            "OPENFUEL_PERF section=$section duration_ms=$sanitizedDurationMs result=$result",
        )
    }
}
