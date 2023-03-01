package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.FilesystemDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.data.model.VamResourceFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.logger
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
    objectMapper: ObjectMapper,
) : AbstractTextResourceProcessor(objectMapper) {
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
        val dependencies = dependencies(textResource, vajResource)
        val vamItem = VamItem(
            id = id(importJobContext, vamFile, fileToImport, textResource),
            displayName = vamFile.displayName,
            type = vamFile.itemType,
            tags = tags,
            author = author,
            resourceFiles = resourceFiles(vamResourceFile, dependencies),
            dependencies = dependencies.toVamDependencyReferences(),
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
    ): Set<DependencyReference> {
        val vamRootNode = getJsonRootNode(vamTextResource)
        val vajRootNode = vajTextResource?.let { getJsonRootNode(it) }
        val vamDependencyRefs = vamRootNode?.let { dependencyRefFromJsonExtractor.extractDependencyReferences(it) } ?: emptySet()
        val vajDependencyRefs = vajRootNode?.let { dependencyRefFromJsonExtractor.extractDependencyReferences(it) } ?: emptySet()
        if (vamDependencyRefs.isNotEmpty() || vajDependencyRefs.isNotEmpty()) {
            logger().info("YAAAAY! Found some dependencies! Vam: $vamDependencyRefs, Vaj: $vajDependencyRefs")
        }
        return vamDependencyRefs + vajDependencyRefs
    }

    private fun Collection<DependencyReference>.toVamDependencyReferences(): Set<VamDependencyReference> =
        this.takeIf { it.isNotEmpty() }?.let { dependencyReferenceService.findOrCreate(it) } ?: emptySet()

    private fun getSiblingVajFile(textResource: TextResource, textResourceProvider: TextResourceProvider): TextResource? {
        val fileNameWithoutExtension = textResource.relativePath.nameWithoutExtension
        val vajFileName = "$fileNameWithoutExtension.vaj"
        val vajPath = textResource.relativePath.resolveSibling(vajFileName)
        return textResourceProvider(vajPath.toString())
    }

    private fun resourceFiles(vamResourceFile: VamResourceFile, dependencies: Collection<DependencyReference>): Set<VamResourceFile> {
        return if (vamResourceFile is VamPackageFile) {
            setOf(vamResourceFile)
        } else {
            dependencies
                .filterIsInstance(FilesystemDependencyReference::class.java)
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
