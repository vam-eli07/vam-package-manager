package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.FilesystemDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamItemService
import com.vameli.vam.packagemanager.core.service.VamStandaloneFileService
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

typealias TextResourceProvider = (stringPathRelativeToCurrentFile: String) -> TextResource?

data class TextResource(val relativePath: Path, val contentAsString: String)

data class TextResourceProcessorResult(
    val dependencyReferences: Set<DependencyReference>,
    val vamItem: VamItem,
)

internal interface TextResourceProcessor {
    fun canProcessResource(
        fileToImport: FileToImport,
        importJobContext: ImportJobContext,
        textResource: TextResource,
    ): Boolean

    fun processResource(
        fileToImport: FileToImport,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        textResourceProvider: TextResourceProvider,
    ): TextResourceProcessorResult?
}

@Service
internal class TextResourceFromFileProcessor(
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val vamStandaloneFileService: VamStandaloneFileService,
    private val vamItemService: VamItemService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
) : ImportFileProcessor {

    override fun canProcessFile(fileToImport: FileToImport): Boolean = fileToImport.getExtension()?.isTextType ?: false

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        context.publishProgress(
            ImportJobProgress(
                "Processing file",
                context.getPathRelativeToVamInstallation(fileToImport),
            ),
        )
        val textResourceContent = loadTextResource(fileToImport, context)
        val results = delegatingTextResourceProcessor.processResource(
            fileToImport,
            context,
            textResourceContent,
        ) { stringPathRelativeToCurrentFile ->
            loadTextResource(fileToImport, context, stringPathRelativeToCurrentFile)
        }
        results.forEach { processResult(it, fileToImport, context) }
    }

    private fun loadTextResource(
        fileToImport: FileToImport,
        context: ImportJobContext,
        stringPathRelativeToCurrentFile: String? = null,
    ): TextResource {
        val relativePathOfCurrentFile = context.getPathRelativeToVamInstallation(fileToImport)
        val finalPath = if (stringPathRelativeToCurrentFile != null) {
            val pathOfSibling = relativePathOfCurrentFile.resolveSibling(stringPathRelativeToCurrentFile)
            val pathFromVamRoot = context.getPathRelativeToVamInstallation(stringPathRelativeToCurrentFile)
            pathOfSibling.takeIf { it.isRegularFile() }
                ?: pathFromVamRoot.takeIf { it.isRegularFile() }
                ?: throw FileNotFoundException("File not found: $stringPathRelativeToCurrentFile")
        } else {
            fileToImport.path
        }

        val contentAsString = finalPath.readText()
        return TextResource(finalPath, contentAsString)
    }

    private fun FileToImport.toVamStandaloneFile(context: ImportJobContext): VamStandaloneFile {
        val relativePathAsString = context.getPathRelativeToVamInstallation(this).toString()
        val dependencyReference = DependencyReference.fromString(relativePathAsString)
            ?: throw IllegalArgumentException("Invalid relative path: $relativePathAsString")
        return VamStandaloneFile(
            relativePath = relativePathAsString,
            fileSizeBytes = fileSizeBytes,
            lastModified = lastModified,
            vamDependencyReference = vamDependencyReferenceService.findOrCreate(dependencyReference),
        )
    }

    private fun processResult(result: TextResourceProcessorResult, fileToImport: FileToImport, context: ImportJobContext) {
        val vamItem = result.vamItem
        val vamStandaloneFile = fileToImport.toVamStandaloneFile(context)
        val standaloneFileDependencies = result.dependencyReferences.filterIsInstance<FilesystemDependencyReference>()
        val standaloneFiles = standaloneFileDependencies.map { dependencyRef ->
            val attributes = Files.readAttributes(Path(dependencyRef.relativePath), BasicFileAttributes::class.java)
            val vamDependencyReference = vamDependencyReferenceService.findOrCreate(dependencyRef)
            VamStandaloneFile(
                relativePath = dependencyRef.relativePath,
                fileSizeBytes = attributes.size(),
                lastModified = attributes.lastModifiedTime().toInstant(),
                vamDependencyReference = vamDependencyReference,
            )
        } + vamStandaloneFile
        vamStandaloneFile.item = vamItem
        vamItemService.addItemResourceFiles(vamItem, vamStandaloneFileService.createOrReplace(standaloneFiles))
        vamItemService.saveItem(vamItem)
    }
}

@Service
internal class DelegatingTextResourceProcessor(
    private val processors: List<TextResourceProcessor>,
) {

    init {
        logger().debug("Following text resource processors are available: ${processors.map { it.javaClass.simpleName }}")
    }

    fun processResource(
        fileToImport: FileToImport,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        textResourceProvider: TextResourceProvider,
    ): List<TextResourceProcessorResult> = processors
        .filter { it.canProcessResource(fileToImport, importJobContext, textResource) }
        .mapNotNull { it.processResource(fileToImport, importJobContext, textResource, textResourceProvider) }
}

abstract class AbstractTextResourceProcessor(protected val objectMapper: ObjectMapper) : TextResourceProcessor {

    protected fun getJsonRootNode(textResource: TextResource): JsonNode? {
        return try {
            objectMapper.readTree(textResource.contentAsString)
        } catch (e: JsonProcessingException) {
            logger().debug("File ${textResource.relativePath} is not a valid JSON file", e)
            null
        }
    }
}
