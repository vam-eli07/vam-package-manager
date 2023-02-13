package com.vameli.vam.packagemanager.importer

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.core.data.model.ArtifactId
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.importer.jobs.FullImportJob
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class ImportJobFactory(
    private val databaseModelService: DatabaseModelService,
    private val databaseEnvironment: DatabaseEnvironment,
) {

    fun createFullImportJob(vamInstallationPath: Path): ImportJob =
        FullImportJob(databaseModelService, vamInstallationPath)
}

interface ImportJob : LongRunningTask<ImportJobProgress, ImportJobResult>

data class ImportError(val artifactId: ArtifactId, val artifactPath: Path, val error: Throwable)

data class ImportJobResult(val importErrors: List<ImportError> = emptyList())

data class ImportJobProgress(
    val currentOperation: String,
    val currentlyProcessingFile: Path? = null,
    val currentlyProcessingArtifactId: ArtifactId? = null,
)
