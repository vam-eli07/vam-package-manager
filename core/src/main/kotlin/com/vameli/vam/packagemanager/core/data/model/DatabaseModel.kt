package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@Node
data class DatabaseModel(@Id val version: Int)

@Repository
interface DatabaseModelRepository : Neo4jRepository<DatabaseModel, Int>
