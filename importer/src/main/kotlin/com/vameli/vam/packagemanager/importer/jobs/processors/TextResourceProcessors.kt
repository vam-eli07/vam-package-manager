package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamResourceFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamStandaloneFileService
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

typealias TextResourceProvider = (stringPathRelativeToCurrentFile: String) -> TextResource?

data class TextResource(val relativePath: Path, val contentAsString: String, val parsedResourceJsonRoot: JsonNode? = null)

internal interface TextResourceProcessor {
    fun canProcessResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
    ): Boolean

    fun processResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        textResourceProvider: TextResourceProvider,
    )
}

@Service
internal class TextResourceFromFileProcessor(
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val vamStandaloneFileService: VamStandaloneFileService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val objectMapper: ObjectMapper,
) : ImportFileProcessor {

    override fun canProcessFile(fileToImport: FileToImport): Boolean = fileToImport.getExtension()?.isTextType ?: false

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        context.publishProgress(
            ImportJobProgress(
                "Processing file",
                context.getPathRelativeToVamInstallation(fileToImport),
            ),
        )
        val vamStandaloneFile = vamStandaloneFileService.createOrReplace(fileToImport.toVamStandaloneFile(context))
        val textResourceContent = loadTextResource(fileToImport, context)
        delegatingTextResourceProcessor.processResource(
            fileToImport,
            vamStandaloneFile,
            context,
            textResourceContent,
        ) { stringPathRelativeToCurrentFile ->
            loadTextResource(fileToImport, context, stringPathRelativeToCurrentFile)
        }
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
        val rootNode = try {
            objectMapper.readTree(contentAsString)
        } catch (e: JsonProcessingException) {
            logger().debug("File $finalPath is not a valid JSON file", e)
            null
        }
        return TextResource(finalPath, contentAsString, rootNode)
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
}

@Service
internal class DelegatingTextResourceProcessor(
    private val processors: List<TextResourceProcessor>,
) : TextResourceProcessor {

    init {
        logger().debug("Following text resource processors are available: ${processors.map { it.javaClass.simpleName }}")
    }

    override fun canProcessResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
    ): Boolean = true

    override fun processResource(
        fileToImport: FileToImport,
        vamResourceFile: VamResourceFile,
        importJobContext: ImportJobContext,
        textResource: TextResource,
        textResourceProvider: TextResourceProvider,
    ) = processors.forEach {
        if (it.canProcessResource(fileToImport, vamResourceFile, importJobContext, textResource)) {
            it.processResource(fileToImport, vamResourceFile, importJobContext, textResource, textResourceProvider)
        }
    }
}
