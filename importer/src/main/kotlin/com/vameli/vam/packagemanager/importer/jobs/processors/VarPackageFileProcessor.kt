package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.FileInPackageDependencyReference
import com.vameli.vam.packagemanager.core.data.model.FilesystemDependencyReference
import com.vameli.vam.packagemanager.core.data.model.PackageDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.requireState
import com.vameli.vam.packagemanager.core.service.VamAuthorService
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamItemService
import com.vameli.vam.packagemanager.core.service.VamPackageFileService
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import com.vameli.vam.packagemanager.importer.vammodel.HasVamMetaJsonDependency
import com.vameli.vam.packagemanager.importer.vammodel.VamMetaJson
import org.springframework.stereotype.Service
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
    private val vamItemService: VamItemService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
) : ImportFileProcessor {

    override fun canProcessFile(fileToImport: FileToImport): Boolean =
        fileToImport.getExtension() == ImportFileExtension.VAR

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        StatefulVarPackageFileProcessor(
            vamAuthorService,
            vamDependencyReferenceService,
            vamPackageFileService,
            vamItemService,
            delegatingTextResourceProcessor,
            objectMapper,
            context,
            fileToImport,
        ).execute()
    }
}

private class StatefulVarPackageFileProcessor(
    private val vamAuthorService: VamAuthorService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val vamPackageFileService: VamPackageFileService,
    private val vamItemService: VamItemService,
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val objectMapper: ObjectMapper,
    private val importJobContext: ImportJobContext,
    private val fileToImport: FileToImport,
) {
    private lateinit var zipFile: ZipFile
    private lateinit var metaJson: VamMetaJson
    private lateinit var vamPackageFile: VamPackageFile
    private lateinit var thisPackageDependencyReference: PackageDependencyReference
    private val otherPackageDependencies: MutableSet<PackageDependencyReference> = mutableSetOf()
    private val zipEntryQueue = LinkedList<ZipEntry>()
    private val pathToZipEntryMap = mutableMapOf<String, ZipEntry>()
    private val itemProcessResults = mutableListOf<TextResourceProcessorResult>()

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

    private fun ZipEntry.isProcessable(): Boolean {
        if (isDirectory) return false
        val matchResult = NAME_WITH_EXTENSION_REGEX.matchEntire(name) ?: return false
        val extension = ImportFileExtension.fromExtension(matchResult.groupValues[2]) ?: return false
        return extension.isTextType
    }

    private fun processZipEntry(fileToImport: FileToImport, zipEntry: ZipEntry) {
        if (zipEntry.name.equals(META_JSON, true)) {
            zipFile.getInputStream(zipEntry).reader().use { reader ->
                processMetaJson(objectMapper.readValue(reader, VamMetaJson::class.java))
            }
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
            val results = delegatingTextResourceProcessor.processResource(
                fileToImport = fileToImport,
                importJobContext = importJobContext,
                textResource = resourceContent,
                deepDependencyScan = false,
            ) { stringPathRelativeToCurrentFile ->
                loadTextResourceInPackage(stringPathRelativeToCurrentFile)
            }
            itemProcessResults.addAll(results)
        }
    }

    private fun VamMetaJson.toVamPackageFile(dependencyRef: PackageDependencyReference): VamPackageFile {
        val relativePath = importJobContext.getPathRelativeToVamInstallation(fileToImport).toString()

        return VamPackageFile(
            relativePath = relativePath,
            version = 0,
            fileSizeBytes = fileToImport.fileSizeBytes,
            lastModified = fileToImport.lastModified,
            providedDependencyReference = vamDependencyReferenceService.findOrCreate(dependencyRef),
            licenseType = licenseType,
            author = vamAuthorService.findOrCreate(creatorName),
        )
    }

    private fun processMetaJson(metaJson: VamMetaJson) {
        this.metaJson = metaJson
        val relativePath = importJobContext.getPathRelativeToVamInstallation(fileToImport).toString()
        val matchResult = PATH_WITH_VERSION_REGEX.matchEntire(relativePath)
            ?: throw IllegalStateException("Invalid file name: $relativePath")
        val dependencyRef = PackageDependencyReference(metaJson.creatorName, metaJson.packageName, matchResult.groupValues[3])
        vamPackageFile = vamPackageFileService.createOrReplace(metaJson.toVamPackageFile(dependencyRef))
        thisPackageDependencyReference = dependencyRef
        processMetaJsonDependency(metaJson)
        vamPackageFileService.setPackageDependencies(vamPackageFile, otherPackageDependencies)
    }

    private fun processMetaJsonDependency(dependency: HasVamMetaJsonDependency) {
        dependency.dependencies.forEach { key, nestedDependency ->
            val packageDependencyRef = DependencyReference.fromString(key) as? PackageDependencyReference
            packageDependencyRef?.let {
                otherPackageDependencies.add(it)
            }
            processMetaJsonDependency(nestedDependency)
        }
    }

    private fun loadTextResourceInPackage(zipEntry: ZipEntry): TextResource = zipFile.getInputStream(zipEntry).reader().use { reader ->
        val contentAsString = reader.readText()
        TextResource(Path(zipEntry.name), contentAsString)
    }

    private fun loadTextResourceInPackage(stringPathRelativeToCurrentFile: String): TextResource? =
        pathToZipEntryMap[stringPathRelativeToCurrentFile]?.let { loadTextResourceInPackage(it) }

    private fun finalize() {
        requireState(::metaJson.isInitialized) { "meta.json not found in package ${fileToImport.path}" }
        zipEntryQueue.forEach { processZipEntry(fileToImport, it) }

        itemProcessResults.forEach {
            processResultItem(vamItemService.saveItem(it.vamItem), it.dependencyReferences)
        }
    }

    private fun processResultItem(vamItem: VamItem, dependencyReferences: Collection<DependencyReference>) {
        vamItemService.addItemResourceFiles(vamItem, listOf(vamPackageFile))

        val packageDependencies = dependencyReferences.mapNotNull {
            when (it) {
                is PackageDependencyReference -> it
                is FileInPackageDependencyReference -> it.toPackageReference()
                is FilesystemDependencyReference -> null
            }
        }

        vamItemService.setItemDependencies(vamItem, packageDependencies)
    }
}
