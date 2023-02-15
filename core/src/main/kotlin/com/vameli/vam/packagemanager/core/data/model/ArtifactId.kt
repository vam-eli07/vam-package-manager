package com.vameli.vam.packagemanager.core.data.model

data class ArtifactId(
    val authorId: String,
    val packageId: String,
    val version: String,
    val relativePath: String? = null,
) {
    val isExactVersion: Boolean = version.toIntOrNull()?.let { true } ?: false
    override fun toString(): String = "$authorId.$packageId.$version" + relativePath?.let { ":$it" }.orEmpty()
}

private val DEPENDENCY_REF_REGEX =
    Regex("^([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9]+):([a-zA-Z0-9 /_.()\\-\\[\\]]+)\$")

fun String.matchDependencyId(): ArtifactId? = DEPENDENCY_REF_REGEX.matchEntire(this)?.let { matchResult ->
    return ArtifactId(
        authorId = matchResult.groupValues[1],
        packageId = matchResult.groupValues[2],
        version = matchResult.groupValues[3],
        relativePath = matchResult.groupValues[4],
    )
}
