package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.ServerProfile

interface ProfileStoreInterface {
    suspend fun loadProfiles(): List<ServerProfile>
    suspend fun saveProfile(profile: ServerProfile)
    suspend fun deleteProfile(profileId: String)
    suspend fun updateProfile(profile: ServerProfile)
}
