package com.olorin.claudette.models

import kotlinx.serialization.Serializable

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val authMethod: AuthMethod,
    val lastProjectPath: String? = null,
    val macAddress: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null
) {
    fun toConnectionSettings(projectPath: String): ConnectionSettings =
        ConnectionSettings(
            host = host,
            port = port,
            username = username,
            authMethod = authMethod,
            projectPath = projectPath
        )

    companion object {
        fun create(
            name: String,
            host: String,
            port: Int,
            username: String,
            authMethod: AuthMethod,
            lastProjectPath: String? = null,
            macAddress: String? = null
        ): ServerProfile = ServerProfile(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            host = host,
            port = port,
            username = username,
            authMethod = authMethod,
            lastProjectPath = lastProjectPath,
            macAddress = macAddress
        )
    }
}
