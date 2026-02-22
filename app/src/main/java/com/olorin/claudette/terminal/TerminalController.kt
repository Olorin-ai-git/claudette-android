package com.olorin.claudette.terminal

import com.olorin.claudette.services.impl.SshConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Bridge between SshConnectionManager output flow and the terminal emulator view.
 * Collects SSH output bytes and exposes them as a render trigger for Compose.
 */
class TerminalController(
    private val connectionManager: SshConnectionManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _renderTrigger = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val renderTrigger: SharedFlow<ByteArray> = _renderTrigger.asSharedFlow()

    private var collectJob: Job? = null

    // Output line buffer for block detection
    private val outputLines = mutableListOf<String>()
    private val maxLines = 5000
    private var currentLine = StringBuilder()

    fun start() {
        if (collectJob != null) return

        collectJob = scope.launch {
            connectionManager.outputFlow.collect { bytes ->
                appendForBlockDetection(bytes)
                _renderTrigger.emit(bytes)
            }
        }
        Timber.i("TerminalController started collecting output")
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        Timber.i("TerminalController stopped")
    }

    fun sendInput(data: ByteArray) {
        connectionManager.sendToRemote(data)
    }

    fun sendInput(text: String) {
        sendInput(text.toByteArray(Charsets.UTF_8))
    }

    fun resize(cols: Int, rows: Int) {
        connectionManager.resizeTerminal(cols, rows)
    }

    fun getOutputLines(): List<String> = synchronized(outputLines) {
        outputLines.toList()
    }

    fun getTerminalContent(): String {
        return connectionManager.getTerminalContent()
    }

    private fun appendForBlockDetection(bytes: ByteArray) {
        val text = String(bytes, Charsets.UTF_8)
        synchronized(outputLines) {
            for (char in text) {
                if (char == '\n') {
                    outputLines.add(currentLine.toString())
                    currentLine.clear()
                    if (outputLines.size > maxLines) {
                        outputLines.removeAt(0)
                    }
                } else {
                    currentLine.append(char)
                }
            }
        }
    }
}
