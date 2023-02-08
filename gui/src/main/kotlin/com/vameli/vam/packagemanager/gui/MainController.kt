package com.vameli.vam.packagemanager.gui

import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.core.service.ModelNotUpToDateException
import com.vameli.vam.packagemanager.gui.settings.ApplicationSettingsUIService
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.WARNING
import net.rgielen.fxweaver.core.FxmlView
import org.springframework.stereotype.Component

@Component
@FxmlView("/layouts/application.fxml")
class MainController(
    private val applicationSettingsUIService: ApplicationSettingsUIService,
    private val databaseUIService: DatabaseUIService,
) {

    @FXML
    fun initialize() {
        applicationSettingsUIService.ensureApplicationConfigured(null)
        try {
            databaseUIService.ensureDatabaseReady()
        } catch (_: ModelNotUpToDateException) {
            Alert(WARNING, "Database model is not up to date. Please run the database migration tool.").showAndWait()
        }
    }

    @FXML
    fun doSomething() {
        logger().info("Did something")
    }
}
