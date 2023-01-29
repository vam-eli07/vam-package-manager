package com.vameli.vam.packagemanager.core

import org.slf4j.LoggerFactory

fun Any.logger() = LoggerFactory.getLogger(this.javaClass)
inline fun <reified T> logger(clazz: Class<T>) = LoggerFactory.getLogger(T::class.java)
