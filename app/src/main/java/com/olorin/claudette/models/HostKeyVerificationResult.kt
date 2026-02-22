package com.olorin.claudette.models

sealed class HostKeyVerificationResult {
    data object Trusted : HostKeyVerificationResult()
    data object NewHost : HostKeyVerificationResult()
    data class KeyChanged(
        val previousFingerprint: String,
        val newFingerprint: String
    ) : HostKeyVerificationResult()
}
