package com.vameli.vam.packagemanager.core.data.model

import org.neo4j.driver.Value
import org.neo4j.driver.internal.value.StringValue
import org.springframework.data.annotation.Version
import org.springframework.data.neo4j.core.convert.ConvertWith
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

private val PACKAGE_REFERENCE_REGEX =
    Regex("^([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9]+):([a-zA-Z0-9 /_.()\\-\\[\\]]+)\$")
private val FILESYSTEM_REFERENCE_REGEX =
    Regex("^([^/\\\\:]+[/\\\\])+[^/\\\\.]+\\.[^/\\\\.]+$")

const val RELATIONSHIP_HAS_REFERENCE = "HAS_REFERENCE"
const val RELATIONSHIP_DEPENDS_ON = "DEPENDS_ON"

sealed interface DependencyReference {
    companion object {
        fun fromString(string: String): DependencyReference? {
            val packageReferenceMatch = PACKAGE_REFERENCE_REGEX.matchEntire(string)
            if (packageReferenceMatch != null) {
                return PackageDependencyReference(
                    authorId = packageReferenceMatch.groupValues[1],
                    packageId = packageReferenceMatch.groupValues[2],
                    version = packageReferenceMatch.groupValues[3],
                    relativePath = packageReferenceMatch.groupValues[4],
                )
            }
            return if (FILESYSTEM_REFERENCE_REGEX matches string) {
                FilesystemDependencyReference(
                    relativePath = string,
                )
            } else {
                null
            }
        }
    }
}

data class PackageDependencyReference(
    val authorId: String,
    val packageId: String,
    val version: String,
    val relativePath: String? = null,
) : DependencyReference {
    val isExactVersion: Boolean = version.toIntOrNull()?.let { true } ?: false
    override fun toString(): String = "$authorId.$packageId.$version" + relativePath?.let { ":/$it" }.orEmpty()
}

data class FilesystemDependencyReference(
    val relativePath: String,
) : DependencyReference {
    override fun toString(): String = relativePath
}

class DependencyReferenceConverter : Neo4jPersistentPropertyConverter<DependencyReference> {
    override fun write(source: DependencyReference?): Value = StringValue(source.toString())

    override fun read(source: Value): DependencyReference? {
        if (source.isNull) return null
        val sourceString = source.asString()
        return DependencyReference.fromString(sourceString)
            ?: throw IllegalArgumentException("Invalid reference id: $sourceString")
    }
}

@Node
data class VamDependencyReference(
    @Id
    @ConvertWith(converter = DependencyReferenceConverter::class)
    var dependencyReference: DependencyReference,
    @Version
    var version: Long,
)

@Repository
interface VamDependencyRepository : Neo4jRepository<VamDependencyReference, DependencyReference>
