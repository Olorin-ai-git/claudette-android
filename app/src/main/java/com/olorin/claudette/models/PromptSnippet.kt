package com.olorin.claudette.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PromptSnippet(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val category: SnippetCategory,
    val isBuiltIn: Boolean = false
)
