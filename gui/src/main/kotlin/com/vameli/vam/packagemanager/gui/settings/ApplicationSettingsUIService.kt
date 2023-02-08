package com.vameli.vam.packagemanager.gui.settings

import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.core.data.infra.SystemEnvironment
import com.vameli.vam.packagemanager.gui.common.ViewService
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.stage.Stage
import org.springframework.stereotype.Component

@Component
class ApplicationSettingsUIService(
    private val systemEnvironment: SystemEnvironment,
    private val databaseEnvironment: DatabaseEnvironment,
    private val viewService: ViewService,
) {

    fun isApplicationConfigured(): Boolean =
        databaseEnvironment.isConfigured() && systemEnvironment.getVamInstallationHome() != null

    fun showAndUpdateSettings(ownerWindow: Stage? = null) {
        val (stage, controller) = viewService.createModalDialog(
            ApplicationSettingsController::class,
            title = "Application Settings",
            resizable = false,
            ownerWindow = ownerWindow,
        )
        stage.showAndWait()
        if (controller.viewModel.saveData) {
            systemEnvironment.setVamInstallationHome(controller.viewModel.vamInstallDirPath)
            databaseEnvironment.setDataDirectory(controller.viewModel.dataDirPath)
        }
    }

    fun ensureApplicationConfigured(ownerWindow: Stage? = null) {
        if (!isApplicationConfigured()) {
            showAndUpdateSettings(ownerWindow)
            if (!isApplicationConfigured()) {
                Alert(
                    Alert.AlertType.ERROR,
                    "Application can't continue if not properly configured. Application will now exit.",
                ).showAndWait()
                Platform.exit()
                throw ApplicationNotConfiguredException()
            }
        }
    }
}

class ApplicationNotConfiguredException : IllegalStateException("Application is not configured")
