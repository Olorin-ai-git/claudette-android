package com.olorin.claudette.terminal

/**
 * Thread-safe rolling line buffer that accumulates text from byte chunks
 * and maintains a capped list of lines for block detection.
 */
class LineBuffer(private val maxLines: Int = 5000) {

    private val lines = mutableListOf<String>()
    private val currentLine = StringBuilder()

    fun append(chunk: ByteArray) {
        val text = String(chunk, Charsets.UTF_8)
        synchronized(lines) {
            for (char in text) {
                if (char == '\n') {
                    lines.add(currentLine.toString())
                    currentLine.clear()
                    if (lines.size > maxLines) {
                        lines.removeAt(0)
                    }
                } else {
                    currentLine.append(char)
                }
            }
        }
    }

    fun getLines(): List<String> = synchronized(lines) {
        lines.toList()
    }

    fun getContent(): String = synchronized(lines) {
        lines.joinToString("\n")
    }
}
