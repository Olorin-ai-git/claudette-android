package com.olorin.claudette.ui.screens.session

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.olorin.claudette.models.ConnectionState
import timber.log.Timber

/**
 * Manages Android lifecycle events for the SSH session:
 * - Acquires PARTIAL_WAKE_LOCK during active connection to prevent CPU sleep
 * - Triggers reconnect on ON_RESUME if disconnected
 */
class SessionLifecycleObserver(
    private val context: Context,
    private val viewModel: SessionViewModel
) : DefaultLifecycleObserver {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        // Reconnect disconnected tabs when app comes back to foreground
        val state = viewModel.activeConnectionState.value
        if (state is ConnectionState.Disconnected || state is ConnectionState.Failed) {
            Timber.i("App resumed — attempting reconnect")
            viewModel.reconnect()
        }

        // Acquire wake lock for active session
        acquireWakeLock()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        // Keep wake lock when backgrounded to maintain connection
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "claudette:ssh-session"
        ).apply {
            acquire(30 * 60 * 1000L) // 30 minute timeout
        }
        Timber.i("Wake lock acquired for SSH session")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Timber.i("Wake lock released")
            }
        }
        wakeLock = null
    }
}
