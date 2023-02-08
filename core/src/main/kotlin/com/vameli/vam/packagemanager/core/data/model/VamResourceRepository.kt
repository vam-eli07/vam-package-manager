package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@Repository
interface VamResourceRepository : Neo4jRepository<VamResource, String> {
    fun findByName(name: String): VamResource?
    fun findAllByName(name: String): List<VamResource>
}
