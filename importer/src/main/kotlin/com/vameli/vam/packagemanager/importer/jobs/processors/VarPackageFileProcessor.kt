package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.PackageDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.requireState
import com.vameli.vam.packagemanager.core.service.VamAuthorService
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamPackageFileService
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import com.vameli.vam.packagemanager.importer.vammodel.VamMetaJson
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.util.LinkedList
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.Path

private const val META_JSON = "meta.json"
private val NAME_WITH_EXTENSION_REGEX = Regex("^(.+)\\.(.+)$")
private val PATH_WITH_VERSION_REGEX = Regex("^([^/\\\\]+[/\\\\])+([^/\\\\]+)\\.(\\S+)\\.(\\S+)$")

@Service
internal class VarPackageFileProcessor(
    private val vamAuthorService: VamAuthorService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val vamPackageFileService: VamPackageFileService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) : ImportFileProcessor {

    override fun canProcessFile(fileToImport: FileToImport): Boolean =
        fileToImport.getExtension() == ImportFileExtension.VAR

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        StatefulVarPackageFileProcessor(
            vamAuthorService,
            vamDependencyReferenceService,
            vamPackageFileService,
            delegatingTextResourceProcessor,
            objectMapper,
            transactionTemplate,
            context,
            fileToImport,
        ).execute()
    }
}

private class StatefulVarPackageFileProcessor(
    private val vamAuthorService: VamAuthorService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val vamPackageFileService: VamPackageFileService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
    private val importJobContext: ImportJobContext,
    private val fileToImport: FileToImport,
) {
    private lateinit var zipFile: ZipFile
    private lateinit var metaJson: VamMetaJson
    private lateinit var vamPackageFile: VamPackageFile
    private val zipEntryQueue = LinkedList<ZipEntry>()
    private val pathToZipEntryMap = mutableMapOf<String, ZipEntry>()

    fun execute() {
        ZipFile(fileToImport.path.toFile()).use { zipFile ->
            this.zipFile = zipFile
            buildZipEntryPathsCache()
            zipFile.entries()
                .asSequence()
                .filter { it.isProcessable() }
                .forEach { processZipEntry(fileToImport, it) }
            finalize()
        }
    }

    private fun buildZipEntryPathsCache() {
        zipFile
            .entries()
            .asSequence()
            .filter { !it.isDirectory }
            .forEach { pathToZipEntryMap[it.name] = it }
    }

    private fun VamMetaJson.toVamPackageFile(): VamPackageFile {
        val relativePath = importJobContext.getPathRelativeToVamInstallation(fileToImport).toString()
        val matchResult = PATH_WITH_VERSION_REGEX.matchEntire(relativePath)
            ?: throw IllegalStateException("Invalid file name: $relativePath")
        val dependencyRef = PackageDependencyReference(creatorName, packageName, matchResult.groupValues[3])

        return VamPackageFile(
            relativePath,
            0,
            fileToImport.fileSizeBytes,
            fileToImport.lastModified,
            licenseType,
            vamDependencyReferenceService.findOrCreate(dependencyRef),
            vamAuthorService.findOrCreate(creatorName),
            mutableSetOf(),
        )
    }

    private fun ZipEntry.isProcessable(): Boolean {
        if (isDirectory) return false
        val matchResult = NAME_WITH_EXTENSION_REGEX.matchEntire(name) ?: return false
        val extension = ImportFileExtension.fromExtension(matchResult.groupValues[2]) ?: return false
        return extension.isTextType
    }

    private fun processZipEntry(fileToImport: FileToImport, zipEntry: ZipEntry) {
        if (zipEntry.name.equals(META_JSON, true)) {
            zipFile.getInputStream(zipEntry).reader().use { reader ->
                metaJson = objectMapper.readValue(reader, VamMetaJson::class.java)
            }
            vamPackageFile = vamPackageFileService.createOrReplace(metaJson.toVamPackageFile())
        } else if (!this::metaJson.isInitialized) {
            zipEntryQueue.add(zipEntry)
        } else {
            importJobContext.publishProgress(
                ImportJobProgress(
                    "Package resource: ${zipEntry.name}",
                    importJobContext.getPathRelativeToVamInstallation(fileToImport),
                ),
            )
            val resourceContent = loadTextResourceInPackage(zipEntry)
            delegatingTextResourceProcessor.processResource(
                fileToImport,
                vamPackageFile,
                importJobContext,
                resourceContent,
            ) { stringPathRelativeToCurrentFile ->
                loadTextResourceInPackage(stringPathRelativeToCurrentFile)
            }
        }
    }

    private fun loadTextResourceInPackage(zipEntry: ZipEntry): TextResource = zipFile.getInputStream(zipEntry).reader().use { reader ->
        val contentAsString = reader.readText()
        val rootNode = try {
            objectMapper.readTree(contentAsString)
        } catch (e: JsonProcessingException) {
            logger().debug("Invalid JSON in file ${zipEntry.name} in package ${fileToImport.path}", e)
            null
        }
        TextResource(Path(zipEntry.name), contentAsString, rootNode)
    }

    private fun loadTextResourceInPackage(stringPathRelativeToCurrentFile: String): TextResource? =
        pathToZipEntryMap[stringPathRelativeToCurrentFile]?.let { loadTextResourceInPackage(it) }

    private fun finalize() {
        transactionTemplate.execute {
            requireState(::metaJson.isInitialized) { "meta.json not found in package ${fileToImport.path}" }
            zipEntryQueue.forEach { processZipEntry(fileToImport, it) }
        }
    }
}
