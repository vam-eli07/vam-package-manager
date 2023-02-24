package com.vameli.vam.packagemanager.importer.vammodel

import com.vameli.vam.packagemanager.core.data.model.VamItemType

data class VamVamFile(
    val itemType: VamItemType,
    val uid: String,
    val displayName: String,
    val creatorName: String,
    val tags: String,
)
