package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import java.time.Instant

@Node
abstract class VamResourceFile(
    @Id var relativePath: String,
    var fileSizeBytes: Long,
    var lastModified: Instant,
)

@Node
class VamPackageFile(
    relativePath: String,
    fileSizeBytes: Long,
    lastModified: Instant,
    var licenseType: String,

    @Relationship(RELATIONSHIP_CONTAINS_ITEM)
    var items: Set<VamItem>,
) : VamResourceFile(relativePath, fileSizeBytes, lastModified)

@Node
class VamStandaloneFile(
    relativePath: String,
    fileSizeBytes: Long,
    lastModified: Instant,

    @Relationship(RELATIONSHIP_CONTAINS_ITEM)
    var item: VamItem,
) : VamResourceFile(relativePath, fileSizeBytes, lastModified)
