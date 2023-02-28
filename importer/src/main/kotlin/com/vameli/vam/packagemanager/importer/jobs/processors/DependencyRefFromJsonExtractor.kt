package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool

private const val FORK_JOIN_THRESHOLD = 2

@Service
class DependencyRefFromJsonExtractor(private val objectMapper: ObjectMapper) : DisposableBean {

    private val forkJoinPool = ForkJoinPool(Runtime.getRuntime().availableProcessors())

    fun extractDependencyReferences(rootNode: JsonNode): Collection<DependencyReference> = when {
        rootNode.isArray -> extractDependencyReferencesFromArray(rootNode as ArrayNode)
        rootNode.isObject -> extractDependencyReferencesFromObject(rootNode as ObjectNode)
        rootNode.isTextual -> DependencyReference.fromString(rootNode.textValue())?.let { listOf(it) } ?: emptyList()
        else -> emptyList()
    }

    fun extractDependencyReferences(jsonContent: String): Collection<DependencyReference> =
        extractDependencyReferences(objectMapper.readTree(jsonContent))

    override fun destroy() {
        forkJoinPool.shutdown()
    }

    private fun extractDependencyReferencesFromArray(node: ArrayNode): Collection<DependencyReference> {
        val itemCount = node.size()
        if (itemCount <= FORK_JOIN_THRESHOLD) {
            return node.flatMap { extractDependencyReferences(it) }
        }
        val tasks = node.map { Callable { extractDependencyReferences(it) } }
        return forkJoinPool.invokeAll(tasks).flatMap { it.get() }
    }

    private fun extractDependencyReferencesFromObject(node: ObjectNode): Collection<DependencyReference> =
        mutableListOf<DependencyReference>().apply {
            val size = node.size()
            val callableTasks = mutableListOf<Callable<Collection<DependencyReference>>>()
            node.fields().forEach { (key, value) ->
                DependencyReference.fromString(key)?.let { add(it) }
                if (size <= FORK_JOIN_THRESHOLD) {
                    addAll(extractDependencyReferences(value))
                } else {
                    callableTasks.add(Callable { extractDependencyReferences(value) })
                }
            }
            if (callableTasks.isNotEmpty()) {
                addAll(forkJoinPool.invokeAll(callableTasks).flatMap { it.get() })
            }
        }
}
