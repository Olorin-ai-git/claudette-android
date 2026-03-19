package com.olorin.claudette.services.impl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.olorin.claudette.terminal.AnsiUtils
import timber.log.Timber
import java.util.UUID

class PermissionNotificationService(
    private val context: Context
) {

    private var buffer: String = ""

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun processOutput(bytes: ByteArray) {
        val text = bytes.toString(Charsets.UTF_8)
        buffer += text

        var newlineIndex = buffer.indexOf('\n')
        while (newlineIndex != -1) {
            val line = buffer.substring(0, newlineIndex)
            buffer = buffer.substring(newlineIndex + 1)
            checkForPermissionPrompt(line)
            newlineIndex = buffer.indexOf('\n')
        }

        // Check partial buffer for permission patterns that arrive without trailing newline
        if (buffer.length > 200) {
            checkForPermissionPrompt(buffer)
            buffer = ""
        }
    }

    private fun checkForPermissionPrompt(text: String) {
        val stripped = stripANSI(text)

        val matchCount = PERMISSION_PATTERNS.count { pattern -> stripped.contains(pattern) }
        if (matchCount < REQUIRED_PATTERN_MATCHES) return

        sendNotification(stripped)
    }

    private fun sendNotification(prompt: String) {
        val truncatedBody = if (prompt.length > MAX_NOTIFICATION_BODY_LENGTH) {
            prompt.substring(0, MAX_NOTIFICATION_BODY_LENGTH)
        } else {
            prompt
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(truncatedBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(truncatedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        val notificationId = UUID.randomUUID().hashCode()
        notificationManager.notify(notificationId, notification)
        Timber.i("Sent permission notification")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "claude_permissions"
        private const val CHANNEL_NAME = "Claude Permissions"
        private const val CHANNEL_DESCRIPTION = "Notifications for Claude Code permission prompts"
        private const val NOTIFICATION_TITLE = "Claude Code Permission"
        private const val MAX_NOTIFICATION_BODY_LENGTH = 200
        private const val REQUIRED_PATTERN_MATCHES = 2

        private val PERMISSION_PATTERNS = listOf(
            "Allow",
            "Deny",
            "Do you want to",
            "Permission requested",
            "approve",
            "y/n",
            "Y/N",
            "(y)es/(n)o"
        )

        fun stripANSI(text: String): String = AnsiUtils.stripANSI(text)
    }
}
