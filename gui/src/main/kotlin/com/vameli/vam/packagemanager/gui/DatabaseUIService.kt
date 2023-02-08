package com.vameli.vam.packagemanager.gui

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.gui.common.AnyConsumer
import com.vameli.vam.packagemanager.gui.common.ViewService
import com.vameli.vam.packagemanager.gui.common.asGuiObservable
import javafx.application.Platform
import javafx.scene.control.Alert
import org.springframework.stereotype.Component

@Component
class DatabaseUIService(
    private val databaseEnvironment: DatabaseEnvironment,
    private val databaseModelService: DatabaseModelService,
    private val viewService: ViewService,
) {

    fun ensureDatabaseReady() {
        openDatabase()
        databaseModelService.checkAndInitializeModel()
    }

    private fun openDatabase() {
        val (stage, controller) = viewService.createTaskProgressModalDialog("Opening database")
        val dataDirString = databaseEnvironment.getDataDirectory().toString()
        controller.updateProgress(primaryText = "Opening database folder: $dataDirString")
        val subscription =
            OpenDatabaseTask(databaseEnvironment).asGuiObservable().subscribe(
                /* onNext = */ AnyConsumer,
                /* onError = */
                { t ->
                    Alert(Alert.AlertType.ERROR, "Failed to open database: ${t.message}").showAndWait()
                    Platform.exit()
                    throw DatabaseNotReadyException()
                },
                /* onComplete = */ { stage.close() },
            )
        stage.setOnCloseRequest {
            subscription.dispose()
            Platform.exit()
            throw DatabaseNotReadyException()
        }
        stage.showAndWait()
    }
}

class DatabaseNotReadyException : IllegalStateException("Database is not ready")

private class OpenDatabaseTask(private val databaseEnvironment: DatabaseEnvironment) : LongRunningTask<Unit, Unit> {
    override fun execute(progressListener: ProgressListener<Unit>) {
        databaseEnvironment.start()
    }
}

private class NukeDatabaseTask(private val databaseEnvironment: DatabaseEnvironment) :
    LongRunningTask<String, Unit> {
    override fun execute(progressListener: ProgressListener<String>) {
        progressListener(TaskProgress("Closing database..."))
        databaseEnvironment.stop()
        progressListener(TaskProgress("Deleting database..."))
        databaseEnvironment.deleteDatabase()
        progressListener(TaskProgress("Starting database..."))
        databaseEnvironment.start()
    }
}
