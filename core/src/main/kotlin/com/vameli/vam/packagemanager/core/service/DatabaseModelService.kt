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
    fun checkAndInitializeModel(): DatabaseInitializationStatus {
        val modelNodes = databaseModelRepository.findAll()
        if (modelNodes.size > 1) {
            // this shouldn't normally happen, most likely the database is corrupt, force rebuild
            throw ModelNotUpToDateException()
        }
        val modelNode = modelNodes.firstOrNull()
        if (modelNode == null) {
            logger().info("Database has apparently just been created, initializing database model.")
            initializeDatabaseModel()
            return DatabaseInitializationStatus.CREATED_NEW
        } else if (modelNode.versionId != DATABASE_MODEL_VERSION) {
            throw ModelNotUpToDateException()
        } else if (!modelNode.importFinished) {
            throw DatabaseImportDidNotFinishException()
        }
        return DatabaseInitializationStatus.OPENED_EXISTING
    }

    fun markDatabaseImportStarted() = transactionTemplate.execute {
        val databaseModel =
            databaseModelRepository.findByVersionId(DATABASE_MODEL_VERSION) ?: throw ModelNotUpToDateException()
        databaseModelRepository.save(databaseModel.copy(importFinished = false))
    }

    fun markDatabaseImportFinished() = transactionTemplate.execute {
        val databaseModel =
            databaseModelRepository.findByVersionId(DATABASE_MODEL_VERSION) ?: throw ModelNotUpToDateException()
        if (databaseModel.importFinished) {
            throw ModelNotUpToDateException()
        }
        databaseModelRepository.save(databaseModel.copy(importFinished = true))
    }

    private fun initializeDatabaseModel() = transactionTemplate.execute {
        databaseModelRepository.save(DatabaseModel(DATABASE_MODEL_VERSION, 0))
        neo4jDriver.session().executeWriteWithoutResult { context ->
            // TODO add indices here
//            context.run("")
        }
    }
}

abstract class DatabaseInInconsistentStateException(message: String) : IllegalStateException(message)

class ModelNotUpToDateException : DatabaseInInconsistentStateException("Database model is not up to date")
class DatabaseImportDidNotFinishException : DatabaseInInconsistentStateException("Database import did not finish")

enum class DatabaseInitializationStatus {
    CREATED_NEW,
    OPENED_EXISTING,
}
