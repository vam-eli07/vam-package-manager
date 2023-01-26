dependencies {
    implementation("org.springframework.data:spring-data-neo4j")
    implementation("org.neo4j:neo4j:${Versions.NEO4J}") {
        exclude("org.slf4j")
    }
    testImplementation("org.testcontainers:neo4j:${Versions.TESTCONTAINER_NEO4J}")
}
