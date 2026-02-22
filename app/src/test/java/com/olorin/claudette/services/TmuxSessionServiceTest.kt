package com.olorin.claudette.services

import com.olorin.claudette.services.impl.TmuxSessionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TmuxSessionServiceTest {

    private lateinit var service: TmuxSessionService

    @Before
    fun setUp() {
        service = TmuxSessionService()
    }

    // --- sessionName ---

    @Test
    fun `sessionName generates prefix-shortId format`() {
        val result = service.sessionName("abcdef1234567890", "claudette")
        assertEquals("claudette-abcdef12", result)
    }

    @Test
    fun `sessionName lowercases the short id`() {
        val result = service.sessionName("ABCDEF1234567890", "prefix")
        assertEquals("prefix-abcdef12", result)
    }

    @Test
    fun `sessionName with short profileId uses full id`() {
        val result = service.sessionName("abc", "cc")
        assertEquals("cc-abc", result)
    }

    @Test
    fun `sessionName with exactly 8 character profileId`() {
        val result = service.sessionName("12345678", "tmux")
        assertEquals("tmux-12345678", result)
    }

    // --- checkTmuxCommand ---

    @Test
    fun `checkTmuxCommand returns which tmux`() {
        val result = service.checkTmuxCommand()
        assertEquals("which tmux", result)
    }

    // --- hasSessionCommand ---

    @Test
    fun `hasSessionCommand wraps session name in single quotes`() {
        val result = service.hasSessionCommand("my-session")
        assertEquals("tmux has-session -t 'my-session' 2>/dev/null", result)
    }

    @Test
    fun `hasSessionCommand escapes single quotes in session name`() {
        val result = service.hasSessionCommand("it's-a-session")
        assertEquals("tmux has-session -t 'it'\\''s-a-session' 2>/dev/null", result)
    }

    // --- attachCommand ---

    @Test
    fun `attachCommand generates proper tmux attach`() {
        val result = service.attachCommand("my-session")
        assertEquals("tmux attach-session -t 'my-session'", result)
    }

    @Test
    fun `attachCommand escapes special characters`() {
        val result = service.attachCommand("session with spaces")
        assertEquals("tmux attach-session -t 'session with spaces'", result)
    }

    // --- newSessionCommand ---

    @Test
    fun `newSessionCommand includes session directory and command`() {
        val result = service.newSessionCommand("sess", "/home/user/project", "claude --continue")
        assertEquals(
            "tmux new-session -s 'sess' -c '/home/user/project' 'claude --continue'",
            result
        )
    }

    @Test
    fun `newSessionCommand escapes all arguments`() {
        val result = service.newSessionCommand(
            "it's",
            "/path/with spaces",
            "echo 'hello'"
        )
        assertEquals(
            "tmux new-session -s 'it'\\''s' -c '/path/with spaces' 'echo '\\''hello'\\'''",
            result
        )
    }

    // --- attachOrCreateCommand ---

    @Test
    fun `attachOrCreateCommand contains tmux command check`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should check for tmux availability",
            result.contains("command -v tmux")
        )
    }

    @Test
    fun `attachOrCreateCommand contains has-session check`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should check for existing session",
            result.contains("tmux has-session")
        )
    }

    @Test
    fun `attachOrCreateCommand contains attach-session`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should contain attach-session command",
            result.contains("tmux attach-session")
        )
    }

    @Test
    fun `attachOrCreateCommand contains new-session`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should contain new-session command",
            result.contains("tmux new-session")
        )
    }

    @Test
    fun `attachOrCreateCommand contains direct fallback`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should contain cd fallback when tmux is unavailable",
            result.contains("cd '/home'")
        )
        assertTrue(
            "Should exec the command in fallback",
            result.contains("exec 'claude'")
        )
    }

    @Test
    fun `attachOrCreateCommand contains restart logic for dead panes`() {
        val result = service.attachOrCreateCommand("sess", "/home", "claude")
        assertTrue(
            "Should check pane_current_command for running claude process",
            result.contains("pane_current_command")
        )
        assertTrue(
            "Should send-keys to restart if claude is not running",
            result.contains("tmux send-keys")
        )
    }

    // --- Special characters ---

    @Test
    fun `commands handle session names with backticks`() {
        val result = service.hasSessionCommand("session`inject`")
        assertEquals("tmux has-session -t 'session`inject`' 2>/dev/null", result)
    }

    @Test
    fun `commands handle session names with dollar signs`() {
        val result = service.attachCommand("session\$HOME")
        assertEquals("tmux attach-session -t 'session\$HOME'", result)
    }

    @Test
    fun `commands handle session names with double quotes`() {
        val result = service.hasSessionCommand("session\"name")
        assertEquals("tmux has-session -t 'session\"name' 2>/dev/null", result)
    }

    @Test
    fun `commands handle empty session name`() {
        val result = service.attachCommand("")
        assertEquals("tmux attach-session -t ''", result)
    }

    @Test
    fun `attachOrCreateCommand with single quote in all fields`() {
        val result = service.attachOrCreateCommand("it's", "/it's/path", "echo 'hi'")
        assertFalse(
            "Should not contain unescaped single quotes that would break shell",
            result.contains("'''")
        )
    }
}
