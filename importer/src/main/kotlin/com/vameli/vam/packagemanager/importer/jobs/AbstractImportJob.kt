package com.vameli.vam.packagemanager.importer.jobs

import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.data.model.ArtifactId
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.requireState
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.importer.ImportError
import com.vameli.vam.packagemanager.importer.ImportJob
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.ImportJobResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.Collections
import java.util.LinkedList
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

private val VAM_ROOT_DIRECTORY_NAMES_TO_SCAN = listOf("AddonPackages", "Custom", "Saves")
private val IMPORTABLE_FILE_EXTENSIONS = setOf("vam", "vap", "var")

internal abstract class AbstractImportJob(
    private val databaseModelService: DatabaseModelService,
    private val vamInstallationPath: Path,
) : ImportJob {

    protected val context = ImportJobContext()

    override fun execute(progressListener: ProgressListener<ImportJobProgress>): ImportJobResult {
        context.progressListener = progressListener
        progressListener(simpleTaskProgress("Scanning for files"))
        databaseModelService.markDatabaseImportStarted()
        onBeforeScanForFiles()
        VAM_ROOT_DIRECTORY_NAMES_TO_SCAN.forEach {
            scanForFiles(vamInstallationPath.resolve(it))
        }
        logger().debug("Files to import: ${context.getFilesToImport()}")
        databaseModelService.markDatabaseImportFinished()
        return ImportJobResult()
    }

    protected fun simpleTaskProgress(text: String) = TaskProgress(ImportJobProgress(text))

    protected fun isPathImportable(path: Path): Boolean {
        if (path.isDirectory()) return false
        return path.extension in IMPORTABLE_FILE_EXTENSIONS
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
}

internal data class FileToImport(val path: Path, val fileSizeBytes: Long, val lastModified: Instant)

internal class ImportJobContext {
    private val importErrors = mutableListOf<ImportError>()
    private val filesToImportStack = LinkedList<FileToImport>()
    private var totalFileSizeBytes: Long = 0

    var progressListener: ProgressListener<ImportJobProgress>? = null
        set(value) {
            requireState(field == null) { "progressListener has already been set" }
            field = value
        }

    fun addFileToImport(path: Path, fileSizeBytes: Long, lastModified: Instant) {
        filesToImportStack.add(FileToImport(path, fileSizeBytes, lastModified))
        totalFileSizeBytes += fileSizeBytes
    }

    fun getFilesToImport(): List<FileToImport> = Collections.unmodifiableList(filesToImportStack)

    fun addImportError(artifactId: ArtifactId, artifactPath: Path, error: Throwable) {
        importErrors.add(ImportError(artifactId, artifactPath, error))
    }

    fun getImportErrors(): List<ImportError> = Collections.unmodifiableList(importErrors)
}
