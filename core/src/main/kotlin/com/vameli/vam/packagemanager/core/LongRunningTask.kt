package com.vameli.vam.packagemanager.core

typealias ProgressListener<T> = (TaskProgress<T>) -> Any?

interface LongRunningTask<PROGRESS, RESULT> {
    fun execute(progressListener: ProgressListener<PROGRESS>): RESULT
}

data class TaskProgress<PROGRESS>(
    val progress: PROGRESS,
    val percentCompleted: Int? = null,
)
