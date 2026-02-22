package com.olorin.claudette

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.ui.theme.ClaudetteTheme
import com.olorin.claudette.ui.screens.ClaudetteNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalAppConfiguration = staticCompositionLocalOf<AppConfiguration> {
    error("AppConfiguration not provided")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appConfiguration: AppConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalAppConfiguration provides appConfiguration) {
                ClaudetteTheme {
                    ClaudetteNavHost()
                }
            }
        }
    }
}
