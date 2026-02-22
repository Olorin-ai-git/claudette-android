package com.olorin.claudette.ui.screens.session

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.models.TerminalBlock
import com.olorin.claudette.services.impl.TerminalBlockDetector
import com.olorin.claudette.terminal.TerminalController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Terminal view using a simple TextView-based renderer.
 * This renders SSH output as monospaced text with ANSI stripping.
 *
 * For a full terminal emulator (xterm-256color with cursor addressing),
 * integrate Termux terminal-emulator library here. This implementation
 * provides a functional terminal for text-based SSH workflows like
 * Claude Code.
 */
@Composable
fun TerminalView(
    controller: TerminalController,
    config: AppConfiguration,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }

    val scrollView = remember {
        ScrollView(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor(config.terminalBackgroundColor))
            isFillViewport = true
        }
    }

    val textView = remember {
        TextView(context).apply {
            typeface = Typeface.MONOSPACE
            textSize = config.terminalFontSize
            setTextColor(android.graphics.Color.parseColor(config.terminalForegroundColor))
            setBackgroundColor(android.graphics.Color.parseColor(config.terminalBackgroundColor))
            setPadding(8, 8, 8, 8)
            isFocusable = true
            isFocusableInTouchMode = true

            // Double-tap gesture for block detection and copy
            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    val lines = controller.getOutputLines()
                    if (lines.isEmpty()) return true

                    // Estimate which line was tapped
                    val lineHeight = this@apply.lineHeight.toFloat()
                    val scrollY = scrollView.scrollY
                    val tapY = e.y + scrollY
                    val lineIndex = (tapY / lineHeight).toInt().coerceIn(0, lines.size - 1)

                    val block = TerminalBlockDetector.detectBlock(lines, lineIndex)
                    if (block != null) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Terminal Block", block.content))
                        Toast.makeText(context, "Block copied", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            })

            setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }

    DisposableEffect(controller) {
        val job = scope.launch {
            controller.renderTrigger.collect { bytes ->
                val text = stripAnsi(String(bytes, Charsets.UTF_8))
                textView.append(text)

                // Auto-scroll to bottom
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
        onDispose { job.cancel() }
    }

    AndroidView(
        factory = {
            scrollView.apply {
                removeAllViews()
                addView(textView)
            }
        },
        modifier = modifier
    )
}

private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*[a-zA-Z]")

private fun stripAnsi(text: String): String {
    return ANSI_REGEX.replace(text, "")
}
