package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VamDependencyReferenceService(private val vamDependencyRepository: VamDependencyRepository) {

    fun findOrCreate(dependencyReference: DependencyReference): VamDependencyReference =
        vamDependencyRepository.findByIdOrNull(dependencyReference)
            ?: vamDependencyRepository.save(VamDependencyReference(dependencyReference))

    fun findOrCreate(dependencyReferences: Collection<DependencyReference>): Set<VamDependencyReference> {
        val existingDependencies = vamDependencyRepository.findAllById(dependencyReferences)
        val existingDependencyReferences = existingDependencies.map { it.dependencyReference }.toSet()
        val nonExistingDependencyReferences = dependencyReferences.filter { !existingDependencyReferences.contains(it) }
        val newDependencies = nonExistingDependencyReferences.map { VamDependencyReference(it) }
        return (existingDependencies + vamDependencyRepository.saveAll(newDependencies)).toSet()
    }
}
