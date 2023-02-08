package com.vameli.vam.packagemanager.core.data.infra

import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.requireState
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable

private const val PROPERTY_DATA_DIR_PATH = "data.dir.path"
private const val DEFAULT_DATA_DIR = "data"

interface DatabaseEnvironment {
    fun getDataDirectory(): Path?
    fun setDataDirectory(path: Path)
    fun isDataDirectoryPathValid(path: Path): Boolean
    fun getConnectionString(): String?
    fun isConfigured(): Boolean
    fun isConfigurable(): Boolean
    fun isStarted(): Boolean
    fun start()
    fun stop()
    fun deleteDatabase()
}

@Component
@Profile(PROFILE_NEO4J_EMBEDDED)
internal class EmbeddedDatabaseEnvironment(
    private val systemEnvironment: SystemEnvironment,
    private val embeddedDatabaseManager: EmbeddedDatabaseManager,
    @Value("\${spring.neo4j.uri}") private val neo4jUri: String,
) : DatabaseEnvironment {

    private var dataDirPath: Path? = null

    init {
        val dataDirPathPropertyValue = systemEnvironment.getProperty(PROPERTY_DATA_DIR_PATH)
        if (dataDirPathPropertyValue == null) {
            dataDirPath = systemEnvironment.getApplicationHome().resolve(DEFAULT_DATA_DIR)
            systemEnvironment.setProperty(PROPERTY_DATA_DIR_PATH, dataDirPath.toString())
            logger().info("Data directory path was not set, initializing to default: $dataDirPath")
        } else {
            dataDirPath = Paths.get(dataDirPathPropertyValue)
        }
        prepareDatabaseDirectory()
    }

    override fun getDataDirectory(): Path? = dataDirPath

    override fun isDataDirectoryPathValid(path: Path): Boolean = path.isDirectory() &&
        path.isWritable()

    override fun setDataDirectory(path: Path) {
        require(isDataDirectoryPathValid(path)) { "Invalid data directory path: $path" }
        requireState(!isStarted()) { "Can't change data directory path while database is running" }
        systemEnvironment.setProperty(PROPERTY_DATA_DIR_PATH, path.toString())
        dataDirPath = path
    }

    override fun getConnectionString(): String = neo4jUri

    override fun isConfigured(): Boolean = getDataDirectory()?.let { isDataDirectoryPathValid(it) } ?: false

    override fun isConfigurable(): Boolean = true

    override fun isStarted(): Boolean = embeddedDatabaseManager.isOpen()

    override fun start() {
        embeddedDatabaseManager.open(requireNotNull(dataDirPath) { "Data directory path is not set" })
    }

    override fun stop() {
        embeddedDatabaseManager.close()
    }

    override fun deleteDatabase() {
        dataDirPath?.let {
            logger().info("Deleting database directory: $it")
            embeddedDatabaseManager.delete(it)
        }
    }

    private fun prepareDatabaseDirectory() {
        val dirPath: Path = dataDirPath!!

        if (!dirPath.isDirectory()) {
            logger().info("Data directory path does not exist, creating: $dataDirPath")
            dirPath.createDirectories()
        }
    }
}

@Component
@Profile("!$PROFILE_NEO4J_EMBEDDED")
internal class ExternalServerDatabaseEnvironment(
    @Value("\${spring.neo4j.uri}") private val neo4jUri: String,
    private val neo4jDriver: Driver,
) : DatabaseEnvironment {
    override fun getDataDirectory(): Path? = null

    override fun isDataDirectoryPathValid(path: Path): Boolean = true

    override fun setDataDirectory(path: Path) =
        throw NotImplementedError("External server database environment does not support setting data directory")

    override fun getConnectionString(): String = neo4jUri

    override fun isConfigured(): Boolean = true

    override fun isConfigurable(): Boolean = false

    override fun isStarted(): Boolean = true

    override fun start() = Unit

    override fun stop() = Unit

    override fun deleteDatabase() {
        neo4jDriver.session().use { session ->
            session.run("MATCH (n) DETACH DELETE n")
        }
    }
}
