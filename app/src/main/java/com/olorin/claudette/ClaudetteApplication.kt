package com.olorin.claudette

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.bouncycastle.jce.provider.BouncyCastleProvider
import timber.log.Timber
import java.security.Security

@HiltAndroidApp
class ClaudetteApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Register Bouncy Castle as a security provider for Ed25519 support
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)

        // Initialize Timber logging
        Timber.plant(Timber.DebugTree())
        Timber.i("Claudette application initialized")
    }
}
