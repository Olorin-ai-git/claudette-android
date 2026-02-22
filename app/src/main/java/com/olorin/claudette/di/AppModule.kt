package com.olorin.claudette.di

import android.content.Context
import com.olorin.claudette.config.AppConfiguration
import com.olorin.claudette.services.impl.ClaudeResourceDiscoveryService
import com.olorin.claudette.services.impl.ClaudeSettingsService
import com.olorin.claudette.services.impl.HostKeyStore
import com.olorin.claudette.services.impl.KeychainService
import com.olorin.claudette.services.impl.ProfileStore
import com.olorin.claudette.services.impl.RemoteFileBrowserService
import com.olorin.claudette.services.impl.SnippetStore
import com.olorin.claudette.services.impl.SshKeyService
import com.olorin.claudette.services.impl.TmuxSessionService
import com.olorin.claudette.services.interfaces.ClaudeResourceDiscoveryServiceInterface
import com.olorin.claudette.services.interfaces.ClaudeSettingsServiceInterface
import com.olorin.claudette.services.interfaces.HostKeyStoreInterface
import com.olorin.claudette.services.interfaces.KeychainServiceInterface
import com.olorin.claudette.services.interfaces.ProfileStoreInterface
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import com.olorin.claudette.services.interfaces.SnippetStoreInterface
import com.olorin.claudette.services.interfaces.SshKeyServiceInterface
import com.olorin.claudette.services.interfaces.TmuxSessionServiceInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppConfiguration(@ApplicationContext context: Context): AppConfiguration {
        return AppConfiguration(context)
    }

    @Provides
    @Singleton
    fun provideKeychainService(
        @ApplicationContext context: Context,
        config: AppConfiguration
    ): KeychainServiceInterface {
        return KeychainService(config.keychainServiceName, context)
    }

    @Provides
    @Singleton
    fun provideProfileStore(@ApplicationContext context: Context): ProfileStoreInterface {
        return ProfileStore(context)
    }

    @Provides
    @Singleton
    fun provideHostKeyStore(@ApplicationContext context: Context): HostKeyStoreInterface {
        return HostKeyStore(context)
    }

    @Provides
    @Singleton
    fun provideSshKeyService(keychainService: KeychainServiceInterface): SshKeyServiceInterface {
        return SshKeyService(keychainService)
    }

    @Provides
    @Singleton
    fun provideTmuxSessionService(): TmuxSessionServiceInterface {
        return TmuxSessionService()
    }

    @Provides
    @Singleton
    fun provideSnippetStore(
        @ApplicationContext context: Context,
        config: AppConfiguration
    ): SnippetStoreInterface {
        return SnippetStore(context, config.snippetsStorageFileName)
    }

    @Provides
    fun provideRemoteFileBrowserService(): RemoteFileBrowserServiceInterface {
        return RemoteFileBrowserService()
    }

    @Provides
    fun provideClaudeResourceDiscoveryService(
        fileBrowserService: RemoteFileBrowserServiceInterface
    ): ClaudeResourceDiscoveryServiceInterface {
        return ClaudeResourceDiscoveryService(fileBrowserService)
    }

    @Provides
    fun provideClaudeSettingsService(
        fileBrowserService: RemoteFileBrowserServiceInterface
    ): ClaudeSettingsServiceInterface {
        return ClaudeSettingsService(fileBrowserService)
    }
}
