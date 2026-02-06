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
        val foods = foodDao.getAllFoods().map { it.toDomain() }
        val meals = mealEntryDao.getAllEntries().map { it.toDomain() }
        val globalGoal = goalsRepository.goalForDate(LocalDate.now(clock)).first()
        val goals = listOfNotNull(globalGoal)
        val snapshot = ExportSnapshot(
            schemaVersion = EXPORT_SCHEMA_VERSION,
            appVersion = appVersion,
            exportedAt = Instant.now(clock),
            foods = foods,
            mealEntries = meals,
            dailyGoals = goals,
        )
        val json = serializer.serialize(snapshot)
        val exportDir = File(cacheDir, "exports").apply { mkdirs() }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now(clock))
        val exportFile = File(exportDir, "openfuel-export-$timestamp.json")
        writeAtomic(exportFile, json)
        exportFile
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
