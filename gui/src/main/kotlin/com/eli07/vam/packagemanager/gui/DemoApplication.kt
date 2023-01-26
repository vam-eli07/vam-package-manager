package com.eli07.vam.packagemanager.gui

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import javafx.stage.Stage

class DemoApplication : Application() {
    override fun start(primaryStage: Stage?) {
        requireNotNull(primaryStage)
        val button = Button("Hello World").apply {
            setOnAction {
                println("Hello World")
            }
        }
        StackPane(button).apply {
            primaryStage.scene = Scene(this, 800.0, 600.0)
        }
        primaryStage.show()
    }
}
