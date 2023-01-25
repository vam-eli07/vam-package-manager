package com.eli07.vam.packagemanager.gui

import com.eli07.vam.packagemanager.core.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class VamPackageManagerGuiApplication

fun main(args: Array<String>) {
    runApplication<VamPackageManagerGuiApplication>(*args)
    logger(VamPackageManagerGuiApplication::class.java).info("Hello, world!")
}
