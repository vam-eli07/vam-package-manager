plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version Versions.KOTLIN
    id("io.spring.dependency-management") version "1.1.0"
    id("org.springframework.boot") version Versions.SPRING_BOOT apply false
    id("org.jetbrains.kotlin.plugin.spring") version Versions.KOTLIN
    idea
}

allprojects {

    group = "com.vameli.vam.package-manager"
    version = "1.0.1"
//    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    apply {
        plugin("kotlin")
        plugin("io.spring.dependency-management")
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${Versions.SPRING_BOOT}")
        }
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.slf4j:slf4j-api")
        testImplementation("io.mockk:mockk:1.13.4")
    }

    kotlin {
        jvmToolchain(17) // LTS version
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }
}

idea {
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}
