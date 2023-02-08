package com.vameli.vam.packagemanager.gui.common

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import net.rgielen.fxweaver.core.FxmlView
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@FxmlView("/layouts/task-progress-window.fxml")
class TaskProgressWindowController : Controller() {

    @FXML
    private lateinit var primaryLabel: Label

    @FXML
    private lateinit var secondaryLabel: Label

    @FXML
    private lateinit var progressBar: ProgressBar

    @FXML
    fun initialize() {
        progressBar.progress = ProgressBar.INDETERMINATE_PROGRESS
        primaryLabel.text = null
        secondaryLabel.text = null
    }

    fun updateProgress(primaryText: String, secondaryText: String? = null, progress: Int? = null) {
        primaryLabel.text = primaryText
        secondaryLabel.text = secondaryText
        progressBar.progress = progress?.let { it.toDouble() / 100.0 } ?: ProgressBar.INDETERMINATE_PROGRESS
    }
}
