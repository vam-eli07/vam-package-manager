package com.vameli.vam.packagemanager.core.data.model

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.annotation.Version
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

const val RELATIONSHIP_CONTAINS_ITEM = "CONTAINS_ITEM"

class VamItemTag(
    @Id
    var tag: String,
    @Version
    var version: Long = 0,
) {
    override fun toString(): String {
        return "VamItemTag(tag='$tag', version=$version)"
    }
}

@Node
class VamItem(
    @Id
    var id: String,
    @Version
    var version: Long = 0,
    var displayName: String,
    var type: VamItemType,
    @Relationship(RELATIONSHIP_CREATED_BY)
    var author: VamAuthor,
    @Relationship("HAS_TAG")
    var tags: MutableSet<VamItemTag>,
    @Relationship(RELATIONSHIP_CONTAINS_ITEM, direction = INCOMING)
    var resourceFiles: MutableSet<VamResourceFile> = mutableSetOf(),
    @Relationship(RELATIONSHIP_DEPENDS_ON)
    var dependencies: MutableSet<VamDependencyReference> = mutableSetOf(),
)

enum class VamItemType(@JsonValue val vamItemType: String, val description: String) {
    HAIR_FEMALE("HairFemale", "Hair (Female)"),
    HAIR_MALE("HairMale", "Hair (Male)"),
    CLOTHING_FEMALE("ClothingFemale", "Clothing (Female)"),
    CLOTHING_MALE("ClothingMale", "Clothing (Male)"),
    SCENE("Scene", "Scene"),
    APPEARANCE("Appearance", "Appearance"),
    FULL_LOOK("FullLook", "Full Look"),
    ;

    companion object {
        private val cache = values().associateBy(VamItemType::vamItemType)

        fun fromVamItemType(vamItemType: String): VamItemType? = cache[vamItemType]
    }
}

interface VamItemDependenciesProjection {
    var dependencies: MutableSet<VamDependencyReference>
}

interface VamItemResourceFilesProjection {
    var resourceFiles: MutableSet<ResourceFileIdProjection>

    interface ResourceFileIdProjection {
        var relativePath: String
    }
}

@Repository
interface VamItemRepository : Neo4jRepository<VamItem, String>

@Repository
interface VamItemTagRepository : Neo4jRepository<VamItemTag, String>
