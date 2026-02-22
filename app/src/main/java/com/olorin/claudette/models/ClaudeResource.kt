package com.olorin.claudette.models

import kotlinx.serialization.Serializable

@Serializable
enum class ClaudeResourceType(
    val displayTitle: String,
    val iconName: String,
    val directoryName: String
) {
    COMMAND("Commands", "terminal", "commands"),
    SKILL("Skills", "star", "skills"),
    AGENT("Agents", "memory", "agents");
}

data class ClaudeResource(
    val id: String,
    val name: String,
    val type: ClaudeResourceType,
    val description: String?,
    val isUserInvocable: Boolean,
    val filePath: String
) {
    val displayName: String
        get() = name
            .removeSuffix(".md")
            .replace("-", " ")
            .replace("_", " ")

    private val slug: String
        get() = name.removeSuffix(".md")

    val invokeCommand: String
        get() = when (type) {
            ClaudeResourceType.COMMAND, ClaudeResourceType.SKILL -> "/$slug"
            ClaudeResourceType.AGENT -> slug
        }

    val triggerCommand: String
        get() = when (type) {
            ClaudeResourceType.COMMAND, ClaudeResourceType.SKILL -> invokeCommand
            ClaudeResourceType.AGENT -> {
                if (!description.isNullOrEmpty()) {
                    "Run the $slug agent: $description"
                } else {
                    "Run the $slug agent"
                }
            }
        }
}
