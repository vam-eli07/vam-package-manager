package com.vameli.vam.packagemanager.importer.jobs.processors

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.InvalidJsonException
import com.jayway.jsonpath.JsonPath
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.service.VamDependencyReferenceService
import com.vameli.vam.packagemanager.core.service.VamStandaloneFileService
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension.Companion.getExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import org.springframework.stereotype.Service
import kotlin.io.path.readText

internal interface TextResourceProcessor {
    fun canProcessResource(
        fileToImport: FileToImport,
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext? = null,
    ): Boolean

    fun processResource(
        fileToImport: FileToImport,
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext? = null,
    )
}

@Service
internal class TextResourceFromFileProcessor(
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
    private val vamStandaloneFileService: VamStandaloneFileService,
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
        vamStandaloneFileService.createOrReplace(fileToImport.toVamStandaloneFile(context))
        val resourceContent = fileToImport.path.readText()
        val jsonContext = try {
            JsonPath.parse(resourceContent)
        } catch (e: InvalidJsonException) {
            logger().debug("File ${fileToImport.path} is not a valid JSON file", e)
            null
        }
        delegatingTextResourceProcessor.processResource(fileToImport, resourceContent, context, jsonContext)
    }

    private fun FileToImport.toVamStandaloneFile(context: ImportJobContext): VamStandaloneFile {
        val relativePathAsString = context.getPathRelativeToVamInstallation(this).toString()
        val dependencyReference = DependencyReference.fromString(relativePathAsString)
            ?: throw IllegalArgumentException("Invalid relative path: $relativePathAsString")
        return VamStandaloneFile(
            relativePath = relativePathAsString,
            fileSizeBytes = fileSizeBytes,
            lastModified = lastModified,
            vamDependencyReference = VamDependencyReference(dependencyReference, 0),
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
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext?,
    ): Boolean = true

    override fun processResource(
        fileToImport: FileToImport,
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext?,
    ) {
        processors.forEach {
            if (it.canProcessResource(fileToImport, resourceContent, importJobContext, parsedResourceJson)) {
                it.processResource(fileToImport, resourceContent, importJobContext, parsedResourceJson)
            }
        }
    }
}
