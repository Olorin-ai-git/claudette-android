package com.olorin.claudette.models

import java.util.UUID

data class TerminalTab(
    val id: String = UUID.randomUUID().toString(),
    val connectionManagerId: String,
    val label: String
)
