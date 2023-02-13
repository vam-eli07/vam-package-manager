import org.gradle.api.file.DuplicatesStrategy.EXCLUDE
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
    implementation(project(":importer"))

    // JavaFX related dependencies
    implementation("org.kordamp.ikonli:ikonli-javafx:${Versions.IKONLI}")
    implementation("org.kordamp.ikonli:ikonli-fontawesome5-pack:${Versions.IKONLI}")
    implementation("org.controlsfx:controlsfx:11.1.2")
    implementation("io.github.mkpaz:atlantafx-base:1.1.0")
    implementation("net.rgielen:javafx-weaver-spring-boot-starter:1.3.0")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.4")
}

javafx {
    version = Versions.JAVAFX
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("com.vameli.vam.packagemanager.gui.VamPackageManagerGuiApplication")
}

tasks.withType<Jar>() {
    duplicatesStrategy = EXCLUDE
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
            "java.scripting",
        ),
    )
}
