package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.VamItemTag
import com.vameli.vam.packagemanager.core.data.model.VamItemTagRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VamTagService(private val vamItemTagRepository: VamItemTagRepository) {

    fun getOrCreateTag(tagName: String): VamItemTag = vamItemTagRepository.findByIdOrNull(tagName) ?: vamItemTagRepository.save(VamItemTag(tagName))

    fun getOrCreateTags(tagNames: Collection<String>): Set<VamItemTag> {
        val existingTags = vamItemTagRepository.findAllById(tagNames)
        val existingTagNames = existingTags.map { it.tag }.toSet()
        val nonExistingTagNames = tagNames.filter { !existingTagNames.contains(it) }
        val newTags = nonExistingTagNames.map { VamItemTag(it) }
        return (existingTags + vamItemTagRepository.saveAll(newTags)).toSet()
    }

    fun getOrCreateTags(tagNamesCommaSeparated: String): Set<VamItemTag> = getOrCreateTags(
        tagNamesCommaSeparated.split(',').mapNotNull { tag ->
            tag.trim().takeIf { it.isNotBlank() }
        },
    )
}
