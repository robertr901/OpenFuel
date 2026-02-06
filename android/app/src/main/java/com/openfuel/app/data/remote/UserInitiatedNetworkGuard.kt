package com.openfuel.app.data.remote

import java.time.Clock
import java.time.Duration
import java.time.Instant

class UserInitiatedNetworkGuard(
    private val clock: Clock = Clock.systemUTC(),
    private val tokenValidityWindow: Duration = Duration.ofMinutes(1),
) {
    fun issueToken(action: String): UserInitiatedNetworkToken {
        require(action.isNotBlank()) { "Action label is required." }
        return UserInitiatedNetworkToken(
            action = action,
            issuedAt = clock.instant(),
        )
    }

    fun validate(token: UserInitiatedNetworkToken) {
        val age = Duration.between(token.issuedAt, clock.instant())
        check(!age.isNegative && age <= tokenValidityWindow) {
            "Network call must be triggered by a recent explicit user action."
        }
    }
}

data class UserInitiatedNetworkToken internal constructor(
    val action: String,
    val issuedAt: Instant,
)
