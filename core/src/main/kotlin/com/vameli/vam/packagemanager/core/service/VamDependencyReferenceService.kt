package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamDependencyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class VamDependencyReferenceService(private val vamDependencyRepository: VamDependencyRepository) {

    @Transactional
    open fun findOrCreate(dependencyReference: DependencyReference): VamDependencyReference =
        vamDependencyRepository.findByIdOrNull(dependencyReference)
            ?: vamDependencyRepository.save(VamDependencyReference(dependencyReference))
}
