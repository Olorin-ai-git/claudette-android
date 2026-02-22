package com.olorin.claudette.models

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionSettings(
    val host: String,
    val port: Int,
    val username: String,
    val authMethod: AuthMethod,
    val projectPath: String
)
