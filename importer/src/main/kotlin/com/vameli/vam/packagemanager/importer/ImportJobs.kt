package com.vameli.vam.packagemanager.importer

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.core.data.model.ArtifactId
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class ImportJobFactory(
    private val databaseModelService: DatabaseModelService,
    private val databaseEnvironment: DatabaseEnvironment,
) {

    fun createFullImportJob(vamInstallationPath: Path): ImportJob = FullImportJob(
        databaseEnvironment = databaseEnvironment,
        databaseModelService = databaseModelService,
        vamInstallationPath = vamInstallationPath,
    )
}

interface ImportJob : LongRunningTask<ImportJobProgress, ImportJobResult>

data class ImportJobProgress(
    val currentOperation: String,
    val currentlyProcessingFile: Path? = null,
    val currentlyProcessingArtifactId: ArtifactId? = null,
)

data class ImportJobResult(val errorsInFiles: List<Path> = emptyList())

internal class ImportJobContext

internal class FullImportJob(
    private val databaseEnvironment: DatabaseEnvironment,
    private val databaseModelService: DatabaseModelService,
    private val vamInstallationPath: Path,
) : ImportJob {

    private val context = ImportJobContext()

    override fun execute(progressListener: ProgressListener<ImportJobProgress>): ImportJobResult {
        progressListener(simpleTaskProgress("Initializing database"))
        databaseModelService.markDatabaseImportStarted()
        Thread.sleep(2000)
        (1..5).forEach {
            val percent = it * 20
            progressListener(TaskProgress(ImportJobProgress("Doing something important", null, null), percent))
            Thread.sleep(2000)
        }
        databaseModelService.markDatabaseImportFinished()
        return ImportJobResult()
    }

    private fun simpleTaskProgress(operationName: String) = TaskProgress(ImportJobProgress(operationName))
}
