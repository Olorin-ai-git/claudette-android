package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.ClaudeResource

interface ClaudeResourceDiscoveryServiceInterface {
    suspend fun discover(projectPath: String, username: String): List<ClaudeResource>
}
