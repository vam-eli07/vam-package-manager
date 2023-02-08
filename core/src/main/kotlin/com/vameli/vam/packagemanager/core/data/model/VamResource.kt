package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node

@Node
data class VamResource(
    @Id
    val resourceId: String,

    val name: String? = null
)
