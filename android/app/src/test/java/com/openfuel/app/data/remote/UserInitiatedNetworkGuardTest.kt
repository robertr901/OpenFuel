package com.openfuel.app.data.remote

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UserInitiatedNetworkGuardTest {
    @Test
    fun issueToken_createsTokenWithAction() {
        val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        val guard = UserInitiatedNetworkGuard(clock = clock)

        val token = guard.issueToken("search-online")

        assertEquals("search-online", token.action)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), token.issuedAt)
    }

    @Test
    fun validate_rejectsExpiredToken() {
        val clock = MutableClock(Instant.parse("2024-01-01T00:00:00Z"))
        val guard = UserInitiatedNetworkGuard(
            clock = clock,
            tokenValidityWindow = Duration.ofSeconds(10),
        )
        val token = guard.issueToken("barcode-lookup")
        clock.currentInstant = Instant.parse("2024-01-01T00:00:11Z")

        assertThrows(IllegalStateException::class.java) {
            guard.validate(token)
        }
    }
}

private class MutableClock(
    var currentInstant: Instant,
) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = currentInstant
}
