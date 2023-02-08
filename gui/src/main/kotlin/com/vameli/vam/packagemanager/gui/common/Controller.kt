package com.vameli.vam.packagemanager.gui.common

import com.vameli.vam.packagemanager.core.requireState
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import javafx.stage.Stage
import javafx.stage.WindowEvent

abstract class Controller {
    private val compositeDisposable = CompositeDisposable()

    var stage: Stage? = null
        set(value) {
            requireState(stage == null) { "Stage is already set" }
            field = value
        }

    protected fun cleanupSubscription(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }

    open fun beforeDispose(event: WindowEvent) = Unit

    open fun dispose() {
        compositeDisposable.dispose()
    }
}
