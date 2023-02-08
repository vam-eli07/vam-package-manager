package com.vameli.vam.packagemanager.core.data.infra

import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories

internal const val PROFILE_NEO4J_EMBEDDED = "embedded"

@Configuration
@EnableNeo4jRepositories(
    basePackages = ["com.vameli.vam.packagemanager.core.data.model"]
)
open class Neo4jRepositoriesConfiguration

@Configuration
@Profile(PROFILE_NEO4J_EMBEDDED)
open class EmbeddedNeo4jConfiguration {

    @Bean
    open fun neo4jDriver(
        databaseEnvironment: DatabaseEnvironment
    ): Driver = GraphDatabase.driver(databaseEnvironment.getConnectionString())
}
