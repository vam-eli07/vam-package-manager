plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("edu.sc.seis.launch4j") version Versions.LAUNCH4J
    application
}

apply {
    plugin("org.springframework.boot")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter:${Versions.SPRING_BOOT}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.SPRING_BOOT}")
}

javafx {
    version = Versions.JAVAFX
    modules("javafx.controls", "javafx.fxml")
}

application {
    mainClassName = "com.eli07.vam.packagemanager.gui.VamPackageManagerGuiApplication"
}

launch4j {
    mainClassName = "com.eli07.vam.packagemanager.gui.VamPackageManagerGuiApplication"
    errTitle = "Error"
}
