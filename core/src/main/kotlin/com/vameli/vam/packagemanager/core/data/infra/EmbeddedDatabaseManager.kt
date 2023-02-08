package com.vameli.vam.packagemanager.core.data.infra

import com.vameli.vam.packagemanager.core.logger
import org.neo4j.configuration.connectors.BoltConnector
import org.neo4j.configuration.helpers.SocketAddress
import org.neo4j.dbms.api.DatabaseManagementService
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder
import org.springframework.beans.factory.DisposableBean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
@Profile(PROFILE_NEO4J_EMBEDDED)
class EmbeddedDatabaseManager : DisposableBean {

    private val monitor = Any()
    private var databaseManagementService: DatabaseManagementService? = null

    fun open(dataDir: Path) = synchronized(monitor) {
        if (databaseManagementService != null) {
            throw IllegalStateException("Database is already open")
        }
        val socketAddress = SocketAddress("localhost", 7687)
        databaseManagementService = DatabaseManagementServiceBuilder(dataDir)
            .setConfig(BoltConnector.enabled, true)
            .setConfig(BoltConnector.listen_address, socketAddress)
            .build()
        logger().info("Starting embedded database server with dataDir: $dataDir listening on $socketAddress")
    }

    fun close() = synchronized(monitor) {
        if (databaseManagementService == null) {
            logger().info("Database is already closed")
            return
        }
        logger().info("Stopping embedded database server")
        databaseManagementService?.shutdown()
        databaseManagementService = null
    }

    fun isOpen() = synchronized(monitor) {
        databaseManagementService != null
    }

    fun delete(dataDir: Path) = synchronized(monitor) {
        logger().info("Deleting embedded database server dataDir: $dataDir")
        if (databaseManagementService != null) {
            close()
        }
        dataDir.toFile().deleteRecursively()
    }

    override fun destroy() = close()
}
