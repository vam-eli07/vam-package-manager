package com.vameli.vam.packagemanager.importer.vammodel

data class VamMetaJson(
    val creatorName: String,
    val packageName: String,
    val licenseType: String? = null,
    val description: String? = null,
    val instructions: String? = null,
    val promotionalLink: String? = null,
    val dependencies: Map<String, VamMetaJsonDependency> = emptyMap(),
)

data class VamMetaJsonDependency(
    val licenseType: String? = null,
    val dependencies: Map<String, VamMetaJsonDependency> = emptyMap(),
)
