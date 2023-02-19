package com.vameli.vam.packagemanager.core.service

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
    open fun createOrReplace(vamStandaloneFile: VamStandaloneFile) {
        vamStandaloneFileRepository.findByIdOrNull(vamStandaloneFile.relativePath)?.let {
            vamStandaloneFile.version = it.version
        }
        vamStandaloneFileRepository.save(vamStandaloneFile)
    }
}

@Service
class VamPackageFileService(private val vamPackageFileRepository: VamPackageFileRepository)
