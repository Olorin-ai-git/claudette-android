package com.olorin.claudette.models

data class AgentTreeNode(
    val id: String,
    val agentType: String,
    val description: String,
    val startTime: Long = System.currentTimeMillis(),
    var isCompleted: Boolean = false,
    val children: MutableList<AgentTreeNode> = mutableListOf()
) {
    val durationSeconds: Int
        get() = ((System.currentTimeMillis() - startTime) / 1000).toInt()

    val displayDuration: String
        get() {
            val seconds = durationSeconds
            return if (seconds < 60) {
                "${seconds}s"
            } else {
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
                "${minutes}m ${remainingSeconds}s"
            }
        }
}
