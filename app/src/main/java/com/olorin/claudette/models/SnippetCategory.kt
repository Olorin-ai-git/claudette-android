package com.olorin.claudette.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SnippetCategory(val displayName: String, val iconName: String) {
    @SerialName("Claude Commands")
    CLAUDE_COMMANDS("Claude Commands", "terminal"),

    @SerialName("Refactoring")
    REFACTORING("Refactoring", "sync"),

    @SerialName("Debugging")
    DEBUGGING("Debugging", "bug_report"),

    @SerialName("Git")
    GIT("Git", "commit"),

    @SerialName("Custom")
    CUSTOM("Custom", "star");
}
