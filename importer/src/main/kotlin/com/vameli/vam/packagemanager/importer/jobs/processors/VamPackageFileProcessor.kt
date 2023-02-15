package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension.Companion.getExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import com.vameli.vam.packagemanager.importer.vammodel.VamMetaJson
import org.springframework.stereotype.Service
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val META_JSON = "meta.json"
private val NAME_WITH_EXTENSION_REGEX = Regex("^(.+)\\.(.+)$")

@Service
internal class VamPackageFileProcessor(
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
) : ImportFileProcessor {

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        if (fileToImport.getExtension() != ImportFileExtension.VAR) {
            return
        }
        val vamPackageContext = VamPackageContext(delegatingTextResourceProcessor, context)
        ZipFile(fileToImport.path.toFile()).use { zipFile ->
            zipFile.entries()
                .asSequence()
                .filter { it.isProcessable() }
                .forEach { processZipEntry(fileToImport, it, vamPackageContext) }
        }
    }

    private fun ZipEntry.isProcessable(): Boolean {
        if (isDirectory) return false
        val matchResult = NAME_WITH_EXTENSION_REGEX.matchEntire(name) ?: return false
        val extension = ImportFileExtension.fromExtension(matchResult.groupValues[2]) ?: return false
        return extension.isTextType
    }

    private fun processZipEntry(fileToImport: FileToImport, zipEntry: ZipEntry, vamPackageContext: VamPackageContext) {
        vamPackageContext.importJobContext.publishProgress(
            ImportJobProgress(
                "Package resource: ${zipEntry.name}",
                vamPackageContext.importJobContext.getPathRelativeToVamInstallation(fileToImport),
            ),
        )
    }
}

private class VamPackageContext(
    val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    val importJobContext: ImportJobContext,
) {

    private lateinit var metaJson: VamMetaJson
}
