package com.vameli.vam.packagemanager.importer.vammodel

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FIELD

/**
 * Marker annotation for properties that are known to optionally refer to a dependency
 * (scene, plugin, clothing item, geometry item etc...).
 */
@Target(FIELD)
@Retention(RUNTIME)
annotation class DependencyRef

