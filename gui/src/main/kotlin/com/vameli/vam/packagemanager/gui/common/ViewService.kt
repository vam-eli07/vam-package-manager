package com.vameli.vam.packagemanager.gui.common

import atlantafx.base.theme.NordDark
import com.vameli.vam.packagemanager.core.LongRunningTask
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Modality
import javafx.stage.Stage
import net.rgielen.fxweaver.core.FxControllerAndView
import net.rgielen.fxweaver.core.FxWeaver
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

typealias ProgressWithWindowControllerListener<PROGRESS, RESULT> = (
    event: LongRunningTaskEvent<PROGRESS, RESULT>,
    windowController: TaskProgressWindowController,
) -> Unit

private

val stylesheet = NordDark().userAgentStylesheet

private const val STYLESHEET_CSS = "/layouts/application.css"
private const val DEFAULT_TITLE = "VAM Package Manager"
private const val DEFAULT_PROGRESS_DIALOG_TITLE = "Task in progress..."

@Component
class ViewService(private val fxWeaver: FxWeaver) {

    fun <C : Controller> createModalDialog(
        controllerClass: KClass<C>,
        title: String = DEFAULT_TITLE,
        resizable: Boolean = true,
        ownerWindow: Stage? = null,
    ): StageWithController<C> {
        val sceneWithController = createScene(controllerClass)
        val controller = sceneWithController.controller
        val stage = Stage().apply {
            if (ownerWindow != null) {
                initModality(Modality.WINDOW_MODAL)
                initOwner(ownerWindow)
            } else {
                initModality(Modality.APPLICATION_MODAL)
            }
            scene = sceneWithController.scene
            this.title = title
            this.isResizable = resizable
            controller.stage = this
            setOnHiding { windowEvent ->
                controller.beforeDispose(windowEvent)
                if (!windowEvent.isConsumed) {
                    controller.dispose()
                }
            }
        }
        return StageWithController(stage, sceneWithController.controller)
    }

    fun createTaskProgressModalDialog(title: String = DEFAULT_PROGRESS_DIALOG_TITLE, ownerWindow: Stage? = null) =
        createModalDialog(TaskProgressWindowController::class, title, false, ownerWindow)

    fun <C : Controller> createScene(controllerClass: KClass<C>): SceneWithController<C> {
        val controllerAndView = fxWeaver.load<C, Parent>(controllerClass.java)
        val scene = Scene(
            controllerAndView.view.orElseThrow {
                IllegalStateException("Cannot create scene for controller $controllerClass: root view is null")
            },
        )
        scene.userAgentStylesheet = stylesheet
        scene.stylesheets.add(javaClass.getResource(STYLESHEET_CSS)?.toExternalForm())
        return SceneWithController(scene, controllerAndView.controller)
    }

    fun <C : Controller> createController(controllerClass: KClass<C>): C = fxWeaver.loadController(controllerClass.java)

    fun <C : Controller, V : Node> createComponent(controllerClass: KClass<C>): FxControllerAndView<C, V> =
        fxWeaver.load(controllerClass.java)

    fun <PROGRESS, RESULT> runLongRunningTaskInModalProgressDialog(
        title: String = DEFAULT_PROGRESS_DIALOG_TITLE,
        ownerWindow: Stage? = null,
        task: LongRunningTask<PROGRESS, RESULT>,
        onAbortThrow: () -> Throwable = { ProgressAbortedException() },
        onProgress: ProgressWithWindowControllerListener<PROGRESS, RESULT>? = null,
    ) {
        var throwable: Throwable? = null
        val (stage, controller) = createTaskProgressModalDialog(title, ownerWindow)

        val subscription = task.asGuiObservable().subscribe(
            /* onNext = */ { event -> onProgress?.invoke(event, controller) },
            /* onError = */
            { t ->
                throwable = t
                stage.close()
            },
            /* onComplete = */ { stage.close() },
        )
        stage.setOnCloseRequest { _ ->
            throwable = onAbortThrow()
        }
        stage.showAndWait()
        subscription.dispose()
        throwable?.let { throw it }
    }
}

data class StageWithController<C : Any>(val stage: Stage, val controller: C)
data class SceneWithController<C : Any>(val scene: Scene, val controller: C)
class ProgressAbortedException : IllegalStateException("Progress window closed by user")
