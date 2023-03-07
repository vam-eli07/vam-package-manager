package com.vameli.vam.packagemanager.importer.vammodel

data class VamMetaJson(
    val creatorName: String,
    val packageName: String,
    override val licenseType: String? = null,
    val description: String? = null,
    val instructions: String? = null,
    val promotionalLink: String? = null,
    override val dependencies: Map<String, VamMetaJsonDependency> = emptyMap(),
) : HasVamMetaJsonDependency

data class VamMetaJsonDependency(
    override val licenseType: String? = null, // TODO process this somehow
    override val dependencies: Map<String, VamMetaJsonDependency> = emptyMap(),
) : HasVamMetaJsonDependency

interface HasVamMetaJsonDependency {
    val licenseType: String?
    val dependencies: Map<String, VamMetaJsonDependency>
}
