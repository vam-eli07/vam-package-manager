package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.annotation.Version
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

const val RELATIONSHIP_CREATED_BY = "CREATED_BY"

@Node
class VamAuthor(
    @Id
    var name: String,
    @Version
    var version: Long = 0,
)

@Repository
interface VamAuthorRepository : Neo4jRepository<VamAuthor, String>
