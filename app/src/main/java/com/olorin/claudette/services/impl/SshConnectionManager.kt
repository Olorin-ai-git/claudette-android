package com.olorin.claudette.services.impl

import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.models.AuthMethod
import com.olorin.claudette.models.ConnectionSettings
import com.olorin.claudette.models.ConnectionState
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.OutputInterceptor
import com.olorin.claudette.services.interfaces.TmuxSessionServiceInterface
import com.olorin.claudette.terminal.LineBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.channel.PtyChannelConfiguration
import org.apache.sshd.sftp.client.SftpClientFactory
import timber.log.Timber
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

class SshConnectionManager(
    private val config: AppConfiguration,
    private val keychainService: KeychainServiceInterface,
    private val tmuxService: TmuxSessionServiceInterface,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    var serverKeyVerifier: ServerKeyVerifier? = null
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _outputFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256)
    val outputFlow: SharedFlow<ByteArray> = _outputFlow.asSharedFlow()

    var outputInterceptor: OutputInterceptor? = null

    private var sshClient: SshClient? = null
    private var session: ClientSession? = null
    private var shellChannel: ChannelShell? = null
    private var stdinStream: OutputStream? = null
    private var connectionJob: Job? = null

    private var lastSettings: ConnectionSettings? = null
    private var lastCredential: String? = null
    private var lastProfileId: String? = null

    private val lineBuffer = LineBuffer()

    fun getTerminalContent(): String = lineBuffer.getContent()

    fun connect(
        settings: ConnectionSettings,
        credential: String,
        profileId: String? = null
    ) {
        when (_connectionState.value) {
            is ConnectionState.Connecting,
            is ConnectionState.Connected,
            is ConnectionState.Reconnecting -> return
            else -> {}
        }

        if (credential.isEmpty() && !settings.authMethod.isKeyBased) {
            _connectionState.value = ConnectionState.Failed("Missing credential")
            return
        }

        lastSettings = settings
        lastCredential = credential
        lastProfileId = profileId

        _connectionState.value = ConnectionState.Connecting
        Timber.i("Connecting to %s:%d", settings.host, settings.port)

        val sessionName: String? = if (config.tmuxEnabled && profileId != null) {
            tmuxService.sessionName(profileId, config.tmuxSessionPrefix)
        } else {
            null
        }

        connectionJob = scope.launch {
            try {
                val client = SshClient.setUpDefaultClient()
                serverKeyVerifier?.let { client.serverKeyVerifier = it }
                client.start()
                sshClient = client

                val sess = client.connect(
                    settings.username,
                    settings.host,
                    settings.port
                ).verify(config.sshConnectTimeout.toLong(), TimeUnit.SECONDS).session

                // Authenticate
                when (settings.authMethod) {
                    is AuthMethod.Password -> {
                        sess.addPasswordIdentity(credential)
                    }
                    is AuthMethod.GeneratedKey, is AuthMethod.ImportedKey -> {
                        val keyTag = settings.authMethod.keyTag
                            ?: error("Key tag missing for key-based auth")
                        val keyData = keychainService.retrievePrivateKeyData(keyTag)
                            ?: error("SSH key not found in keychain — try regenerating")

                        val keyPair = Ed25519KeyPairBuilder.build(keyData)
                        sess.addPublicKeyIdentity(keyPair)
                    }
                }

                sess.auth().verify(config.sshConnectTimeout.toLong(), TimeUnit.SECONDS)
                session = sess

                _connectionState.value = ConnectionState.Connected
                Timber.i("SSH connected, opening PTY")

                // Open PTY shell
                val ptyConfig = PtyChannelConfiguration().apply {
                    ptyType = config.terminalTermType
                    ptyColumns = config.terminalDefaultColumns
                    ptyLines = config.terminalDefaultRows
                }

                val channel = sess.createShellChannel(ptyConfig, null)

                // Set up stdin piping
                val pipedOutput = PipedOutputStream()
                val pipedInput = PipedInputStream(pipedOutput)
                channel.setIn(pipedInput)

                channel.open().verify(config.sshConnectTimeout.toLong(), TimeUnit.SECONDS)
                shellChannel = channel
                stdinStream = pipedOutput

                // Send initial command
                val command = if (sessionName != null) {
                    tmuxService.attachOrCreateCommand(
                        sessionName = sessionName,
                        directory = settings.projectPath,
                        initialCommand = config.sshCommand
                    ) + "\n"
                } else {
                    "cd ${shellEscape(settings.projectPath)} && ${config.sshCommand}\n"
                }

                Timber.i("Sending command: %s", command.take(120))
                pipedOutput.write(command.toByteArray())
                pipedOutput.flush()

                // Read stdout and stderr concurrently
                val stdoutStream = channel.invertedOut
                val stderrStream = channel.invertedErr

                // Stderr reader on a separate coroutine
                val stderrJob = launch {
                    val errBuf = ByteArray(4096)
                    try {
                        while (isActive && channel.isOpen) {
                            val n = stderrStream.read(errBuf)
                            if (n == -1) break
                            val chunk = errBuf.copyOf(n)
                            outputInterceptor?.onOutput(chunk)
                            lineBuffer.append(chunk)
                            _outputFlow.emit(chunk)
                        }
                    } catch (_: Exception) {
                        // Stderr stream closed
                    }
                }

                // Stdout reader on the main connection coroutine
                val buffer = ByteArray(8192)
                while (isActive && channel.isOpen) {
                    val bytesRead = stdoutStream.read(buffer)
                    if (bytesRead == -1) break

                    val chunk = buffer.copyOf(bytesRead)
                    outputInterceptor?.onOutput(chunk)
                    lineBuffer.append(chunk)
                    _outputFlow.emit(chunk)
                }

                stderrJob.cancel()

                Timber.i("PTY session ended")
                _connectionState.value = ConnectionState.Disconnected
                cleanup()

            } catch (e: CancellationException) {
                Timber.i("Connection cancelled")
                _connectionState.value = ConnectionState.Disconnected
                cleanup()
            } catch (e: Exception) {
                val message = humanReadableError(e)
                Timber.e(e, "Connection failed: %s", message)
                _connectionState.value = ConnectionState.Failed(message)
                cleanup()
            }
        }
    }

    fun reconnect() {
        val settings = lastSettings ?: run {
            Timber.w("Cannot reconnect — no previous connection parameters")
            return
        }
        val credential = lastCredential ?: ""
        val maxAttempts = config.reconnectMaxAttempts
        val delayMs = (config.reconnectDelaySeconds * 1000).toLong()

        connectionJob?.cancel()
        connectionJob = null
        cleanup()

        connectionJob = scope.launch {
            for (attempt in 1..maxAttempts) {
                if (!isActive) return@launch

                _connectionState.value = ConnectionState.Reconnecting(attempt, maxAttempts)
                Timber.i("Reconnect attempt %d/%d", attempt, maxAttempts)

                delay(delayMs)
                if (!isActive) return@launch

                _connectionState.value = ConnectionState.Disconnected
                connect(settings, credential, lastProfileId)

                delay(delayMs)

                if (_connectionState.value is ConnectionState.Connected) {
                    Timber.i("Reconnected successfully on attempt %d", attempt)
                    return@launch
                }
            }

            _connectionState.value = ConnectionState.Failed(
                "Reconnection failed after $maxAttempts attempts"
            )
        }
    }

    fun sendToRemote(data: ByteArray) {
        val stream = stdinStream ?: return
        scope.launch {
            try {
                stream.write(data)
                stream.flush()
            } catch (e: Exception) {
                Timber.e(e, "Failed to write to remote")
            }
        }
    }

    fun resizeTerminal(cols: Int, rows: Int) {
        val channel = shellChannel ?: return
        Timber.d("Resizing terminal to %dx%d", cols, rows)
        scope.launch {
            try {
                channel.sendWindowChange(cols, rows, 0, 0)
            } catch (e: Exception) {
                Timber.e(e, "Failed to resize terminal")
            }
        }
    }

    suspend fun uploadData(data: ByteArray, remotePath: String) {
        val sess = session ?: error("Not connected")
        withContext(Dispatchers.IO) {
            val sftpClient = SftpClientFactory.instance().createSftpClient(sess)
            try {
                sftpClient.write(
                    remotePath,
                    EnumSet.of(
                        org.apache.sshd.sftp.client.SftpClient.OpenMode.Write,
                        org.apache.sshd.sftp.client.SftpClient.OpenMode.Create,
                        org.apache.sshd.sftp.client.SftpClient.OpenMode.Truncate
                    )
                ).use { outputStream ->
                    outputStream.write(data)
                }
                Timber.i("Uploaded %d bytes via SFTP to %s", data.size, remotePath)
            } finally {
                sftpClient.close()
            }
        }
    }

    fun disconnect() {
        Timber.i("Disconnecting SSH session")
        connectionJob?.cancel()
        connectionJob = null
        cleanup()
        _connectionState.value = ConnectionState.Disconnected
    }

    private fun cleanup() {
        try { stdinStream?.close() } catch (_: Exception) {}
        try { shellChannel?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { sshClient?.stop() } catch (_: Exception) {}
        stdinStream = null
        shellChannel = null
        session = null
        sshClient = null
    }

    private fun shellEscape(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun humanReadableError(error: Throwable): String {
        val message = error.message ?: error.toString()

        if (message.contains("Auth fail", ignoreCase = true) ||
            message.contains("authentication", ignoreCase = true)
        ) {
            return "Authentication failed — check username and password"
        }

        if (message.contains("timed out", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true)
        ) {
            return "Connection timed out — check host and port"
        }

        if (message.contains("Connection refused", ignoreCase = true)) {
            return "Connection refused — check host and port"
        }

        if (message.contains("UnknownHost", ignoreCase = true) ||
            message.contains("No route", ignoreCase = true)
        ) {
            return "Host not reachable — check hostname"
        }

        return message
    }
}
