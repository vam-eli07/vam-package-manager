package com.vameli.vam.packagemanager.importer.jobs

import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.importer.jobs.processors.DelegatingImportFileProcessor
import java.nio.file.Path

internal class FullImportJob(
    databaseModelService: DatabaseModelService,
    delegatingImportFileProcessor: DelegatingImportFileProcessor,
    vamInstallationPath: Path,
) : AbstractImportJob(databaseModelService, delegatingImportFileProcessor, vamInstallationPath)
