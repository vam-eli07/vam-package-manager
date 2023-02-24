package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamItemRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VamItemService(private val vamItemRepository: VamItemRepository) {
    fun createOrReplace(vamItem: VamItem) = vamItemRepository.findByIdOrNull(vamItem.id) ?: vamItemRepository.save(vamItem)
}
