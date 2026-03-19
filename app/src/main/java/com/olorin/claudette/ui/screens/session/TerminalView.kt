package com.olorin.claudette.ui.screens.session

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewTreeObserver
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
import com.olorin.claudette.services.impl.TerminalBlockDetector
import com.olorin.claudette.terminal.TerminalController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

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

    // Measure monospace char width for terminal size calculation
    val charMetrics = remember {
        val paint = Paint().apply {
            typeface = Typeface.MONOSPACE
            textSize = config.terminalFontSize * context.resources.displayMetrics.scaledDensity
        }
        val charWidth = paint.measureText("M")
        val lineHeight = paint.fontMetrics.let { it.bottom - it.top + it.leading }
        charWidth to lineHeight
    }

    // Track last sent terminal size to avoid duplicate resize calls
    val lastSentSize = remember { intArrayOf(0, 0) }

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

                    val lineHeight = this@apply.lineHeight.toFloat()
                    val scrollY = scrollView.scrollY
                    val tapY = e.y + scrollY
                    val lineIndex = (tapY / lineHeight).toInt().coerceIn(0, lines.size - 1)

                    val block = TerminalBlockDetector.detectBlock(lines, lineIndex)
                    if (block != null) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Terminal Block", block.content))
                        // Haptic feedback on successful copy
                        performCopyHaptic(context)
                        Toast.makeText(context, "Block copied", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
            })

            setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }

    DisposableEffect(controller) {
        // Collect SSH output and render
        val job = scope.launch {
            controller.renderTrigger.collect { bytes ->
                val text = stripAnsi(String(bytes, Charsets.UTF_8))
                textView.append(text)
                scrollView.post {
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }

        // Measure terminal size on layout changes and send resize
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val (charWidth, lineHeight) = charMetrics
            val viewWidth = scrollView.width - scrollView.paddingLeft - scrollView.paddingRight
            val viewHeight = scrollView.height - scrollView.paddingTop - scrollView.paddingBottom

            if (viewWidth > 0 && viewHeight > 0 && charWidth > 0 && lineHeight > 0) {
                val cols = (viewWidth / charWidth).toInt().coerceAtLeast(20)
                val rows = (viewHeight / lineHeight).toInt().coerceAtLeast(5)

                if (cols != lastSentSize[0] || rows != lastSentSize[1]) {
                    lastSentSize[0] = cols
                    lastSentSize[1] = rows
                    Timber.d("Terminal resize: %dx%d", cols, rows)
                    controller.resize(cols, rows)
                }
            }
        }
        scrollView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        onDispose {
            job.cancel()
            scrollView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
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

@Suppress("DEPRECATION")
private fun performCopyHaptic(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator.vibrate(
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(30)
    }
}
