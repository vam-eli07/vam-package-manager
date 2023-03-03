package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.data.model.VamPackageFileRepository
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFileRepository
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
class VamPackageFileService(private val vamPackageFileRepository: VamPackageFileRepository) {
    fun createOrReplace(vamPackageFile: VamPackageFile): VamPackageFile {
        vamPackageFileRepository.findByIdOrNull(vamPackageFile.relativePath)?.let {
            vamPackageFile.version = it.version
        }
        return vamPackageFileRepository.save(vamPackageFile)
    }
}
