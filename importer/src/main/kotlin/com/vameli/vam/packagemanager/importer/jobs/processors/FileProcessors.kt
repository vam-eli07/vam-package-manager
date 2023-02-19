package com.vameli.vam.packagemanager.importer.jobs.processors

import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import org.springframework.stereotype.Service

internal interface ImportFileProcessor {
    fun canProcessFile(fileToImport: FileToImport): Boolean
    fun processFile(fileToImport: FileToImport, context: ImportJobContext)
}

@Service
internal class DelegatingImportFileProcessor(
    private val processors: List<ImportFileProcessor>,
) : ImportFileProcessor {

    init {
        logger().debug("Following file processors are available: ${processors.map { it.javaClass.simpleName }}")
    }

    override fun canProcessFile(fileToImport: FileToImport): Boolean = true

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        processors.forEach {
            if (it.canProcessFile(fileToImport)) {
                it.processFile(fileToImport, context)
            }
        }
    }
}
