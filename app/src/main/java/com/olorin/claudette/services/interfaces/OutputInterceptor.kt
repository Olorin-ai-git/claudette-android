package com.olorin.claudette.services.interfaces

fun interface OutputInterceptor {
    fun onOutput(bytes: ByteArray)
}
