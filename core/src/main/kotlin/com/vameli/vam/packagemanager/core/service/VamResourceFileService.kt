package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.PackageDependencyReference
import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.data.model.VamPackageFileDependenciesProjection
import com.vameli.vam.packagemanager.core.data.model.VamPackageFileRepository
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFileRepository
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VamResourceFileService()

@Service
@Transactional
class VamStandaloneFileService(
    private val vamStandaloneFileRepository: VamStandaloneFileRepository,
) {

    fun createOrReplace(vamStandaloneFile: VamStandaloneFile): VamStandaloneFile {
        vamStandaloneFileRepository.findByIdOrNull(vamStandaloneFile.relativePath)?.let {
            vamStandaloneFile.version = it.version
        }
        return vamStandaloneFileRepository.save(vamStandaloneFile)
    }

    fun createOrReplace(vamStandaloneFiles: Collection<VamStandaloneFile>): Collection<VamStandaloneFile> {
        val ids = vamStandaloneFiles.map { it.relativePath }
        val existingStandaloneFiles = vamStandaloneFileRepository.findAllById(ids)
        val existingStandaloneFileIds = existingStandaloneFiles.map { it.relativePath }.toSet()
        val nonExistingStandaloneFiles = vamStandaloneFiles.filter { !existingStandaloneFileIds.contains(it.relativePath) }
        val newStandaloneFiles = vamStandaloneFileRepository.saveAll(nonExistingStandaloneFiles)
        return (existingStandaloneFiles + newStandaloneFiles)
    }
}

@Service
@Transactional
class VamPackageFileService(
    private val vamPackageFileRepository: VamPackageFileRepository,
    private val dependencyReferenceService: VamDependencyReferenceService,
    private val neo4jTemplate: Neo4jTemplate,
) {
    fun createOrReplace(vamPackageFile: VamPackageFile): VamPackageFile {
        vamPackageFileRepository.findByIdOrNull(vamPackageFile.relativePath)?.let {
            vamPackageFile.version = it.version
        }
        return vamPackageFileRepository.save(vamPackageFile)
    }

    fun setPackageDependencies(vamPackageFile: VamPackageFile, dependencies: Set<PackageDependencyReference>) {
        val vamDependencyReferences = dependencyReferenceService.findOrCreate(dependencies)
        vamPackageFile.packageDependencies.clear()
        vamPackageFile.packageDependencies.addAll(vamDependencyReferences)
        neo4jTemplate.saveAs(vamPackageFile, VamPackageFileDependenciesProjection::class.java)
    }
}
