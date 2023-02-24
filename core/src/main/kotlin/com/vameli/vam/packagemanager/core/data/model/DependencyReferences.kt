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
    Regex("^([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9_\\-\\[\\]]+)\\.([a-zA-Z0-9]+)(:([a-zA-Z0-9 /_.()\\-\\[\\]]+))?\$")
private val STANDALONE_FILE_REFERENCE_REGEX =
    Regex("^([^/\\\\:]+[/\\\\])+[^/\\\\.]+\\.[^/\\\\.]+$")

const val RELATIONSHIP_PROVIDES_REFERENCE = "PROVIDES_REFERENCE"
const val RELATIONSHIP_DEPENDS_ON = "DEPENDS_ON"

sealed interface DependencyReference {
    companion object {
        fun fromString(string: String): DependencyReference? {
            val packageReferenceMatch = PACKAGE_REFERENCE_REGEX.matchEntire(string)
            return when {
                packageReferenceMatch != null -> fromStringPackageFile(packageReferenceMatch)
                STANDALONE_FILE_REFERENCE_REGEX matches string -> FilesystemDependencyReference(
                    relativePath = string,
                )

                else -> null
            }
        }

        private fun fromStringPackageFile(matchResult: MatchResult): DependencyReference {
            val fileInPackagePath = matchResult.groupValues[5]
            return if (fileInPackagePath.isEmpty()) {
                PackageDependencyReference(
                    authorId = matchResult.groupValues[1],
                    packageId = matchResult.groupValues[2],
                    version = matchResult.groupValues[3],
                )
            } else {
                FileInPackageDependencyReference(
                    authorId = matchResult.groupValues[1],
                    packageId = matchResult.groupValues[2],
                    version = matchResult.groupValues[3],
                    relativePath = fileInPackagePath,
                )
            }
        }
    }
}

data class FileInPackageDependencyReference(
    val authorId: String,
    val packageId: String,
    val version: String,
    val relativePath: String,
) : DependencyReference {
    val isExactVersion: Boolean = version.toIntOrNull()?.let { true } ?: false

    fun toPackageReference(): PackageDependencyReference = PackageDependencyReference(authorId, packageId, version)

    override fun toString(): String = StringBuilder("$authorId.$packageId.$version:").run {
        if (!relativePath.startsWith("/")) {
            append("/$relativePath")
        } else {
            append(relativePath)
        }
        toString()
    }
}

data class PackageDependencyReference(
    val authorId: String,
    val packageId: String,
    val version: String,

) : DependencyReference {
    val isExactVersion: Boolean = version.toIntOrNull()?.let { true } ?: false
    override fun toString(): String = "$authorId.$packageId.$version"
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
    var version: Long = 0,
)

@Repository
interface VamDependencyRepository : Neo4jRepository<VamDependencyReference, DependencyReference>
