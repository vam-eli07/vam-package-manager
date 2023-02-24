package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamPackageFile
import com.vameli.vam.packagemanager.core.data.model.VamPackageFileRepository
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFile
import com.vameli.vam.packagemanager.core.data.model.VamStandaloneFileRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class VamResourceFileService()

@Service
open class VamStandaloneFileService(
    private val vamStandaloneFileRepository: VamStandaloneFileRepository,
) {

    @Transactional
    open fun createOrReplace(vamStandaloneFile: VamStandaloneFile): VamStandaloneFile {
        vamStandaloneFileRepository.findByIdOrNull(vamStandaloneFile.relativePath)?.let {
            vamStandaloneFile.version = it.version
        }
        return vamStandaloneFileRepository.save(vamStandaloneFile)
    }
}

@Service
open class VamPackageFileService(private val vamPackageFileRepository: VamPackageFileRepository) {
    open fun createOrReplace(vamPackageFile: VamPackageFile): VamPackageFile {
        vamPackageFileRepository.findByIdOrNull(vamPackageFile.relativePath)?.let {
            vamPackageFile.version = it.version
        }
        return vamPackageFileRepository.save(vamPackageFile)
    }
}
