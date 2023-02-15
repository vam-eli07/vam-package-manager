package com.vameli.vam.packagemanager.importer.jobs.processors

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.InvalidJsonException
import com.jayway.jsonpath.JsonPath
import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import com.vameli.vam.packagemanager.importer.jobs.FileToImport
import com.vameli.vam.packagemanager.importer.jobs.ImportFileExtension.Companion.getExtension
import com.vameli.vam.packagemanager.importer.jobs.ImportJobContext
import org.springframework.stereotype.Service
import kotlin.io.path.readText

internal interface ImportFileProcessor {
    fun processFile(fileToImport: FileToImport, context: ImportJobContext)
}

@Service
internal class DelegatingImportFileProcessor(
    private val processors: List<ImportFileProcessor>,
) : ImportFileProcessor {

    init {
        logger().debug("Following file processors are available: ${processors.map { it.javaClass.simpleName }}")
    }

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        processors.forEach { it.processFile(fileToImport, context) }
    }
}

@Service
internal class TextResourceFromFileProcessor(
    private val delegatingTextResourceProcessor: DelegatingTextResourceProcessor,
) : ImportFileProcessor {

    override fun processFile(fileToImport: FileToImport, context: ImportJobContext) {
        val isTextResource = fileToImport.getExtension()?.isTextType ?: false
        if (!isTextResource) {
            return
        }
        context.publishProgress(
            ImportJobProgress(
                "Processing file",
                context.getPathRelativeToVamInstallation(fileToImport),
            ),
        )
        val resourceContent = fileToImport.path.readText()
        val jsonContext = try {
            JsonPath.parse(resourceContent)
        } catch (e: InvalidJsonException) {
            logger().debug("File ${fileToImport.path} is not a valid JSON file", e)
            null
        }
        delegatingTextResourceProcessor.processResource(fileToImport, resourceContent, context, jsonContext)
    }
}

internal interface TextResourceProcessor {
    fun processResource(
        fileToImport: FileToImport,
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext? = null,
    )
}

@Service
internal class DelegatingTextResourceProcessor(
    private val processors: List<TextResourceProcessor>,
) : TextResourceProcessor {

    init {
        logger().debug("Following text resource processors are available: ${processors.map { it.javaClass.simpleName }}")
    }

    override fun processResource(
        fileToImport: FileToImport,
        resourceContent: String,
        importJobContext: ImportJobContext,
        parsedResourceJson: DocumentContext?,
    ) {
        processors.forEach { it.processResource(fileToImport, resourceContent, importJobContext, parsedResourceJson) }
    }
}
