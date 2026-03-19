package com.olorin.claudette.models

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
enum class UserTier(val displayName: String) {
    FREE("Free"),
    ECHO("Echo");

    companion object {
        private const val PREFS_NAME = "claudette_prefs"
        private const val KEY_USER_TIER = "user_tier"

        fun current(context: Context): UserTier {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_USER_TIER, FREE.name) ?: FREE.name
            return try {
                valueOf(raw)
            } catch (_: IllegalArgumentException) {
                FREE
            }
        }

        fun save(context: Context, tier: UserTier) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_TIER, tier.name)
                .apply()
        }
    }
}
