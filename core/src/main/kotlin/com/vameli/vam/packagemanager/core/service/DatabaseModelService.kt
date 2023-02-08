package com.vameli.vam.packagemanager.core.service

import com.vameli.vam.packagemanager.core.data.model.DatabaseModel
import com.vameli.vam.packagemanager.core.data.model.DatabaseModelRepository
import com.vameli.vam.packagemanager.core.logger
import org.neo4j.driver.Driver
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

private const val DATABASE_MODEL_VERSION = 1

@Service
class DatabaseModelService(
    private val databaseModelRepository: DatabaseModelRepository,
    private val transactionTemplate: TransactionTemplate,
    private val neo4jDriver: Driver,
) {
    fun checkAndInitializeModel() {
        val modelNodes = databaseModelRepository.findAll()
        if (modelNodes.size > 1) {
            // this shouldn't normally happen, most likely the database is corrupt, force reindex
            throw ModelNotUpToDateException()
        }
        val modelNode = modelNodes.firstOrNull()
        if (modelNode == null) {
            logger().info("Database has apparently just been created, initializing database model.")
            initializeDatabaseModel()
        } else if (modelNode.version != DATABASE_MODEL_VERSION) {
            throw ModelNotUpToDateException()
        }
    }

    private fun initializeDatabaseModel() = transactionTemplate.execute {
        databaseModelRepository.save(DatabaseModel(DATABASE_MODEL_VERSION))
        neo4jDriver.session().executeWriteWithoutResult { context ->
//            context.run("")
        }
    }
}

class ModelNotUpToDateException : IllegalStateException("Database model is not up to date")
