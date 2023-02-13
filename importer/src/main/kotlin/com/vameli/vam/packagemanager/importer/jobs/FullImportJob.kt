package com.vameli.vam.packagemanager.importer.jobs

import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import java.nio.file.Path

internal class FullImportJob(
    databaseModelService: DatabaseModelService,
    vamInstallationPath: Path,
) : AbstractImportJob(databaseModelService, vamInstallationPath)
