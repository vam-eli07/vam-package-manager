package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.requireState
import com.vameli.vam.packagemanager.core.service.VamResourceFileService
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
internal class VamPackageFileProcessorDelegate(
    private val vamResourceFileService: VamResourceFileService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
) : ImportFileProcessor {

    override fun canProcessFile(fileToImport: FileToImport): Boolean =
        fileToImport.getExtension() == ImportFileExtension.VAR

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        VamPackageFileProcessor(
            vamResourceFileService,
            delegatingTextResourceProcessor,
            objectMapper,
            context,
            fileToImport,
        ).execute()
    }
}

private class VamPackageFileProcessor(
    private val vamResourceFileService: VamResourceFileService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
    private val importJobContext: ImportJobContext,
    private val fileToImport: FileToImport,
) {
    private lateinit var metaJson: VamMetaJson
    private lateinit var zipFile: ZipFile

    fun execute() {
        ZipFile(fileToImport.path.toFile()).use { zipFile ->
            this.zipFile = zipFile
            zipFile.entries()
                .asSequence()
                .filter { it.isProcessable() }
                .forEach { processZipEntry(fileToImport, it) }
        }
        finalize()
    }

    private fun VamMetaJson.toVamPackageFile() = VamPackageFile(
        importJobContext.getPathRelativeToVamInstallation(fileToImport).toString(),
        0,
        fileToImport.fileSizeBytes,
        fileToImport.lastModified,
        licenseType,
        TODO(),
        TODO(),
        TODO(),
    )

    private fun ZipEntry.isProcessable(): Boolean {
        if (isDirectory) return false
        val matchResult = NAME_WITH_EXTENSION_REGEX.matchEntire(name) ?: return false
        val extension = ImportFileExtension.fromExtension(matchResult.groupValues[2]) ?: return false
        return extension.isTextType
    }

    private fun processZipEntry(fileToImport: FileToImport, zipEntry: ZipEntry) {
        importJobContext.publishProgress(
            ImportJobProgress(
                "Package resource: ${zipEntry.name}",
                importJobContext.getPathRelativeToVamInstallation(fileToImport),
            ),
        )
        if (zipEntry.name == META_JSON) {
            zipFile.getInputStream(zipEntry).reader().use { reader ->
                metaJson = objectMapper.readValue(reader, VamMetaJson::class.java)
            }
        }
    }

    private fun finalize() {
        requireState(::metaJson.isInitialized) { "meta.json not found in package ${fileToImport.path}" }
    }
}
