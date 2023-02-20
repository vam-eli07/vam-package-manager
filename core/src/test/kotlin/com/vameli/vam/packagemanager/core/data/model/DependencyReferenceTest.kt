package com.vameli.vam.packagemanager.core.data.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class DependencyReferenceTest {

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "YameteOuji",
            "YameteOuji.OL01_Set",
            "YameteOuji.OL01_Set.1",
            "YameteOuji.OL01_Set.1:",
            "YameteOuji.OL01_Set.latest:\\Custom\\Clothing\\Female\\YameteOuji\\YameteOuji_OL01_Skirt\\OL01_Skirt.vam, YameteOuji, OL01_Set, latest, \\Custom\\Clothing\\Female\\YameteOuji\\YameteOuji_OL01_Skirt\\OL01_Skirt.vam",
            "/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `does not match invalid dependency ids`(value: String) {
        assertThat(DependencyReference.fromString(value)).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `matches valid package reference`(
        value: String,
        expectedAuthorId: String,
        expectedPackageId: String,
        expectedVersion: String,
        expectedRelativePath: String,
    ) {
        val dependencyReference = DependencyReference.fromString(value) ?: fail("Dependency ID did not match: $value")

        require(dependencyReference is PackageDependencyReference)
        assertSoftly { softly ->
            softly.assertThat(dependencyReference.authorId).describedAs("author ID").isEqualTo(expectedAuthorId)
            softly.assertThat(dependencyReference.packageId).describedAs("package ID").isEqualTo(expectedPackageId)
            softly.assertThat(dependencyReference.version).describedAs("version").isEqualTo(expectedVersion)
            softly.assertThat(dependencyReference.relativePath).describedAs("relative path")
                .isEqualTo(expectedRelativePath)
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "Custom\\Clothing\\Female\\YameteOuji\\YameteOuji_OL01_Skirt\\OL01_Skirt.vam",
        ],
    )
    fun `matches valid filesystem reference`(value: String) {
        val dependencyReference = DependencyReference.fromString(value)

        assertThat(dependencyReference).isNotNull
        require(dependencyReference is FilesystemDependencyReference)
        assertThat(dependencyReference.relativePath).describedAs("relative path").isEqualTo(value)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01 Skirt.vam, YameteOuji, OL01_Set, 1, Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01 Skirt.vam",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, latest, Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `package reference creates correct toString()`(
        expectedValue: String,
        authorId: String,
        packageId: String,
        version: String,
        relativePath: String,
    ) {
        val vamReference = PackageDependencyReference(authorId, packageId, version, relativePath)

        assertThat(vamReference.toString()).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01 Skirt.vam",
        ],
    )
    fun `filesystem reference creates correct toString()`(value: String) {
        val vamReference = FilesystemDependencyReference(value)

        assertThat(vamReference.toString()).isEqualTo(value)
    }
}
