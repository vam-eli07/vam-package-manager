package com.vameli.vam.packagemanager.gui.common

import javafx.scene.control.Control
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationResult.fromError
import org.controlsfx.validation.Validator
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable

private const val ERROR_VALUE_CANNOT_BE_EMPTY = "Value cannot be empty"
private const val ERROR_NOT_A_VALID_FILESYSTEM_PATH = "Not a valid filesystem path"
private const val ERROR_NOT_A_WRITABLE_DIRECTORY = "Not a directory or directory is not writable"
private const val ERROR_NOT_A_VALID_VAM_INSTALL_DIRECTORY = "This does not appear to be a valid VAM installation folder"

private val VAM_DIRECTORY_REQUIRED_FILES = setOf("vam.exe")
private val VAM_DIRECTORY_REQUIRED_DIRS = setOf("vam_data", "addonpackages")

open class ValidPathValidator(private val acceptEmptyValue: Boolean = false) : Validator<String> {

    override fun apply(control: Control, value: String?): ValidationResult {
        if (value.isNullOrEmpty()) {
            return if (acceptEmptyValue) {
                ValidationResult()
            } else {
                fromError(control, ERROR_VALUE_CANNOT_BE_EMPTY)
            }
        }
        return try {
            doValidate(control, Paths.get(value))
        } catch (e: InvalidPathException) {
            return fromError(control, ERROR_NOT_A_VALID_FILESYSTEM_PATH)
        }
    }

    protected open fun doValidate(control: Control, path: Path): ValidationResult = ValidationResult()
}

open class WritableDirectoryValidator(acceptEmptyValue: Boolean) : ValidPathValidator(acceptEmptyValue) {
    override fun doValidate(control: Control, path: Path): ValidationResult = super.doValidate(control, path).run {
        if (!path.isDirectory() || !path.isWritable()) {
            combine(fromError(control, ERROR_NOT_A_WRITABLE_DIRECTORY))
        } else {
            this
        }
    }
}

open class ValidVAMInstallDirectoryValidator(acceptEmptyValue: Boolean) : WritableDirectoryValidator(acceptEmptyValue) {
    override fun doValidate(control: Control, path: Path): ValidationResult = super.doValidate(control, path).run {
        val containsAllFiles = VAM_DIRECTORY_REQUIRED_FILES.all {
            val requestedPath = path.resolve(it)
            requestedPath.exists() && requestedPath.isRegularFile()
        }

        val containsAllDirs = VAM_DIRECTORY_REQUIRED_DIRS.all {
            val requestedPath = path.resolve(it)
            requestedPath.exists() && requestedPath.isDirectory()
        }

        if (containsAllFiles && containsAllDirs) {
            this
        } else {
            combine(fromError(control, ERROR_NOT_A_VALID_VAM_INSTALL_DIRECTORY))
        }
    }
}
