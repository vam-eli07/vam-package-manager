package com.vameli.vam.packagemanager.core.data.model

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING

const val RELATIONSHIP_CONTAINS_ITEM = "CONTAINS_ITEM"

class VamItemTag(
    @Id
    var tag: String,
)

@Node
class VamItem(
    @Id
    var id: String,
    var displayName: String,
    var type: VamItemType,
    @Relationship("HAS_TAG")
    var tags: Set<VamItemTag>,
    @Relationship(RELATIONSHIP_CONTAINS_ITEM, direction = INCOMING)
    var resourceFiles: Set<VamResourceFile>,
)

enum class VamItemType(val vamItemType: String, val description: String) {
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
