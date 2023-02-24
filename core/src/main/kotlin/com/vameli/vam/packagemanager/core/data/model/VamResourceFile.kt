package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.annotation.Version
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Node
abstract class VamResourceFile(
    @Id
    var relativePath: String,
    @Version
    var version: Long? = null,
    var fileSizeBytes: Long,
    var lastModified: Instant,
)

@Node
class VamPackageFile(
    relativePath: String,
    version: Long? = null,
    fileSizeBytes: Long,
    lastModified: Instant,
    var licenseType: String? = null,

    @Relationship(RELATIONSHIP_PROVIDES_REFERENCE)
    var vamDependencyReference: VamDependencyReference,

    @Relationship(RELATIONSHIP_CREATED_BY)
    var author: VamAuthor,

    @Relationship(RELATIONSHIP_CONTAINS_ITEM)
    var items: Set<VamItem>,
) : VamResourceFile(relativePath, version, fileSizeBytes, lastModified)

@Node
class VamStandaloneFile(
    relativePath: String,
    version: Long? = null,
    fileSizeBytes: Long,
    lastModified: Instant,

    @Relationship(RELATIONSHIP_PROVIDES_REFERENCE)
    var vamDependencyReference: VamDependencyReference,

    @Relationship(RELATIONSHIP_CONTAINS_ITEM)
    var item: VamItem? = null,
) : VamResourceFile(relativePath, version, fileSizeBytes, lastModified)

@Repository
interface VamResourceFileRepository : Neo4jRepository<VamResourceFile, String>

@Repository
interface VamStandaloneFileRepository : Neo4jRepository<VamStandaloneFile, String>

@Repository
interface VamPackageFileRepository : Neo4jRepository<VamPackageFile, String>
