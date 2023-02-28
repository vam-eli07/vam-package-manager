package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamItem
import com.vameli.vam.packagemanager.core.data.model.VamItemRepository
import org.springframework.stereotype.Service

@Service
class VamItemService(private val vamItemRepository: VamItemRepository) {
    fun saveItem(vamItem: VamItem): VamItem = vamItemRepository.save(vamItem)
}
