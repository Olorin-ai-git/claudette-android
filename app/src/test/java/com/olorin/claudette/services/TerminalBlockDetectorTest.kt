package com.olorin.claudette.services

import com.olorin.claudette.services.impl.TerminalBlockDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalBlockDetectorTest {

    // --- isPromptLine ---

    @Test
    fun `isPromptLine detects dollar prompt`() {
        assertTrue(TerminalBlockDetector.isPromptLine("user@host:~$ "))
    }

    @Test
    fun `isPromptLine detects percent prompt`() {
        assertTrue(TerminalBlockDetector.isPromptLine("user@host ~ % "))
    }

    @Test
    fun `isPromptLine detects hash prompt`() {
        assertTrue(TerminalBlockDetector.isPromptLine("root@host:~# "))
    }

    @Test
    fun `isPromptLine detects angle bracket prompt`() {
        assertTrue(TerminalBlockDetector.isPromptLine("prompt> "))
    }

    @Test
    fun `isPromptLine detects prompt with ANSI codes`() {
        assertTrue(TerminalBlockDetector.isPromptLine("\u001B[32muser@host:~\u001B[0m$ "))
    }

    @Test
    fun `isPromptLine returns false for regular output`() {
        assertFalse(TerminalBlockDetector.isPromptLine("Hello, world!"))
    }

    @Test
    fun `isPromptLine returns false for empty line`() {
        assertFalse(TerminalBlockDetector.isPromptLine(""))
    }

    @Test
    fun `isPromptLine returns false for whitespace-only line`() {
        assertFalse(TerminalBlockDetector.isPromptLine("   "))
    }

    @Test
    fun `isPromptLine returns false for dollar in middle of line`() {
        assertFalse(TerminalBlockDetector.isPromptLine("The cost is $5 today"))
    }

    @Test
    fun `isPromptLine returns false for prompt without trailing space`() {
        // The suffixes require a trailing space: "$ ", "% ", "# ", "> "
        assertFalse(TerminalBlockDetector.isPromptLine("user@host:~$"))
    }

    // --- detectBlock ---

    @Test
    fun `detectBlock finds block between two prompts`() {
        val lines = listOf(
            "user@host:~$ ",        // line 0: prompt
            "ls -la",               // line 1: command output start
            "file1.txt",            // line 2: output
            "file2.txt",            // line 3: output
            "user@host:~$ "         // line 4: next prompt
        )

        val block = TerminalBlockDetector.detectBlock(lines, 2)

        assertNotNull(block)
        assertEquals(1, block!!.startLine)
        assertEquals(3, block.endLine)
        assertEquals("ls -la\nfile1.txt\nfile2.txt", block.content)
    }

    @Test
    fun `detectBlock with line in middle of block`() {
        val lines = listOf(
            "user@host:~$ ",
            "output line 1",
            "output line 2",
            "output line 3",
            "user@host:~$ "
        )

        val block = TerminalBlockDetector.detectBlock(lines, 1)

        assertNotNull(block)
        assertEquals(1, block!!.startLine)
        assertEquals(3, block.endLine)
    }

    @Test
    fun `detectBlock at first line when it is a prompt`() {
        val lines = listOf(
            "user@host:~$ ",
            "output"
        )

        // When atLine is the prompt line itself, the block starts at 0 (no prompt above)
        val block = TerminalBlockDetector.detectBlock(lines, 0)

        assertNotNull(block)
        assertEquals(0, block!!.startLine)
        assertEquals(1, block.endLine)
    }

    @Test
    fun `detectBlock at last line`() {
        val lines = listOf(
            "user@host:~$ ",
            "some output",
            "final line"
        )

        val block = TerminalBlockDetector.detectBlock(lines, 2)

        assertNotNull(block)
        assertEquals(1, block!!.startLine)
        assertEquals(2, block.endLine)
    }

    @Test
    fun `detectBlock with no prompts returns entire content`() {
        val lines = listOf(
            "output line 1",
            "output line 2",
            "output line 3"
        )

        val block = TerminalBlockDetector.detectBlock(lines, 1)

        assertNotNull(block)
        assertEquals(0, block!!.startLine)
        assertEquals(2, block.endLine)
        assertEquals("output line 1\noutput line 2\noutput line 3", block.content)
    }

    @Test
    fun `detectBlock returns null for out of bounds atLine`() {
        val lines = listOf("line 1", "line 2")

        val block = TerminalBlockDetector.detectBlock(lines, 5)

        assertNull(block)
    }

    @Test
    fun `detectBlock returns null for blank content`() {
        val lines = listOf(
            "user@host:~$ ",
            "",
            "   ",
            "user@host:~$ "
        )

        // Lines 1 and 2 are blank/whitespace, so the block content is blank
        val block = TerminalBlockDetector.detectBlock(lines, 1)

        assertNull(block)
    }

    @Test
    fun `detectBlock with empty list returns null`() {
        val block = TerminalBlockDetector.detectBlock(emptyList(), 0)

        assertNull(block)
    }

    @Test
    fun `detectBlock single non-prompt line`() {
        val lines = listOf("single output line")

        val block = TerminalBlockDetector.detectBlock(lines, 0)

        assertNotNull(block)
        assertEquals(0, block!!.startLine)
        assertEquals(0, block.endLine)
        assertEquals("single output line", block.content)
    }

    @Test
    fun `detectBlock with multiple blocks selects correct one`() {
        val lines = listOf(
            "user@host:~$ ",     // line 0: prompt
            "block1-line1",      // line 1
            "block1-line2",      // line 2
            "user@host:~$ ",     // line 3: prompt
            "block2-line1",      // line 4
            "block2-line2",      // line 5
            "user@host:~$ "      // line 6: prompt
        )

        val block1 = TerminalBlockDetector.detectBlock(lines, 1)
        assertNotNull(block1)
        assertEquals(1, block1!!.startLine)
        assertEquals(2, block1.endLine)
        assertEquals("block1-line1\nblock1-line2", block1.content)

        val block2 = TerminalBlockDetector.detectBlock(lines, 5)
        assertNotNull(block2)
        assertEquals(4, block2!!.startLine)
        assertEquals(5, block2.endLine)
        assertEquals("block2-line1\nblock2-line2", block2.content)
    }

    // --- stripANSI ---

    @Test
    fun `stripANSI removes color codes`() {
        val input = "\u001B[31mred text\u001B[0m"
        assertEquals("red text", TerminalBlockDetector.stripANSI(input))
    }

    @Test
    fun `stripANSI removes multiple escape sequences`() {
        val input = "\u001B[1m\u001B[32mbold green\u001B[0m normal"
        assertEquals("bold green normal", TerminalBlockDetector.stripANSI(input))
    }

    @Test
    fun `stripANSI removes cursor movement codes`() {
        val input = "\u001B[2Jcleared\u001B[H"
        assertEquals("cleared", TerminalBlockDetector.stripANSI(input))
    }

    @Test
    fun `stripANSI returns plain text unchanged`() {
        val input = "no escape sequences here"
        assertEquals("no escape sequences here", TerminalBlockDetector.stripANSI(input))
    }

    @Test
    fun `stripANSI handles empty string`() {
        assertEquals("", TerminalBlockDetector.stripANSI(""))
    }

    @Test
    fun `stripANSI removes SGR codes with multiple parameters`() {
        val input = "\u001B[38;5;196mextended color\u001B[0m"
        assertEquals("extended color", TerminalBlockDetector.stripANSI(input))
    }
}
