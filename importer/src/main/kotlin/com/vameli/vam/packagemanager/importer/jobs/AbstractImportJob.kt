package com.vameli.vam.packagemanager.importer.jobs

import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.importer.ImportJob
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.ImportJobResult
import com.vameli.vam.packagemanager.importer.jobs.processors.DelegatingImportFileProcessor
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

private val VAM_ROOT_DIRECTORY_NAMES_TO_SCAN = listOf("AddonPackages", "Custom", "Saves")

internal abstract class AbstractImportJob(
    private val databaseModelService: DatabaseModelService,
    private val delegatingImportFileProcessor: DelegatingImportFileProcessor,
    private val vamInstallationPath: Path,
) : ImportJob {

    protected lateinit var context: ImportJobContext

    override fun execute(progressListener: ProgressListener<ImportJobProgress>): ImportJobResult {
        context = ImportJobContext(vamInstallationPath, progressListener)
        progressListener(simpleTaskProgress("Scanning for files"))
        databaseModelService.markDatabaseImportStarted()
        onBeforeScanForFiles()
        VAM_ROOT_DIRECTORY_NAMES_TO_SCAN.forEach {
            scanForFiles(vamInstallationPath.resolve(it))
        }
        processAllFiles()
        resolveLatestDependencies()
        databaseModelService.markDatabaseImportFinished()
        return ImportJobResult()
    }

    protected fun simpleTaskProgress(text: String) = TaskProgress(ImportJobProgress(text))

    protected fun isPathImportable(path: Path): Boolean {
        if (path.isDirectory()) return false
        return ImportFileExtension.fromExtension(path.extension) != null
    }

    protected fun onBeforeScanForFiles(): Unit = Unit

    private fun scanForFiles(directoryPath: Path) {
        directoryPath.toFile().walkTopDown()
            .asSequence()
            .map { it.toPath() }
            .filter { isPathImportable(it) }
            .forEach { path ->
                val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                context.addFileToImport(path, attributes.size(), attributes.lastModifiedTime().toInstant())
            }
    }

    private fun processAllFiles() {
        context.getFilesToImport().forEach { fileToImport ->
            try {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException("Import interrupted")
                }

                delegatingImportFileProcessor.processFile(fileToImport, context)

                context.markFileAsImported(fileToImport)
            } catch (e: Exception) {
                when (e) {
                    is IOException,
                    is RuntimeException,
                    -> {
                        logger().warn("Error while importing file ${fileToImport.path}", e)
                        context.addImportError(fileToImport.path, e)
                    }

                    else -> throw e
                }
            }
        }

        val importErrors = context.getImportErrors()
        if (importErrors.isNotEmpty()) {
            logger().warn("Import finished with ${importErrors.size} errors: $importErrors")
        }
    }

    private fun resolveLatestDependencies() {

    }
}
