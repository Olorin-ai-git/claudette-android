package com.olorin.claudette.services.impl

import com.olorin.claudette.models.NetworkStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket

class NetworkProbeService(
    private val host: String,
    private val port: Int,
    private val intervalSeconds: Double,
    private val timeoutSeconds: Double,
    private val degradedThresholdMs: Double,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val _status = MutableStateFlow<NetworkStatus>(NetworkStatus.Unknown)
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var probeJob: Job? = null

    fun start() {
        stop()
        probeJob = scope.launch {
            while (isActive) {
                probe()
                delay((intervalSeconds * 1000).toLong())
            }
        }
        Timber.i("Network probe started for %s:%d", host, port)
    }

    fun stop() {
        probeJob?.cancel()
        probeJob = null
        Timber.i("Network probe stopped")
    }

    private suspend fun probe() = withContext(Dispatchers.IO) {
        val startNanos = System.nanoTime()
        val timeoutMs = (timeoutSeconds * 1000).toInt()

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }

            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0

            _status.value = if (elapsedMs > degradedThresholdMs) {
                NetworkStatus.Degraded(elapsedMs)
            } else {
                NetworkStatus.Reachable(elapsedMs)
            }
        } catch (e: Exception) {
            Timber.d("Network probe failed: %s", e.message)
            _status.value = NetworkStatus.Unreachable
        }
    }
}
