package com.olorin.claudette.services.impl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Patterns
import com.olorin.claudette.terminal.AnsiUtils
import timber.log.Timber

class AuthUrlInterceptor(
    private val context: Context
) {

    var detectedUrl: String? = null
        private set

    private var lineBuffer: String = ""
    private var lastCopiedUrl: String? = null

    private val authDomains = listOf("anthropic.com", "claude.ai")

    fun processOutput(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        lineBuffer += text

        var newlineIndex = lineBuffer.indexOf('\n')
        while (newlineIndex != -1) {
            val line = lineBuffer.substring(0, newlineIndex)
            lineBuffer = lineBuffer.substring(newlineIndex + 1)
            scanForAuthUrl(line)
            newlineIndex = lineBuffer.indexOf('\n')
        }

        // Scan partial buffer for URLs that arrive without a trailing newline
        if (lineBuffer.length > 20) {
            scanForAuthUrl(lineBuffer)
        }
    }

    fun clearDetectedUrl() {
        detectedUrl = null
    }

    private fun scanForAuthUrl(text: String) {
        val cleaned = stripANSI(text)
        val matcher = Patterns.WEB_URL.matcher(cleaned)

        while (matcher.find()) {
            val urlString = matcher.group() ?: continue
            val lowercaseUrl = urlString.lowercase()

            val isAuthDomain = authDomains.any { domain -> lowercaseUrl.contains(domain) }
            if (!isAuthDomain) continue

            if (urlString == lastCopiedUrl) continue

            lastCopiedUrl = urlString
            copyToClipboard(urlString)
            detectedUrl = urlString
            Timber.i("Auth URL detected and copied to clipboard")
            return
        }
    }

    private fun copyToClipboard(url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val clip = ClipData.newPlainText("Auth URL", url)
        clipboard.setPrimaryClip(clip)
    }

    companion object {
        fun stripANSI(text: String): String = AnsiUtils.stripANSI(text)
    }
}
