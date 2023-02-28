package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vameli.vam.packagemanager.core.data.model.FilesystemDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.data.model.VamResourceFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.service.VamAuthorService
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamItemService
import com.vameli.vam.packagemanager.core.service.VamStandaloneFileService
import com.vameli.vam.packagemanager.core.service.VamTagService
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import com.vameli.vam.packagemanager.importer.vammodel.VamVamFile
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

@Service
internal class VamFileProcessor(
    private val dependencyRefFromJsonExtractor: DependencyRefFromJsonExtractor,
    private val dependencyReferenceService: VamDependencyReferenceService,
    private val vamStandaloneFileService: VamStandaloneFileService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val vamAuthorService: VamAuthorService,
    private val vamItemService: VamItemService,
    private val vamItemTagService: VamTagService,
    private val objectMapper: ObjectMapper,
) :
    TextResourceProcessor {
    override fun canProcessResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
    ): Boolean = fileToImport.hasExtension(ImportFileExtension.VAM) ||
        ImportFileExtension.fromExtension(textResource.relativePath.extension) == ImportFileExtension.VAM

    override fun processResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        textResourceProvider: TextResourceProvider,
    ) {
        val vamFile = objectMapper.readValue<VamVamFile>(textResource.contentAsString)
        val vajResource = getSiblingVajFile(textResource, textResourceProvider)
        val author = vamAuthorService.findOrCreate(vamFile.creatorName)
        val tags = vamFile.tags?.let { vamItemTagService.getOrCreateTags(it) } ?: emptySet()
        val vamItem = VamItem(
            id = id(importJobContext, vamFile, fileToImport, textResource),
            displayName = vamFile.displayName,
            type = vamFile.itemType,
            tags = tags,
            author = author,
            resourceFiles = resourceFiles(vamResourceFile, setOfNotNull(textResource, vajResource)),
            dependencies = dependencies(textResource, vajResource),
        )
        vamResourceFile.addItem(vamItem)
        vamItemService.saveItem(vamItem)
    }

    private fun id(
        importJobContext: ImportJobContext,
        vamVamFile: VamVamFile,
        fileToImport: FileToImport,
        textResource: TextResource,
    ): String {
        val relativePath = importJobContext.getPathRelativeToVamInstallation(fileToImport)
        return if (fileToImport.hasExtension(ImportFileExtension.VAR)) {
            "$relativePath::${textResource.relativePath}::${vamVamFile.uid}"
        } else {
            "$relativePath::${vamVamFile.uid}"
        }
    }

    private fun dependencies(
        vamTextResource: TextResource,
        vajTextResource: TextResource?,
    ): Set<VamDependencyReference> {
        val vamDependencyRefs =
            vamTextResource.parsedResourceJsonRoot?.let { dependencyRefFromJsonExtractor.extractDependencyReferences(it) } ?: emptyList()
        val vajDependencyRefs =
            vajTextResource?.parsedResourceJsonRoot?.let { dependencyRefFromJsonExtractor.extractDependencyReferences(it) } ?: emptyList()
        val allDependencyRefs = vamDependencyRefs + vajDependencyRefs
        return if (allDependencyRefs.isNotEmpty()) {
            return dependencyReferenceService.findOrCreate(vamDependencyRefs + vajDependencyRefs).toSet()
        } else {
            emptySet()
        }
    }

    private fun getSiblingVajFile(textResource: TextResource, textResourceProvider: TextResourceProvider): TextResource? {
        val fileNameWithoutExtension = textResource.relativePath.nameWithoutExtension
        val vajFileName = "$fileNameWithoutExtension.vaj"
        val vajPath = textResource.relativePath.resolveSibling(vajFileName)
        return textResourceProvider(vajPath.toString())
    }

    private fun resourceFiles(vamResourceFile: VamResourceFile, textResources: Collection<TextResource>): Set<VamResourceFile> {
        return if (vamResourceFile is VamPackageFile) {
            setOf(vamResourceFile)
        } else {
            textResources
                .mapNotNull { it.parsedResourceJsonRoot }
                .flatMap {
                    dependencyRefFromJsonExtractor.extractDependencyReferences(it).filterIsInstance(FilesystemDependencyReference::class.java)
                }
                .map { dependencyRef ->
                    val attributes = Files.readAttributes(Path(dependencyRef.relativePath), BasicFileAttributes::class.java)
                    val vamDependencyReference = vamDependencyReferenceService.findOrCreate(dependencyRef)
                    val standaloneFile = VamStandaloneFile(
                        relativePath = dependencyRef.relativePath,
                        fileSizeBytes = attributes.size(),
                        lastModified = attributes.lastModifiedTime().toInstant(),
                        vamDependencyReference = vamDependencyReference,
                    )
                    vamStandaloneFileService.createOrReplace(standaloneFile)
                }.toSet()
        }
    }

    private fun VamResourceFile.addItem(item: VamItem) {
        when (this) {
            is VamPackageFile -> items.add(item)
            is VamStandaloneFile -> this.item = item
            else -> throw IllegalStateException("Unknown VamResourceFile type: $this")
        }
    }
}
