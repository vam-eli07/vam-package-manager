package com.vameli.vam.packagemanager.gui.common

import com.vameli.vam.packagemanager.core.LongRunningTask
import com.vameli.vam.packagemanager.core.TaskProgress
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import java.util.concurrent.TimeUnit

fun <T : Any> ObservableValue<T>.toObservable(): Observable<T> = Observable.create { emitter ->
    val listener: ChangeListener<T> = ChangeListener<T> { _, _, newValue ->
        emitter.onNext(newValue)
    }
    addListener(listener)
    emitter.setCancellable { removeListener(listener) }
}

typealias LongRunningTaskObservable<PROGRESS, RESULT> = Observable<LongRunningTaskEvent<PROGRESS, RESULT>>

fun <PROGRESS, RESULT> LongRunningTask<PROGRESS, RESULT>.asGuiObservable(): LongRunningTaskObservable<PROGRESS, RESULT> =
    Observable.create<LongRunningTaskEvent<PROGRESS, RESULT>> { emitter ->
        try {
            val result = execute { progress ->
                emitter.onNext(LongRunningTaskEvent(taskProgress = progress))
            }
            emitter.onNext(LongRunningTaskEvent(result = result))
            emitter.onComplete()
        } catch (t: Throwable) {
            emitter.onError(t)
        }
    }
        .publish()
        .refCount()
        .debounce(100, TimeUnit.MILLISECONDS)
        .subscribeOn(Schedulers.io())
        .observeOn(JavaFXPlatformScheduler)

data class LongRunningTaskEvent<PROGRESS, RESULT>(
    val taskProgress: TaskProgress<PROGRESS>? = null,
    val result: RESULT? = null,
) {
    init {
        require(taskProgress != null || result != null) {
            "At least one of progress or result must be non-null"
        }
    }
}

object AnyConsumer : (Any) -> Unit {
    override fun invoke(t: Any) {
        // Do nothing
    }
}

object UnitConsumer : () -> Unit {
    override fun invoke() {
        // Do nothing
    }
}

private object JavaFXPlatformScheduler : Scheduler() {
    override fun createWorker(): Worker = JavaFXPlatformRunLaterWorker
}

private object JavaFXPlatformRunLaterWorker : Scheduler.Worker() {

    override fun schedule(run: Runnable): Disposable = Disposable.disposed().also {
        Platform.runLater(run)
    }

    override fun schedulePeriodically(run: Runnable, initialDelay: Long, period: Long, unit: TimeUnit): Disposable =
        throw NotImplementedError("Periodic execution not supported")

    override fun schedule(run: Runnable, delay: Long, unit: TimeUnit): Disposable =
        throw NotImplementedError("Delayed execution not supported")

    override fun isDisposed(): Boolean = true

    override fun dispose() = Unit
}
