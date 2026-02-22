package com.olorin.claudette.services

import com.olorin.claudette.services.impl.AgentActivityParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AgentActivityParserTest {

    private lateinit var parser: AgentActivityParser

    @Before
    fun setUp() {
        parser = AgentActivityParser()
    }

    // --- Spawn lines ---

    @Test
    fun `parses Spawning agent line`() {
        feedLine("Spawning search-agent agent: Find all usages of deprecated API")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("search-agent", parser.rootNodes[0].agentType)
        assertEquals("Find all usages of deprecated API", parser.rootNodes[0].description)
    }

    @Test
    fun `parses Launching agent line`() {
        feedLine("Launching code-review agent: Review the pull request changes")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("code-review", parser.rootNodes[0].agentType)
        assertEquals("Review the pull request changes", parser.rootNodes[0].description)
    }

    @Test
    fun `parses Starting agent line`() {
        feedLine("Starting refactor agent: Rename variables across the codebase")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("refactor", parser.rootNodes[0].agentType)
    }

    // --- Completion lines ---

    @Test
    fun `parses Agent completed line`() {
        feedLine("Spawning search-agent agent: Find usages")
        feedLine("Agent search-agent completed")

        assertEquals(1, parser.rootNodes.size)
        assertTrue(parser.rootNodes[0].isCompleted)
    }

    @Test
    fun `parses Task finished line`() {
        feedLine("Spawning analyzer agent: Analyze code")
        feedLine("Task analyzer finished")

        assertEquals(1, parser.rootNodes.size)
        assertTrue(parser.rootNodes[0].isCompleted)
    }

    @Test
    fun `parses Agent done line`() {
        feedLine("Spawning builder agent: Build the module")
        feedLine("Agent builder done")

        assertEquals(1, parser.rootNodes.size)
        assertTrue(parser.rootNodes[0].isCompleted)
    }

    // --- Task tool invocation ---

    @Test
    fun `parses Task tool invocation with description`() {
        feedLine("Task(description: 'search-files'")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("search-files", parser.rootNodes[0].agentType)
    }

    @Test
    fun `parses Task tool invocation with subagent_type`() {
        feedLine("Task(subagent_type= \"code-review\"")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("code-review", parser.rootNodes[0].agentType)
    }

    // --- Parent-child relationships ---

    @Test
    fun `maintains parent-child relationships`() {
        feedLine("Spawning parent-agent agent: Parent task")
        feedLine("Spawning child-agent agent: Child task")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("parent-agent", parser.rootNodes[0].agentType)
        assertEquals(1, parser.rootNodes[0].children.size)
        assertEquals("child-agent", parser.rootNodes[0].children[0].agentType)
    }

    @Test
    fun `child completion pops stack correctly`() {
        feedLine("Spawning parent-agent agent: Parent task")
        feedLine("Spawning child-agent agent: Child task")
        feedLine("Agent child-agent completed")
        feedLine("Spawning sibling-agent agent: Sibling task")

        assertEquals(1, parser.rootNodes.size)
        assertEquals(2, parser.rootNodes[0].children.size)
        assertEquals("child-agent", parser.rootNodes[0].children[0].agentType)
        assertEquals("sibling-agent", parser.rootNodes[0].children[1].agentType)
        assertTrue(parser.rootNodes[0].children[0].isCompleted)
        assertFalse(parser.rootNodes[0].children[1].isCompleted)
    }

    @Test
    fun `deeply nested agents maintain hierarchy`() {
        feedLine("Spawning root-agent agent: Root")
        feedLine("Spawning mid-agent agent: Middle")
        feedLine("Spawning leaf-agent agent: Leaf")

        assertEquals(1, parser.rootNodes.size)
        assertEquals(1, parser.rootNodes[0].children.size)
        assertEquals(1, parser.rootNodes[0].children[0].children.size)
        assertEquals("leaf-agent", parser.rootNodes[0].children[0].children[0].agentType)
    }

    // --- activeAgentCount ---

    @Test
    fun `activeAgentCount starts at zero`() {
        assertEquals(0, parser.activeAgentCount)
    }

    @Test
    fun `activeAgentCount increments on spawn`() {
        feedLine("Spawning agent-a agent: Task A")
        assertEquals(1, parser.activeAgentCount)

        feedLine("Spawning agent-b agent: Task B")
        assertEquals(2, parser.activeAgentCount)
    }

    @Test
    fun `activeAgentCount decrements on completion`() {
        feedLine("Spawning agent-a agent: Task A")
        feedLine("Spawning agent-b agent: Task B")
        feedLine("Agent agent-b completed")

        assertEquals(1, parser.activeAgentCount)
    }

    @Test
    fun `activeAgentCount does not go below zero`() {
        feedLine("Agent phantom-agent completed")
        assertEquals(0, parser.activeAgentCount)
    }

    // --- ANSI handling ---

    @Test
    fun `handles ANSI escape sequences in spawn lines`() {
        feedLine("\u001B[32mSpawning \u001B[1msearch-agent\u001B[0m agent: Find files\u001B[0m")

        assertEquals(1, parser.rootNodes.size)
        assertEquals("search-agent", parser.rootNodes[0].agentType)
    }

    @Test
    fun `handles ANSI escape sequences in completion lines`() {
        feedLine("Spawning search-agent agent: Find files")
        feedLine("\u001B[33mAgent search-agent completed\u001B[0m")

        assertTrue(parser.rootNodes[0].isCompleted)
    }

    @Test
    fun `stripANSI removes escape sequences`() {
        val result = AgentActivityParser.stripANSI("\u001B[31mred\u001B[0m plain \u001B[1mbold\u001B[0m")
        assertEquals("red plain bold", result)
    }

    @Test
    fun `stripANSI returns plain text unchanged`() {
        assertEquals("plain text", AgentActivityParser.stripANSI("plain text"))
    }

    // --- Reset ---

    @Test
    fun `reset clears all state`() {
        feedLine("Spawning agent-a agent: Task A")
        feedLine("Spawning agent-b agent: Task B")

        parser.reset()

        assertEquals(0, parser.rootNodes.size)
        assertEquals(0, parser.activeAgentCount)
    }

    @Test
    fun `reset clears buffer so partial lines are discarded`() {
        // Feed bytes without a newline to leave data in the buffer
        parser.processOutput("Spawning partial-agent agent: Incomple".toByteArray(Charsets.UTF_8))
        parser.reset()

        // Feed a completion of a different line - should not interact with old buffer
        feedLine("Spawning fresh-agent agent: Fresh task")
        assertEquals(1, parser.rootNodes.size)
        assertEquals("fresh-agent", parser.rootNodes[0].agentType)
    }

    // --- Partial lines ---

    @Test
    fun `handles partial lines that arrive without trailing newline`() {
        parser.processOutput("Spawning search-agent agent:".toByteArray(Charsets.UTF_8))

        // No newline yet, should not have parsed
        assertEquals(0, parser.rootNodes.size)

        // Now complete the line
        parser.processOutput(" Find files\n".toByteArray(Charsets.UTF_8))

        assertEquals(1, parser.rootNodes.size)
        assertEquals("search-agent", parser.rootNodes[0].agentType)
    }

    @Test
    fun `handles multiple lines in single processOutput call`() {
        val multiLine = "Spawning agent-a agent: Task A\nSpawning agent-b agent: Task B\n"
        parser.processOutput(multiLine.toByteArray(Charsets.UTF_8))

        assertEquals(1, parser.rootNodes.size)
        assertEquals("agent-a", parser.rootNodes[0].agentType)
        assertEquals(1, parser.rootNodes[0].children.size)
        assertEquals("agent-b", parser.rootNodes[0].children[0].agentType)
    }

    // --- Edge cases ---

    @Test
    fun `completion for non-existent agent is a no-op`() {
        feedLine("Agent nonexistent completed")

        assertEquals(0, parser.rootNodes.size)
        assertEquals(0, parser.activeAgentCount)
    }

    @Test
    fun `multiple root agents when parent completes before next spawn`() {
        feedLine("Spawning agent-a agent: Task A")
        feedLine("Agent agent-a completed")
        feedLine("Spawning agent-b agent: Task B")

        assertEquals(2, parser.rootNodes.size)
        assertTrue(parser.rootNodes[0].isCompleted)
        assertFalse(parser.rootNodes[1].isCompleted)
    }

    @Test
    fun `completion matching is case insensitive`() {
        feedLine("Spawning MyAgent agent: A task")
        feedLine("Agent myagent completed")

        assertTrue(parser.rootNodes[0].isCompleted)
    }

    // --- Helpers ---

    private fun feedLine(line: String) {
        parser.processOutput("$line\n".toByteArray(Charsets.UTF_8))
    }
}
