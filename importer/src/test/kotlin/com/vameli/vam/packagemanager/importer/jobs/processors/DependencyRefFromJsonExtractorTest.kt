package com.vameli.vam.packagemanager.importer.jobs.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.vameli.vam.packagemanager.core.data.model.DependencyReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

private const val JSON = """
{
   "something":{
      "hair":[
         {
            "id":"NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Top Bun.vam",
            "internalId":"NoStage3:Top Bun",
            "enabled":"true"
         },
         {
            "id":"NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Long Upswept.vam",
            "internalId":"NoStage3:Long Upswept",
            "enabled":"true"
         },
         {
            "id":"NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Some Other.vam",
            "internalId":"NoStage3:Some Other",
            "enabled":"true"
         }
      ]
   },
   "somethingElse":{
      "MeshedVR.3PointLightSetup.1:/Custom/SubScene/MeshedVR/3PointLightSetup/3 Point Light Setup UI Hidden.json":{
         "dependencies":{
            "AcidBubbles.Timeline.218:/Custom/Scripts/AcidBubbles/Timeline/VamTimeline.AtomAnimation.cslist":{
               "dependencies":{
                  
               }
            }
         }
      }
   },
   "somethingTotallyDifferent":{
      "MeshedVR.3PointLightSetup.2:/Custom/SubScene/MeshedVR/3PointLightSetup/3 Point Light Setup UI Hidden.json":{
         "dependencies":{
            "AcidBubbles.Timeline.219:/Custom/Scripts/AcidBubbles/Timeline/VamTimeline.AtomAnimation.cslist":{
               "dependencies":{
                  
               }
            }
         }
      }
   }
}
"""

private val EXPECTED_PACKAGE_DEPS = setOf(
    DependencyReference.fromString("NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Top Bun.vam"),
    DependencyReference.fromString("NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Long Upswept.vam"),
    DependencyReference.fromString("NoStage3.Hair_Long_Upswept_Top_Bun.1:/Custom/Hair/Female/NoStage3/Long Upswept Top Bun/Some Other.vam"),
    DependencyReference.fromString("MeshedVR.3PointLightSetup.1:/Custom/SubScene/MeshedVR/3PointLightSetup/3 Point Light Setup UI Hidden.json"),
    DependencyReference.fromString("MeshedVR.3PointLightSetup.2:/Custom/SubScene/MeshedVR/3PointLightSetup/3 Point Light Setup UI Hidden.json"),
    DependencyReference.fromString("AcidBubbles.Timeline.218:/Custom/Scripts/AcidBubbles/Timeline/VamTimeline.AtomAnimation.cslist"),
    DependencyReference.fromString("AcidBubbles.Timeline.219:/Custom/Scripts/AcidBubbles/Timeline/VamTimeline.AtomAnimation.cslist"),
)

internal class DependencyRefFromJsonExtractorTest {

    private val objectMapper = ObjectMapper()

    @Test
    fun `extracts all references from json root node`() {
        val extractor = DependencyRefFromJsonExtractor(objectMapper)

        val dependencyReferences = extractor.extractDependencyReferences(objectMapper.readTree(JSON))

        assertThat(dependencyReferences).containsExactlyInAnyOrderElementsOf(EXPECTED_PACKAGE_DEPS)
    }

    @Test
    fun `extracts all references from json string`() {
        val extractor = DependencyRefFromJsonExtractor(objectMapper)

        val dependencyReferences = extractor.extractDependencyReferences(JSON)

        assertThat(dependencyReferences).containsExactlyInAnyOrderElementsOf(EXPECTED_PACKAGE_DEPS)
    }
}
