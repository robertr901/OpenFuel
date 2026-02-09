package com.openfuel.app.export

import androidx.core.util.AtomicFile
import com.openfuel.app.data.db.FoodDao
import com.openfuel.app.data.db.MealEntryDao
import com.openfuel.app.data.mappers.toDomain
import com.openfuel.app.domain.repository.GoalsRepository
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ExportManager(
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val goalsRepository: GoalsRepository,
    private val serializer: ExportSerializer = ExportSerializer(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend fun export(cacheDir: File, appVersion: String): File = withContext(Dispatchers.IO) {
        val snapshot = buildSnapshot(
            appVersion = appVersion,
            redactionOptions = ExportRedactionOptions(redactBrand = false),
        )
        val json = serializer.serialize(snapshot)
        val exportFile = buildExportFile(
            cacheDir = cacheDir,
            baseName = "openfuel-export",
            extension = "json",
        )
        writeAtomic(exportFile, json)
        exportFile
    }

    suspend fun previewAdvancedExport(
        redactionOptions: ExportRedactionOptions,
    ): AdvancedExportPreview = withContext(Dispatchers.IO) {
        val sourceFoods = foodDao.getAllFoods().map { it.toDomain() }
        val snapshot = buildSnapshot(
            appVersion = "preview",
            redactionOptions = redactionOptions,
        )
        val redactedBrandCount = if (redactionOptions.redactBrand) {
            sourceFoods.count { food -> !food.brand.isNullOrBlank() }
        } else {
            0
        }
        AdvancedExportPreview(
            foodCount = snapshot.foods.size,
            mealEntryCount = snapshot.mealEntries.size,
            dailyGoalCount = snapshot.dailyGoals.size,
            redactedBrandCount = redactedBrandCount,
        )
    }

    suspend fun exportAdvanced(
        cacheDir: File,
        appVersion: String,
        format: ExportFormat,
        redactionOptions: ExportRedactionOptions,
    ): File = withContext(Dispatchers.IO) {
        val snapshot = buildSnapshot(
            appVersion = appVersion,
            redactionOptions = redactionOptions,
        )
        val suffix = if (redactionOptions.redactBrand) "-redacted" else ""
        when (format) {
            ExportFormat.JSON -> {
                val file = buildExportFile(
                    cacheDir = cacheDir,
                    baseName = "openfuel-advanced-export$suffix",
                    extension = "json",
                )
                writeAtomic(file, serializer.serialize(snapshot))
                file
            }
            ExportFormat.CSV -> {
                val file = buildExportFile(
                    cacheDir = cacheDir,
                    baseName = "openfuel-advanced-export$suffix",
                    extension = "csv",
                )
                writeAtomic(file, serializer.serializeCsv(snapshot))
                file
            }
        }
    }

    private suspend fun buildSnapshot(
        appVersion: String,
        redactionOptions: ExportRedactionOptions,
    ): ExportSnapshot {
        val foods = foodDao.getAllFoods().map { it.toDomain() }
            .map { food ->
                if (redactionOptions.redactBrand) {
                    food.copy(brand = null)
                } else {
                    food
                }
            }
        val meals = mealEntryDao.getAllEntries().map { it.toDomain() }
        val globalGoal = goalsRepository.goalForDate(LocalDate.now(clock)).first()
        val goals = listOfNotNull(globalGoal)
        return ExportSnapshot(
            schemaVersion = EXPORT_SCHEMA_VERSION,
            appVersion = appVersion,
            exportedAt = Instant.now(clock),
            foods = foods,
            mealEntries = meals,
            dailyGoals = goals,
        )
    }

    private fun buildExportFile(
        cacheDir: File,
        baseName: String,
        extension: String,
    ): File {
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now(clock))
        return File(exportDir, "$baseName-$timestamp.$extension")
    }

    private fun writeAtomic(file: File, content: String) {
        val atomicFile = AtomicFile(file)
        val outputStream = atomicFile.startWrite()
        try {
            outputStream.write(content.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            atomicFile.finishWrite(outputStream)
        } catch (exception: IOException) {
            atomicFile.failWrite(outputStream)
            throw exception
        }
    }
}
