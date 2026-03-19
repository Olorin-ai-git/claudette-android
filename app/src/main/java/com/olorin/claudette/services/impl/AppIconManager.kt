package com.olorin.claudette.services.impl

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.olorin.claudette.models.UserTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages alternate app icons using activity-alias components declared in the manifest.
 * Mirrors iOS AppIconManager behavior.
 *
 * Requires activity-alias entries in AndroidManifest.xml:
 * - .MainActivityDefault (default icon)
 * - .MainActivityEcho (Echo tier icon)
 */
class AppIconManager(private val context: Context) {

    private val _currentTier = MutableStateFlow(UserTier.current(context))
    val currentTier: StateFlow<UserTier> = _currentTier.asStateFlow()

    fun upgradeTo(tier: UserTier) {
        _currentTier.value = tier
        UserTier.save(context, tier)
        applyIcon(tier)
    }

    private fun applyIcon(tier: UserTier) {
        val pm = context.packageManager
        val packageName = context.packageName

        val defaultAlias = ComponentName(packageName, "$packageName.MainActivityDefault")
        val echoAlias = ComponentName(packageName, "$packageName.MainActivityEcho")

        try {
            when (tier) {
                UserTier.FREE -> {
                    pm.setComponentEnabledSetting(
                        defaultAlias,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    pm.setComponentEnabledSetting(
                        echoAlias,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                UserTier.ECHO -> {
                    pm.setComponentEnabledSetting(
                        echoAlias,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    pm.setComponentEnabledSetting(
                        defaultAlias,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
            Timber.i("App icon switched to %s tier", tier.displayName)
        } catch (e: Exception) {
            Timber.w(e, "Failed to switch app icon — activity-alias may not be configured")
        }
    }
}
