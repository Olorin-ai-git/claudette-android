package com.olorin.claudette.services.impl

import com.olorin.claudette.models.AgentTreeNode
import com.olorin.claudette.terminal.AnsiUtils
import timber.log.Timber
import java.util.UUID

class AgentActivityParser {

    val rootNodes: MutableList<AgentTreeNode> = mutableListOf()

    var activeAgentCount: Int = 0
        private set

    private var buffer: String = ""
    private val nodeStack: MutableList<AgentTreeNode> = mutableListOf()

    private val spawnPattern: Regex = Regex(
        """(?:Launching|Spawning|Starting)\s+(\w[\w-]*)\s+agent.*?[:\u2026](.+?)$""",
        RegexOption.MULTILINE
    )

    private val completePattern: Regex = Regex(
        """(?:Agent|Task)\s+(\w[\w-]*)\s+(?:completed|finished|done)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    private val taskToolPattern: Regex = Regex(
        """Task\s*\(\s*(?:description|subagent_type)\s*[:=]\s*["']?(\w[\w\s-]*)["']?""",
        RegexOption.MULTILINE
    )

    fun processOutput(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        buffer += text

        var newlineIndex = buffer.indexOf('\n')
        while (newlineIndex != -1) {
            val line = buffer.substring(0, newlineIndex)
            buffer = buffer.substring(newlineIndex + 1)
            parseLine(line)
            newlineIndex = buffer.indexOf('\n')
        }
    }

    fun reset() {
        rootNodes.clear()
        nodeStack.clear()
        activeAgentCount = 0
        buffer = ""
    }

    private fun parseLine(line: String) {
        val stripped = stripANSI(line)

        val spawnMatch = spawnPattern.find(stripped)
        if (spawnMatch != null) {
            val agentType = spawnMatch.groupValues[1]
            val description = spawnMatch.groupValues[2]
            spawnAgent(agentType, description)
            return
        }

        val taskMatch = taskToolPattern.find(stripped)
        if (taskMatch != null) {
            val agentType = taskMatch.groupValues[1]
            spawnAgent(agentType, agentType)
            return
        }

        val completeMatch = completePattern.find(stripped)
        if (completeMatch != null) {
            val agentType = completeMatch.groupValues[1]
            completeAgent(agentType)
        }
    }

    private fun spawnAgent(type: String, description: String) {
        val node = AgentTreeNode(
            id = UUID.randomUUID().toString(),
            agentType = type.trim(),
            description = description.trim()
        )

        val parent = nodeStack.lastOrNull()
        if (parent != null) {
            parent.children.add(node)
        } else {
            rootNodes.add(node)
        }

        nodeStack.add(node)
        activeAgentCount++
        Timber.d("Agent spawned: %s", type)
    }

    private fun completeAgent(type: String) {
        val trimmedType = type.trim()

        for (i in nodeStack.indices.reversed()) {
            if (nodeStack[i].agentType.equals(trimmedType, ignoreCase = true)) {
                nodeStack[i].isCompleted = true
                nodeStack.removeAt(i)
                activeAgentCount = maxOf(0, activeAgentCount - 1)
                Timber.d("Agent completed: %s", type)
                return
            }
        }
    }

    companion object {
        fun stripANSI(text: String): String = AnsiUtils.stripANSI(text)
    }
}
