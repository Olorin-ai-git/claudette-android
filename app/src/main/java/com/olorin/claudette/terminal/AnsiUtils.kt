package com.olorin.claudette.terminal

object AnsiUtils {
    private val ansiPattern = Regex("\u001B\\[[0-9;]*[a-zA-Z]")

    fun stripANSI(text: String): String {
        return ansiPattern.replace(text, "")
    }
}
