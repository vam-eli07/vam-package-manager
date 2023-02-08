dependencies {
    implementation("org.springframework.data:spring-data-neo4j")
    implementation("org.neo4j:neo4j:${Versions.NEO4J}") {
        exclude("org.slf4j")
    }
    implementation("org.neo4j:neo4j-bolt:${Versions.NEO4J}")
    testImplementation("org.testcontainers:neo4j:${Versions.TESTCONTAINER_NEO4J}")
}
