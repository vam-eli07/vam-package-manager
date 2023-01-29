import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.runtime") version "1.12.7"
    application
}

apply {
    plugin("org.springframework.boot")
    plugin("java")
}

dependencies {
    implementation(project(":core"))
    implementation("org.kordamp.ikonli:ikonli-javafx:${Versions.IKONLI}")
    implementation("org.springframework.boot:spring-boot-starter:${Versions.SPRING_BOOT}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.SPRING_BOOT}")
}

javafx {
    version = Versions.JAVAFX
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.vameli.vam.packagemanager.gui.VamPackageManagerGuiApplication")
}

tasks.withType<BootJar> {
    enabled = false
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    modules.set(
        listOf(
            "java.desktop",
            "jdk.jfr",
            "java.xml",
            "jdk.unsupported",
            "java.scripting"
        )
    )
}
