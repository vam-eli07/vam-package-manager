package com.vameli.vam.packagemanager.core.data.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class ArtifactIdTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "YameteOuji",
            "YameteOuji.OL01_Set",
            "YameteOuji.OL01_Set.1",
            "YameteOuji.OL01_Set.1:",
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `does not match invalid dependency ids`(value: String) {
        assertThat(value.matchDependencyId()).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, latest, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `matches valid dependency IDs`(
        value: String,
        expectedAuthorId: String,
        expectedPackageId: String,
        expectedVersion: String,
        expectedRelativePath: String,
    ) {
        val dependencyId = value.matchDependencyId() ?: fail("Dependency ID did not match: $value")

        assertSoftly { softly ->
            softly.assertThat(dependencyId.authorId).describedAs("author ID").isEqualTo(expectedAuthorId)
            softly.assertThat(dependencyId.packageId).describedAs("package ID").isEqualTo(expectedPackageId)
            softly.assertThat(dependencyId.version).describedAs("version").isEqualTo(expectedVersion)
            softly.assertThat(dependencyId.relativePath).describedAs("relative path").isEqualTo(expectedRelativePath)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, latest, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `creates correct toString()`(
        expectedValue: String,
        authorId: String,
        packageId: String,
        version: String,
        relativePath: String,
    ) {
        val artifactId = ArtifactId(authorId, packageId, version, relativePath)

        assertThat(artifactId.toString()).isEqualTo(expectedValue)
    }
}
