package com.olorin.claudette.services.impl

import com.olorin.claudette.models.TerminalBlock
import com.olorin.claudette.terminal.AnsiUtils

object TerminalBlockDetector {

    private val promptSuffixes = listOf("$ ", "% ", "# ", "> ")

    fun isPromptLine(line: String): Boolean {
        val stripped = stripANSI(line).trim()
        if (stripped.isEmpty()) return false
        return promptSuffixes.any { suffix -> stripped.endsWith(suffix) }
    }

    fun detectBlock(lines: List<String>, atLine: Int): TerminalBlock? {
        if (atLine >= lines.size) return null

        // Search upward for block start (stop at prompt line above)
        var startLine = atLine
        while (startLine > 0) {
            if (isPromptLine(lines[startLine - 1])) {
                break
            }
            startLine--
        }

        // Search downward for block end (stop before next prompt)
        var endLine = atLine
        while (endLine < lines.size - 1) {
            if (isPromptLine(lines[endLine + 1])) {
                break
            }
            endLine++
        }

        val blockLines = lines.subList(startLine, endLine + 1)
        val content = blockLines.joinToString("\n")

        if (content.isBlank()) return null

        return TerminalBlock(
            startLine = startLine,
            endLine = endLine,
            content = content
        )
    }

    fun stripANSI(text: String): String = AnsiUtils.stripANSI(text)
}
