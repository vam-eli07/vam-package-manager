package com.vameli.vam.packagemanager.importer.jobs

import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.importer.ImportError
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.util.Collections
import java.util.LinkedList

internal class ImportJobContext(
    private val vamInstallationPath: Path,
    private val progressListener: ProgressListener<ImportJobProgress>,
) {
    private val importErrors = mutableListOf<ImportError>()
    private val filesToImport = LinkedList<FileToImport>()
    private var totalFileSizeBytes: Long = 0
    private var totalBytesImported: Long = 0
    private var percentCompleted: Int = 0

    fun addFileToImport(path: Path, fileSizeBytes: Long, lastModified: Instant) {
        val fileToImport = FileToImport(path, fileSizeBytes, lastModified)
        filesToImport.add(fileToImport)
        totalFileSizeBytes += fileSizeBytes
        logger().trace("Added file to import: $fileToImport total size is now $totalFileSizeBytes")
        recomputePercentCompleted()
    }

    fun getFilesToImport(): List<FileToImport> = Collections.unmodifiableList(filesToImport)

    fun markFileAsImported(fileToImport: FileToImport) {
        totalBytesImported += fileToImport.fileSizeBytes
        recomputePercentCompleted()
        logger().trace("$fileToImport imported, total percent completed is now $percentCompleted")
    }

    fun addImportError(filePath: Path, error: Throwable) {
        importErrors.add(ImportError(filePath, error))
    }

    fun getImportErrors(): List<ImportError> = Collections.unmodifiableList(importErrors)

    fun publishProgress(importJobProgress: ImportJobProgress) {
        progressListener(TaskProgress(importJobProgress, percentCompleted))
    }

    fun getPathRelativeToVamInstallation(fileToImport: FileToImport): Path =
        vamInstallationPath.relativize(fileToImport.path)

    fun getPathRelativeToVamInstallation(relativePath: String): Path =
        vamInstallationPath.relativize(Paths.get(relativePath))

    private fun recomputePercentCompleted() {
        if (totalFileSizeBytes == 0L) return
        val newPercentCompleted = (totalBytesImported * 100 / totalFileSizeBytes).toInt()
        if (newPercentCompleted > percentCompleted) {
            percentCompleted = newPercentCompleted
        }
    }
}

data class FileToImport(val path: Path, val fileSizeBytes: Long, val lastModified: Instant)
