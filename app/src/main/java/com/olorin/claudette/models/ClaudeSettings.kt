package com.olorin.claudette.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
data class ClaudeSettings(
    val hooks: Map<String, List<ClaudeHook>>? = null
)

@Serializable
data class ClaudeHook(
    @Transient val id: String = UUID.randomUUID().toString(),
    val type: String,
    val command: String,
    val matcher: String? = null
)
