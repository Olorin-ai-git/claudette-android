package com.olorin.claudette.services.impl

import android.content.Context
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.services.interfaces.ProfileStoreInterface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class ProfileStore(context: Context) : ProfileStoreInterface {

    private val file: File
    private val mutex = Mutex()
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    init {
        val appDir = File(context.filesDir, "com.olorin.claudette")
        if (!appDir.exists()) appDir.mkdirs()
        file = File(appDir, "profiles.json")
        Timber.i("Profile store path: %s", file.absolutePath)
    }

    override suspend fun loadProfiles(): List<ServerProfile> = mutex.withLock {
        if (!file.exists()) return@withLock emptyList()
        return@withLock try {
            val data = file.readText()
            val profiles = json.decodeFromString<List<ServerProfile>>(data)
            Timber.i("Loaded %d profiles", profiles.size)
            profiles
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profiles")
            emptyList()
        }
    }

    override suspend fun saveProfile(profile: ServerProfile) = mutex.withLock {
        val profiles = loadProfilesUnsafe().toMutableList()
        profiles.removeAll { it.id == profile.id }
        profiles.add(profile)
        writeProfiles(profiles)
        Timber.i("Saved profile: %s", profile.name)
    }

    override suspend fun deleteProfile(profileId: String) = mutex.withLock {
        val profiles = loadProfilesUnsafe().toMutableList()
        profiles.removeAll { it.id == profileId }
        writeProfiles(profiles)
        Timber.i("Deleted profile: %s", profileId)
    }

    override suspend fun updateProfile(profile: ServerProfile) = mutex.withLock {
        val profiles = loadProfilesUnsafe().toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
            writeProfiles(profiles)
            Timber.i("Updated profile: %s", profile.name)
        }
    }

    private fun loadProfilesUnsafe(): List<ServerProfile> {
        if (!file.exists()) return emptyList()
        return try {
            json.decodeFromString(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeProfiles(profiles: List<ServerProfile>) {
        val data = json.encodeToString(profiles)
        file.writeText(data)
    }
}
