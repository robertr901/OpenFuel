package com.openfuel.app.docs

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentationConsistencyTest {
    private val repoRoot: Path = resolveRepoRoot()

    @Test
    fun onlineLookupDefaultClaim_matchesRuntimeDefault() {
        val settingsRepositorySource = readRepoFile(
            "android/app/src/main/java/com/openfuel/app/data/repository/SettingsRepositoryImpl.kt",
        )
        val runtimeDefault = Regex("ONLINE_LOOKUP_ENABLED\\]\\s*\\?:\\s*(true|false)")
            .find(settingsRepositorySource)
            ?.groupValues
            ?.get(1)
            ?: error("Could not determine online lookup default from SettingsRepositoryImpl.")

        val securityDoc = readRepoFile("SECURITY.md")
        val documentedDefault = Regex("Online lookup default is currently `enabled = (true|false)`")
            .find(securityDoc)
            ?.groupValues
            ?.get(1)
            ?: error("SECURITY.md must document online lookup default as `enabled = true|false`.")

        assertEquals(
            "SECURITY.md online lookup default must match SettingsRepositoryImpl runtime default.",
            runtimeDefault,
            documentedDefault,
        )
    }

    @Test
    fun explicitActionNetworkingClaim_isPresentInCoreDocs() {
        val architectureDoc = readRepoFile("docs/architecture.md")
        val threatModelDoc = readRepoFile("docs/threat-model.md")
        val securityDoc = readRepoFile("SECURITY.md")

        assertTrue(
            "docs/architecture.md must state explicit user-action token guardrails for online lookups.",
            architectureDoc.contains("require explicit user action tokens"),
        )
        assertTrue(
            "docs/threat-model.md must state explicit user-action online lookups.",
            threatModelDoc.contains("All online lookups must be initiated by explicit user action"),
        )
        assertTrue(
            "SECURITY.md must state explicit-action-only online lookups.",
            securityDoc.contains("Online lookups are explicit-action only and guarded."),
        )
    }

    private fun readRepoFile(relativePath: String): String {
        return Files.readString(repoRoot.resolve(relativePath))
    }

    private fun resolveRepoRoot(): Path {
        var current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        repeat(10) {
            val hasReadme = Files.exists(current.resolve("README.md"))
            val hasDocsDir = Files.isDirectory(current.resolve("docs"))
            if (hasReadme && hasDocsDir) {
                return current
            }
            val parent = current.parent ?: return@repeat
            current = parent
        }
        error("Unable to locate repository root from ${System.getProperty("user.dir")}.")
    }
}
