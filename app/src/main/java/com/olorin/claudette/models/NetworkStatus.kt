package com.olorin.claudette.models

sealed class NetworkStatus {
    data object Unknown : NetworkStatus()
    data class Reachable(override val latencyMs: Double) : NetworkStatus()
    data class Degraded(override val latencyMs: Double) : NetworkStatus()
    data object Unreachable : NetworkStatus()

    val isReachable: Boolean
        get() = this is Reachable || this is Degraded

    open val latencyMs: Double?
        get() = null
}
