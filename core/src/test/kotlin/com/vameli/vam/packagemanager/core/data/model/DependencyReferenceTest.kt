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
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, true",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, latest, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, false",
        ],
    )
    fun `matches valid file in package reference`(
        value: String,
        expectedAuthorId: String,
        expectedPackageId: String,
        expectedVersion: String,
        expectedRelativePath: String,
        expectedIsExactVersion: Boolean,
    ) {
        val dependencyReference = DependencyReference.fromString(value) ?: fail("Dependency ID did not match: $value")

        require(dependencyReference is FileInPackageDependencyReference)
        assertSoftly { softly ->
            softly.assertThat(dependencyReference.authorId).describedAs("author ID").isEqualTo(expectedAuthorId)
            softly.assertThat(dependencyReference.packageId).describedAs("package ID").isEqualTo(expectedPackageId)
            softly.assertThat(dependencyReference.version).describedAs("version").isEqualTo(expectedVersion)
            softly.assertThat(dependencyReference.relativePath).describedAs("relative path")
                .isEqualTo(expectedRelativePath)
            softly.assertThat(dependencyReference.isExactVersion).describedAs("is exact version")
                .isEqualTo(expectedIsExactVersion)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1, YameteOuji, OL01_Set, 1, true",
            "YameteOuji.OL01_Set.latest, YameteOuji, OL01_Set, latest, false",
        ],
    )
    fun `matches valid package reference`(
        value: String,
        expectedAuthorId: String,
        expectedPackageId: String,
        expectedVersion: String,
        expectedIsExactVersion: Boolean,
    ) {
        val dependencyReference = DependencyReference.fromString(value) ?: fail("Dependency ID did not match: $value")

        require(dependencyReference is PackageDependencyReference)
        assertSoftly { softly ->
            softly.assertThat(dependencyReference.authorId).describedAs("author ID").isEqualTo(expectedAuthorId)
            softly.assertThat(dependencyReference.packageId).describedAs("package ID").isEqualTo(expectedPackageId)
            softly.assertThat(dependencyReference.version).describedAs("version").isEqualTo(expectedVersion)
            softly.assertThat(dependencyReference.isExactVersion).describedAs("is exact version").isEqualTo(expectedIsExactVersion)
        }
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, 1, Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01 Skirt.vam, YameteOuji, OL01_Set, 1, /Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01 Skirt.vam",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji, OL01_Set, latest, Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `file in package reference creates correct toString()`(
        expectedValue: String,
        authorId: String,
        packageId: String,
        version: String,
        relativePath: String,
    ) {
        val vamReference = FileInPackageDependencyReference(authorId, packageId, version, relativePath)

        assertThat(vamReference.toString()).isEqualTo(expectedValue)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "YameteOuji.OL01_Set.1:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji.OL01_Set.1",
            "YameteOuji.OL01_Set.latest:/Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji.OL01_Set.latest",
        ],
    )
    fun `file in package reference creates correct package reference`(fileInPackageRefString: String, packageRefString: String) {
        val fileInPackageRef = DependencyReference.fromString(fileInPackageRefString) as FileInPackageDependencyReference

        val packageRef = fileInPackageRef.toPackageReference()

        val expectedPackageRef = DependencyReference.fromString(packageRefString) as PackageDependencyReference
        assertThat(packageRef).isEqualTo(expectedPackageRef)
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
    @ValueSource(
        strings = [
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `filesystem reference creates correct toString()`(value: String) {
        val vamReference = FilesystemDependencyReference(value)

        assertThat(vamReference.toString()).isEqualTo(value)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam, YameteOuji.OL01_Set.1, YameteOuji.OL01_Set.1:Custom/Clothing/Female/YameteOuji/YameteOuji_OL01_Skirt/OL01_Skirt.vam",
        ],
    )
    fun `filesystem reference creates correct file in package reference`(localFilePath: String, packageRefString: String, expectedRefString: String) {
        val filesystemRef = FilesystemDependencyReference(localFilePath)
        val packageRef = DependencyReference.fromString(packageRefString) as PackageDependencyReference

        val fileInPackageRef = filesystemRef.toFileInPackageReference(packageRef)

        val expectedFileInPackageRef = DependencyReference.fromString(expectedRefString) as FileInPackageDependencyReference
        assertThat(fileInPackageRef).isEqualTo(expectedFileInPackageRef)
    }
}
