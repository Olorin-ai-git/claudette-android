package com.olorin.claudette.models

data class BonjourHost(
    val serviceName: String,
    val hostname: String,
    val port: Int,
    val txtRecord: Map<String, String> = emptyMap()
) {
    val id: String get() = "$serviceName$hostname$port"

    val displayName: String
        get() = serviceName.ifEmpty { hostname }
}
