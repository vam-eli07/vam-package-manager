package com.vameli.vam.packagemanager.gui

import javafx.application.Application
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class VamPackageManagerGuiApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(VamPackageManagerGuiApplication::class.java, *args)
            Application.launch(DemoApplication::class.java, *args)
        }
    }
}
