package com.olorin.claudette.config

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Properties

@Serializable
data class KeyboardButtonConfig(
    val label: String,
    val byteSequence: List<Int>,
    val action: String? = null
)

class AppConfiguration(context: Context) {

    // SSH
    val sshDefaultPort: Int
    val sshConnectTimeout: Int
    val sshCommand: String

    // Bonjour/mDNS
    val bonjourServiceType: String
    val bonjourDomain: String

    // Terminal
    val terminalFontName: String
    val terminalFontSize: Float
    val terminalForegroundColor: String
    val terminalBackgroundColor: String
    val terminalCaretColor: String
    val terminalTermType: String
    val terminalDefaultColumns: Int
    val terminalDefaultRows: Int

    // Splash
    val splashAccentColor: String
    val splashAccentColorLight: String
    val splashAccentColorDark: String
    val splashBackgroundColor: String
    val splashAppName: String
    val splashCursorSymbol: String
    val splashSlogan: String
    val splashFooterText: String

    // Keychain/Secrets
    val keychainServiceName: String

    // Logger
    val loggerTag: String

    // Keyboard Accessory
    val keyboardAccessoryHeight: Float
    val keyboardAccessoryBackgroundColor: String
    val keyboardAccessoryButtonColor: String
    val keyboardAccessoryButtonTextColor: String
    val keyboardAccessoryButtons: List<KeyboardButtonConfig>

    // Session Persistence (tmux)
    val tmuxEnabled: Boolean
    val tmuxSessionPrefix: String
    val reconnectMaxAttempts: Int
    val reconnectDelaySeconds: Double

    // Network Probe
    val networkProbeIntervalSeconds: Double
    val networkProbeTimeoutSeconds: Double
    val networkProbeDegradedThresholdMs: Double

    // File Editor
    val fileEditorMaxSizeBytes: Int
    val fileEditorFontName: String
    val fileEditorFontSize: Float

    // Snippets
    val snippetsStorageFileName: String

    init {
        val props = Properties()
        context.assets.open("config.properties").use { props.load(it) }

        sshDefaultPort = requiredInt(props, "ssh.default_port")
        sshConnectTimeout = requiredInt(props, "ssh.connect_timeout_seconds")
        sshCommand = requiredString(props, "ssh.command")

        bonjourServiceType = requiredString(props, "bonjour.service_type")
        bonjourDomain = props.getProperty("bonjour.domain", "")

        terminalFontName = requiredString(props, "terminal.font_name")
        terminalFontSize = requiredFloat(props, "terminal.font_size")
        terminalForegroundColor = requiredString(props, "terminal.foreground_color")
        terminalBackgroundColor = requiredString(props, "terminal.background_color")
        terminalCaretColor = requiredString(props, "terminal.caret_color")
        terminalTermType = requiredString(props, "terminal.term_type")
        terminalDefaultColumns = requiredInt(props, "terminal.default_columns")
        terminalDefaultRows = requiredInt(props, "terminal.default_rows")

        splashAccentColor = requiredString(props, "splash.accent_color")
        splashAccentColorLight = requiredString(props, "splash.accent_color_light")
        splashAccentColorDark = requiredString(props, "splash.accent_color_dark")
        splashBackgroundColor = requiredString(props, "splash.background_color")
        splashAppName = requiredString(props, "splash.app_name")
        splashCursorSymbol = requiredString(props, "splash.cursor_symbol")
        splashSlogan = requiredString(props, "splash.slogan")
        splashFooterText = requiredString(props, "splash.footer_text")

        keychainServiceName = requiredString(props, "keychain.service_name")
        loggerTag = requiredString(props, "logger.tag")

        keyboardAccessoryHeight = requiredFloat(props, "keyboard.accessory_height")
        keyboardAccessoryBackgroundColor = requiredString(props, "keyboard.accessory_background_color")
        keyboardAccessoryButtonColor = requiredString(props, "keyboard.accessory_button_color")
        keyboardAccessoryButtonTextColor = requiredString(props, "keyboard.accessory_button_text_color")

        keyboardAccessoryButtons = loadKeyboardButtons(context)

        tmuxEnabled = props.getProperty("session.tmux_enabled", "false").toBoolean()
        tmuxSessionPrefix = requiredString(props, "session.tmux_session_prefix")
        reconnectMaxAttempts = requiredInt(props, "session.reconnect_max_attempts")
        reconnectDelaySeconds = requiredDouble(props, "session.reconnect_delay_seconds")

        networkProbeIntervalSeconds = requiredDouble(props, "network_probe.interval_seconds")
        networkProbeTimeoutSeconds = requiredDouble(props, "network_probe.timeout_seconds")
        networkProbeDegradedThresholdMs = requiredDouble(props, "network_probe.degraded_threshold_ms")

        fileEditorMaxSizeBytes = requiredInt(props, "file_editor.max_size_bytes")
        fileEditorFontName = requiredString(props, "file_editor.font_name")
        fileEditorFontSize = requiredFloat(props, "file_editor.font_size")

        snippetsStorageFileName = requiredString(props, "snippets.storage_file_name")
    }

    private fun loadKeyboardButtons(context: Context): List<KeyboardButtonConfig> {
        val json = context.assets.open("keyboard_buttons.json")
            .bufferedReader()
            .use { it.readText() }
        return Json.decodeFromString<List<KeyboardButtonConfig>>(json)
    }

    private fun requiredString(props: Properties, key: String): String {
        return props.getProperty(key)
            ?: error("Missing required configuration value: $key")
    }

    private fun requiredInt(props: Properties, key: String): Int {
        val value = requiredString(props, key)
        return value.toIntOrNull()
            ?: error("Invalid integer configuration value for $key: $value")
    }

    private fun requiredFloat(props: Properties, key: String): Float {
        val value = requiredString(props, key)
        return value.toFloatOrNull()
            ?: error("Invalid float configuration value for $key: $value")
    }

    private fun requiredDouble(props: Properties, key: String): Double {
        val value = requiredString(props, key)
        return value.toDoubleOrNull()
            ?: error("Invalid double configuration value for $key: $value")
    }
}
