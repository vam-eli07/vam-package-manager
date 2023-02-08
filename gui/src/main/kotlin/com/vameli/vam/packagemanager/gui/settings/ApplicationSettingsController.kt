package com.vameli.vam.packagemanager.gui.settings

import com.vameli.vam.packagemanager.core.data.infra.DatabaseEnvironment
import com.vameli.vam.packagemanager.gui.common.Controller
import com.vameli.vam.packagemanager.gui.common.ValidVAMInstallDirectoryValidator
import com.vameli.vam.packagemanager.gui.common.WritableDirectoryValidator
import com.vameli.vam.packagemanager.gui.common.toObservable
import javafx.beans.property.ReadOnlyProperty
import javafx.beans.property.StringProperty
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import net.rgielen.fxweaver.core.FxmlView
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationSupport
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths

@Component
@FxmlView("/layouts/settings/application-settings.fxml")
class ApplicationSettingsController(
    private val applicationSettingsUIService: ApplicationSettingsUIService,
    private val databaseEnvironment: DatabaseEnvironment,
) : Controller() {

    lateinit var viewModel: ViewModel

    @FXML
    private lateinit var okButton: Button

    @FXML
    private lateinit var vamInstallDirPathTextField: TextField

    @FXML
    private lateinit var databaseDirPathDescriptionLabel: Label

    @FXML
    private lateinit var databaseDirPathTextField: TextField

    @FXML
    private lateinit var browseDatabaseDirPathButton: Button

    private val validationSupport = ValidationSupport()

    @FXML
    fun initialize() {
        ButtonBar.setButtonData(okButton, ButtonBar.ButtonData.OK_DONE)
        databaseDirPathTextField.text = databaseEnvironment.getDataDirectory().toString()
        if (!databaseEnvironment.isConfigurable()) {
            databaseDirPathDescriptionLabel.text =
                "Database directory not configurable when using external database server."
            databaseDirPathDescriptionLabel.isDisable = true
            databaseDirPathTextField.isDisable = true
            browseDatabaseDirPathButton.isDisable = true
        }
        viewModel = ViewModel(
            vamInstallDirPathProperty = vamInstallDirPathTextField.textProperty(),
            dataDirPathProperty = databaseDirPathTextField.textProperty(),
            validationResultProperty = validationSupport.validationResultProperty(),
        )
        initValidations()
    }

    @FXML
    fun browseVamInstallDir() {
        val selectedDir = DirectoryChooser().showDialog(stage)
        selectedDir?.let { vamInstallDirPathTextField.text = it.absolutePath }
    }

    @FXML
    fun browseDatabaseDir() {
        val selectedDir = DirectoryChooser().showDialog(stage)
        selectedDir?.let { databaseDirPathTextField.text = it.absolutePath }
    }

    @FXML
    fun ok() {
        if (viewModel.isValid) {
            viewModel.saveData = true
            stage!!.close()
        } else {
            Alert(Alert.AlertType.WARNING, "Please fix the errors before continuing.").showAndWait()
        }
    }

    private fun initValidations() {
        validationSupport.registerValidator(
            vamInstallDirPathTextField,
            ValidVAMInstallDirectoryValidator(false),
        )

        validationSupport.registerValidator(
            databaseDirPathTextField,
            WritableDirectoryValidator(false),
        )

        validationSupport.initInitialDecoration()

        cleanupSubscription(
            validationSupport.validationResultProperty().toObservable().subscribe {
                okButton.isDisable = !it.errors.isEmpty()
            },
        )
    }

    data class ViewModel(
        val vamInstallDirPathProperty: StringProperty,
        val dataDirPathProperty: StringProperty,
        val validationResultProperty: ReadOnlyProperty<ValidationResult>,
        var saveData: Boolean = false,
    ) {
        val vamInstallDirPath: Path
            get() = Paths.get(vamInstallDirPathProperty.value)
        val dataDirPath: Path
            get() = Paths.get(dataDirPathProperty.value)
        val isValid: Boolean
            get() = validationResultProperty.value.errors.isEmpty()
    }
}
