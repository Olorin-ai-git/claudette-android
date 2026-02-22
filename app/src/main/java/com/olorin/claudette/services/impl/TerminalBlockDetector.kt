package com.olorin.claudette.services.impl

import com.olorin.claudette.models.TerminalBlock

object TerminalBlockDetector {

    private val promptSuffixes = listOf("$ ", "% ", "# ", "> ")

    private val ansiPattern = Regex("\u001B\\[[0-9;]*[a-zA-Z]")

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

    fun stripANSI(text: String): String {
        return ansiPattern.replace(text, "")
    }
}
