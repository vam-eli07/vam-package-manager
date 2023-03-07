package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.service.VamAuthorService
import com.vameli.vam.packagemanager.core.service.VamTagService
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import com.vameli.vam.packagemanager.importer.vammodel.VamVamFile
import org.springframework.stereotype.Service
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

@Service
internal class VamFileProcessor(
    private val dependencyRefFromJsonExtractor: DependencyRefFromJsonExtractor,
    private val vamAuthorService: VamAuthorService,
    private val vamItemTagService: VamTagService,
    objectMapper: ObjectMapper,
) : AbstractTextResourceProcessor(objectMapper) {
    override fun canProcessResource(
        fileToImport: FileToImport,
        importJobContext: ImportJobContext,
        textResource: TextResource,
    ): Boolean = fileToImport.hasExtension(ImportFileExtension.VAM) ||
        ImportFileExtension.fromExtension(textResource.relativePath.extension) == ImportFileExtension.VAM

    override fun processResource(
        fileToImport: FileToImport,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        deepDependencyScan: Boolean,
        textResourceProvider: TextResourceProvider,
    ): TextResourceProcessorResult? {
        val vamFile = objectMapper.readValue<VamVamFile>(textResource.contentAsString)
        val vajResource = getSiblingVajFile(textResource, textResourceProvider)
        val author = vamAuthorService.findOrCreate(vamFile.creatorName)
        val tags = vamFile.tags?.let { vamItemTagService.getOrCreateTags(it).toMutableSet() } ?: mutableSetOf()
        val dependencies = if (deepDependencyScan) dependencies(textResource, vajResource) else emptySet()
        val vamItem = VamItem(
            id = id(importJobContext, vamFile, fileToImport, textResource),
            displayName = vamFile.displayName,
            type = vamFile.itemType,
            tags = tags,
            author = author,
        )
        return TextResourceProcessorResult(dependencies, vamItem)
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
        return vamDependencyRefs + vajDependencyRefs
    }

    private fun getSiblingVajFile(textResource: TextResource, textResourceProvider: TextResourceProvider): TextResource? {
        val fileNameWithoutExtension = textResource.relativePath.nameWithoutExtension
        val vajFileName = "$fileNameWithoutExtension.vaj"
        val vajPath = textResource.relativePath.resolveSibling(vajFileName)
        return textResourceProvider(vajPath.toString())
    }
}
