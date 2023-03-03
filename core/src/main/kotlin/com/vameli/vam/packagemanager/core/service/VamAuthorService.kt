package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamAuthor
import com.vameli.vam.packagemanager.core.data.model.VamAuthorRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VamAuthorService(private val vamAuthorRepository: VamAuthorRepository) {

    @Transactional
    fun findOrCreate(name: String): VamAuthor = vamAuthorRepository.findByIdOrNull(name)
        ?: vamAuthorRepository.save(VamAuthor(name))
}
