package com.vameli.vam.packagemanager.importer

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.importer.jobs.FullImportJob
import com.vameli.vam.packagemanager.importer.jobs.processors.DelegatingImportFileProcessor
import org.springframework.stereotype.Component
import java.nio.file.Path

interface ImportJobFactory {
    fun createFullImportJob(vamInstallationPath: Path): ImportJob
}

@Component
internal class ImportJobFactoryImpl(
    private val databaseModelService: DatabaseModelService,
    private val delegatingImportFileProcessor: DelegatingImportFileProcessor,
) : ImportJobFactory {

    override fun createFullImportJob(vamInstallationPath: Path): ImportJob =
        FullImportJob(databaseModelService, delegatingImportFileProcessor, vamInstallationPath)
}

interface ImportJob : LongRunningTask<ImportJobProgress, ImportJobResult>

data class ImportError(val filePath: Path, val error: Throwable)

data class ImportJobResult(val importErrors: List<ImportError> = emptyList())

data class ImportJobProgress(
    val currentOperation: String,
    val currentlyProcessingFile: Path? = null,
    val currentlyProcessingDependencyReference: DependencyReference? = null,
)
