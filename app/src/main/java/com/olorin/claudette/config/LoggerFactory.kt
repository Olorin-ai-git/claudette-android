package com.olorin.claudette.config

import timber.log.Timber

object LoggerFactory {

    fun logger(category: String): Timber.Tree {
        return Timber.tag(category)
    }

    fun d(category: String, message: String) {
        Timber.tag(category).d(message)
    }

    fun i(category: String, message: String) {
        Timber.tag(category).i(message)
    }

    fun w(category: String, message: String) {
        Timber.tag(category).w(message)
    }

    fun e(category: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(category).e(throwable, message)
        } else {
            Timber.tag(category).e(message)
        }
    }
}
