package com.vameli.vam.packagemanager.core.data.infra

import com.vameli.vam.packagemanager.core.logger
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isWritable

private const val HOME_SUBDIR_NAME = ".vamPackageManager"
private const val VAM_INSTALLATION_HOME_PROPERTY = "vam.installation.home"
private const val APPLICATION_PROPERTIES = "application.properties"

interface SystemEnvironment {

    fun getUserHome(): Path
    fun getApplicationHome(): Path
    fun getVamInstallationHome(): Path?
    fun setVamInstallationHome(path: Path)
    fun getProperty(propertyName: String): String?
    fun setProperty(propertyName: String, propertyValue: String)
    fun getProperties(): Properties
    fun storeProperties(properties: Properties)
}

@Component
internal class SystemEnvironmentImpl : SystemEnvironment {

    private val userHome: Path = Path.of(System.getProperty("user.home"))
    private val applicationHome: Path = userHome.resolve(HOME_SUBDIR_NAME)
    private val propertiesPath = applicationHome.resolve(APPLICATION_PROPERTIES)

    init {
        logger().info("Using application home: ${getApplicationHome()}")
        require(userHome.isDirectory() && userHome.isWritable()) { "User home is not a writable directory: $userHome" }
        if (!applicationHome.isDirectory()) {
            applicationHome.createDirectory()
        }
    }

    override fun getUserHome(): Path = userHome

    override fun getApplicationHome(): Path = applicationHome

    override fun getVamInstallationHome(): Path? = getProperty(VAM_INSTALLATION_HOME_PROPERTY)?.let { Paths.get(it) }

    override fun setVamInstallationHome(path: Path) {
        setProperty(VAM_INSTALLATION_HOME_PROPERTY, path.toString())
    }

    override fun getProperty(propertyName: String): String? = getProperties()?.getProperty(propertyName)

    override fun setProperty(propertyName: String, propertyValue: String) = (getProperties() ?: Properties()).let {
        it.setProperty(propertyName, propertyValue)
        storeProperties(it)
    }

    override fun getProperties(): Properties = propertiesPath.takeIf { it.exists() }?.let { path ->
        Properties().apply {
            FileInputStream(path.toFile()).use { load(it) }
        }
    } ?: Properties()

    override fun storeProperties(properties: Properties) {
        if (!propertiesPath.exists()) {
            propertiesPath.createFile()
        }
        FileOutputStream(propertiesPath.toFile()).use { properties.store(it, "Vam package manager main settings") }
    }
}
