package com.olorin.claudette.models

data class RemoteFileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long? = null,
    val modifiedAt: Long? = null
) {
    val id: String get() = path
}
