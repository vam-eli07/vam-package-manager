package com.vameli.vam.packagemanager.gui

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.ProgressListener
import com.vameli.vam.packagemanager.core.TaskProgress
import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.core.data.infra.SystemEnvironment
import com.vameli.vam.packagemanager.core.service.DatabaseInitializationStatus
import com.vameli.vam.packagemanager.core.service.DatabaseModelService
import com.vameli.vam.packagemanager.core.service.ModelNotUpToDateException
import com.vameli.vam.packagemanager.gui.common.ProgressAbortedException
import com.vameli.vam.packagemanager.gui.common.TaskProgressWindowController
import com.vameli.vam.packagemanager.gui.common.ViewService
import com.vameli.vam.packagemanager.importer.ImportJobFactory
import com.vameli.vam.packagemanager.importer.ImportJobProgress
import javafx.application.Platform
import javafx.scene.control.Alert
import org.springframework.stereotype.Component

@Component
class DatabaseUIService(
    private val systemEnvironment: SystemEnvironment,
    private val databaseEnvironment: DatabaseEnvironment,
    private val databaseModelService: DatabaseModelService,
    private val viewService: ViewService,
    private val importJobFactory: ImportJobFactory,
) {

    fun ensureDatabaseReady() {
        openDatabase()
        try {
            val databaseInitializationStatus = databaseModelService.checkAndInitializeModel()
            if (databaseInitializationStatus == DatabaseInitializationStatus.CREATED_NEW) {
                runFullDatabaseImport()
            }
        } catch (_: ModelNotUpToDateException) {
            Alert(
                Alert.AlertType.WARNING,
                "Database model is not up to date. Database will now be rebuilt.",
            ).showAndWait()
            nukeDatabase()
            runFullDatabaseImport()
        }
    }

    private fun openDatabase() {
        try {
            viewService.runLongRunningTaskInModalProgressDialog(
                title = "Opening database",
                task = OpenDatabaseTask(databaseEnvironment),
            ) { event, windowController -> windowController.updateProgress(event.taskProgress?.progress) }
        } catch (e: ProgressAbortedException) {
            Platform.exit()
            throw e
        } catch (t: Throwable) {
            Alert(Alert.AlertType.ERROR, "Failed to open database: ${t.message}").showAndWait()
            Platform.exit()
            throw t
        }
    }

    private fun nukeDatabase() {
        try {
            viewService.runLongRunningTaskInModalProgressDialog(
                title = "Rebuilding database",
                task = NukeDatabaseTask(databaseEnvironment),
            ) { event, windowController -> event.taskProgress?.progress?.let { windowController.updateProgress(it) } }
        } catch (e: ProgressAbortedException) {
            Platform.exit()
            throw e
        } catch (t: Throwable) {
            Alert(Alert.AlertType.ERROR, "Failed to recreate database: ${t.message}").showAndWait()
            Platform.exit()
            throw t
        }
    }

    private fun runFullDatabaseImport() {
        val importJob = importJobFactory.createFullImportJob(systemEnvironment.getVamInstallationHome()!!)
        try {
            viewService.runLongRunningTaskInModalProgressDialog(
                title = "Building database",
                task = importJob,
            ) { event, windowController -> windowController.updateImportTaskProgress(event.taskProgress) }
        } catch (e: ProgressAbortedException) {
            Platform.exit()
            throw e
        } catch (t: Throwable) {
            Alert(Alert.AlertType.ERROR, "Failed to open database: ${t.message}").showAndWait()
            Platform.exit()
            throw t
        }
    }

    private fun TaskProgressWindowController.updateImportTaskProgress(taskProgress: TaskProgress<ImportJobProgress>?) {
        if (taskProgress == null) {
            return
        }
        var primaryText = taskProgress.progress.currentOperation
        if (taskProgress.progress.currentlyProcessingFile != null) {
            primaryText += ": " + taskProgress.progress.currentlyProcessingFile
        }
        val secondaryText = if (taskProgress.progress.currentlyProcessingArtifactId != null) {
            taskProgress.progress.currentlyProcessingArtifactId?.toString()
        } else {
            ""
        }
        updateProgress(primaryText, secondaryText, taskProgress.percentCompleted)
    }
}

private class OpenDatabaseTask(private val databaseEnvironment: DatabaseEnvironment) : LongRunningTask<String, Unit> {
    override fun execute(progressListener: ProgressListener<String>) {
        val dataDirString = databaseEnvironment.getDataDirectory().toString()
        progressListener(TaskProgress("Opening database: $dataDirString"))
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
