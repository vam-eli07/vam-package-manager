package com.vameli.vam.packagemanager.gui

import atlantafx.base.theme.NordDark
import com.vameli.vam.packagemanager.core.data.infra.SystemEnvironment
import com.vameli.vam.packagemanager.core.logger
import io.reactivex.rxjava3.core.Observable
import javafx.application.Application
import javafx.beans.value.ChangeListener
import javafx.scene.Scene
import javafx.stage.Stage
import net.rgielen.fxweaver.core.FxWeaver
import net.rgielen.fxweaver.spring.SpringFxWeaver
import org.springframework.boot.WebApplicationType.NONE
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private const val PROPERTY_WINDOW_WIDTH = "ui.main.window.width"
private const val PROPERTY_WINDOW_HEIGHT = "ui.main.window.height"
private const val PROPERTY_WINDOW_MAXIMIZED = "ui.main.window.maximized"
private const val WINDOW_MIN_WIDTH = 800.0
private const val WINDOW_MIN_HEIGHT = 600.0

@SpringBootApplication(scanBasePackages = ["com.vameli.vam.packagemanager"])
open class VamPackageManagerGuiApplication {

    @Bean
    open fun fxWeaver(context: ConfigurableApplicationContext): FxWeaver = SpringFxWeaver(context)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(VamManagerFXApplication::class.java, *args)
        }
    }
}

class VamManagerFXApplication : Application() {

    private lateinit var context: ConfigurableApplicationContext

    override fun init() {
        super.init()
        context = SpringApplicationBuilder()
            .sources(VamPackageManagerGuiApplication::class.java)
            .web(NONE)
            .run(*parameters.raw.toTypedArray())
    }

    override fun start(primaryStage: Stage?) {
        requireNotNull(primaryStage)
        context.publishEvent(StageReadyEvent(primaryStage))
    }

    override fun stop() {
        super.stop()
        val weaver = context.getBean(FxWeaver::class.java)
        context.close()
        weaver.shutdown()
    }
}

class StageReadyEvent(val stage: Stage) : ApplicationEvent(stage)

@Component
class PrimaryStageInitializer(
    private val systemEnvironment: SystemEnvironment,
    private val fxWeaver: FxWeaver
) : ApplicationListener<StageReadyEvent> {

    private lateinit var stage: Stage

    private lateinit var windowStateObservable: Observable<Unit>

    override fun onApplicationEvent(event: StageReadyEvent) {
        stage = event.stage
        val uiWidth = systemEnvironment.getProperty(PROPERTY_WINDOW_WIDTH)?.toDoubleOrNull() ?: WINDOW_MIN_WIDTH
        val uiHeight = systemEnvironment.getProperty(PROPERTY_WINDOW_HEIGHT)?.toDoubleOrNull() ?: WINDOW_MIN_HEIGHT
        val isMaximized = systemEnvironment.getProperty(PROPERTY_WINDOW_MAXIMIZED)?.toBoolean() ?: false
        val scene = Scene(fxWeaver.loadView(MainController::class.java), uiWidth, uiHeight)
        scene.userAgentStylesheet = NordDark().userAgentStylesheet
        scene.stylesheets.add(javaClass.getResource("/layouts/application.css")?.toExternalForm())
        stage.minWidth = WINDOW_MIN_WIDTH
        stage.minHeight = WINDOW_MIN_HEIGHT
        stage.scene = scene
        stage.title = "VAM Package Manager"
        stage.isMaximized = isMaximized
        stage.show()

        windowStateObservable = Observable.create { emitter ->
            val listener: ChangeListener<Any> = ChangeListener { _, _, _ -> emitter.onNext(Unit) }
            stage.heightProperty().addListener(listener)
            stage.widthProperty().addListener(listener)
            stage.maximizedProperty().addListener(listener)
        }
        windowStateObservable.debounce(500, TimeUnit.MILLISECONDS).subscribe { persistWindowState() }
    }

    private fun persistWindowState() {
        logger().debug("Persisting window state")
        val properties = systemEnvironment.getProperties()
        properties.setProperty(PROPERTY_WINDOW_WIDTH, stage.width.toString())
        properties.setProperty(PROPERTY_WINDOW_HEIGHT, stage.height.toString())
        properties.setProperty(PROPERTY_WINDOW_MAXIMIZED, stage.isMaximized.toString())
        systemEnvironment.storeProperties(properties)
    }
}
