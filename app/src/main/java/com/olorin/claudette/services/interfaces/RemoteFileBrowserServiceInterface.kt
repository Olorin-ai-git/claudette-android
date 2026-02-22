package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.RemoteFileEntry
import kotlinx.coroutines.flow.StateFlow

interface RemoteFileBrowserServiceInterface {
    val isConnected: StateFlow<Boolean>

    suspend fun connect(
        host: String,
        port: Int,
        username: String,
        password: String?,
        privateKeyData: ByteArray?
    )

    suspend fun listDirectory(atPath: String): List<RemoteFileEntry>
    suspend fun getHomeDirectory(username: String): String
    suspend fun readFile(atPath: String): ByteArray
    suspend fun writeFile(data: ByteArray, atPath: String)
    suspend fun fileExists(atPath: String): Boolean
    fun disconnect()
}
