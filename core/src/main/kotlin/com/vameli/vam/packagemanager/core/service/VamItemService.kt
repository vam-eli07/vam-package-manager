package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamItemDependenciesProjection
import com.vameli.vam.packagemanager.core.data.model.VamItemRepository
import com.vameli.vam.packagemanager.core.data.model.VamItemResourceFilesProjection
import com.vameli.vam.packagemanager.core.data.model.VamResourceFile
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VamItemService(
    private val vamItemRepository: VamItemRepository,
    private val vamAuthorService: VamAuthorService,
    private val vamTagService: VamTagService,
    private val vamDependencyReferenceService: VamDependencyReferenceService,
    private val neo4jTemplate: Neo4jTemplate,
) {
    fun saveItem(vamItem: VamItem): VamItem {
        vamItem.author = vamAuthorService.findOrCreate(vamItem.author.name)
        vamItem.tags = vamTagService.getOrCreateTags(vamItem.tags.map { it.tag }).toMutableSet()
        return vamItemRepository.save(vamItem)
    }

    fun addItemResourceFiles(vamItem: VamItem, resourceFiles: Collection<VamResourceFile>) {
        val existingResourceFileIds = vamItem.resourceFiles.map { it.relativePath }.toSet()
        val newResourceFiles = resourceFiles.filter { !existingResourceFileIds.contains(it.relativePath) }
        vamItem.resourceFiles.addAll(newResourceFiles)
        neo4jTemplate.saveAs(vamItem, VamItemResourceFilesProjection::class.java)
    }

    fun setItemDependencies(vamItem: VamItem, dependencyRefs: Collection<DependencyReference>) {
        val vamDependencyReferences = vamDependencyReferenceService.findOrCreate(dependencyRefs)
        vamItem.dependencies.clear()
        vamItem.dependencies.addAll(vamDependencyReferences)
        neo4jTemplate.saveAs(vamItem, VamItemDependenciesProjection::class.java)
    }
}
