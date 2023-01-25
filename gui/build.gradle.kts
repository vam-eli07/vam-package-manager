import org.graalvm.buildtools.gradle.dsl.GraalVMExtension

apply {
    plugin("org.springframework.boot")
    plugin("org.graalvm.buildtools.native")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter:${Versions.SPRING_BOOT}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.SPRING_BOOT}")
}

val action = Action<GraalVMExtension> {
    binaries {
        named("main") {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            )
        }
    }
}
extensions.configure("graalvmNative", action)
