package com.vameli.vam.packagemanager.gui

import com.vameli.vam.packagemanager.core.logger
import com.vameli.vam.packagemanager.gui.settings.ApplicationSettingsUIService
import javafx.fxml.FXML
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
        databaseUIService.ensureDatabaseReady()
    }

    @FXML
    fun doSomething() {
        logger().info("Did something")
    }
}
