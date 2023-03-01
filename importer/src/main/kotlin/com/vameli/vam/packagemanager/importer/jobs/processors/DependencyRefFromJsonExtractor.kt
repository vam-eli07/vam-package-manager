package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import org.springframework.stereotype.Service

@Service
class DependencyRefFromJsonExtractor(private val objectMapper: ObjectMapper) {
    fun extractDependencyReferences(rootNode: JsonNode): Set<DependencyReference> = when {
        rootNode.isArray -> rootNode.flatMap { extractDependencyReferences(it) }.toSet()
        rootNode.isObject -> extractDependencyReferencesFromObject(rootNode as ObjectNode)
        rootNode.isTextual -> DependencyReference.fromString(rootNode.textValue())?.let { setOf(it) } ?: emptySet()
        else -> emptySet()
    }

    fun extractDependencyReferences(jsonContent: String): Set<DependencyReference> =
        extractDependencyReferences(objectMapper.readTree(jsonContent))

    private fun extractDependencyReferencesFromObject(node: ObjectNode): Set<DependencyReference> =
        mutableSetOf<DependencyReference>().apply {
            node.fields().forEach { (key, value) ->
                DependencyReference.fromString(key)?.let { add(it) }
                addAll(extractDependencyReferences(value))
            }
        }
}
