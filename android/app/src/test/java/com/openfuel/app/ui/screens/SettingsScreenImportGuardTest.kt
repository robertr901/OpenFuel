package com.openfuel.app.ui.screens

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenImportGuardTest {

    @Test
    fun settingsScreen_usesSingleMaterial3ExperimentalImport() {
        val sourceFile = resolveSettingsScreenPath()
        val lines = Files.readAllLines(sourceFile)

        val allExperimentalImports = lines.count { line ->
            val trimmed = line.trim()
            trimmed.startsWith("import ") && trimmed.contains("ExperimentalMaterial3Api")
        }
        val material3Imports = lines.count { line ->
            line.trim() == "import androidx.compose.material3.ExperimentalMaterial3Api"
        }
        val material2Imports = lines.count { line ->
            line.trim() == "import androidx.compose.material.ExperimentalMaterial3Api"
        }

        assertEquals(
            "SettingsScreen.kt must contain exactly one ExperimentalMaterial3Api import.",
            1,
            allExperimentalImports,
        )
        assertEquals(
            "SettingsScreen.kt must import ExperimentalMaterial3Api from Material3.",
            1,
            material3Imports,
        )
        assertEquals(
            "SettingsScreen.kt must not import ExperimentalMaterial3Api from Material2.",
            0,
            material2Imports,
        )
    }

    private fun resolveSettingsScreenPath(): Path {
        val candidates = listOf(
            Path.of("app", "src", "main", "java", "com", "openfuel", "app", "ui", "screens", "SettingsScreen.kt"),
            Path.of("src", "main", "java", "com", "openfuel", "app", "ui", "screens", "SettingsScreen.kt"),
        )
        val existing = candidates.firstOrNull { Files.exists(it) }
        assertTrue("Unable to locate SettingsScreen.kt for import guard.", existing != null)
        return existing!!
    }
}
