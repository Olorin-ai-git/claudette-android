package com.olorin.claudette.services.impl

import android.content.Context
import com.olorin.claudette.models.PromptSnippet
import com.olorin.claudette.services.interfaces.SnippetStoreInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File

class SnippetStore(
    private val context: Context,
    storageFileName: String
) : SnippetStoreInterface {

    private val file: File
    private val mutex = Mutex()
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    init {
        val appDir = File(context.filesDir, "com.olorin.claudette")
        if (!appDir.exists()) appDir.mkdirs()
        file = File(appDir, storageFileName)
    }

    override suspend fun loadSnippets(): List<PromptSnippet> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val snippets = loadDefaultSnippets().toMutableList()

            if (file.exists()) {
                try {
                    val userSnippets = json.decodeFromString<List<PromptSnippet>>(file.readText())
                    val userCustom = userSnippets.filter { !it.isBuiltIn }
                    snippets.addAll(userCustom)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load user snippets")
                }
            }

            snippets
        }
    }

    override suspend fun saveSnippets(snippets: List<PromptSnippet>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val data = json.encodeToString(snippets)
            file.writeText(data)
            Timber.i("Saved %d snippets", snippets.size)
        }
    }

    private fun loadDefaultSnippets(): List<PromptSnippet> {
        return try {
            val data = context.assets.open("default_snippets.json")
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString(data)
        } catch (e: Exception) {
            Timber.w(e, "default_snippets.json not found in assets")
            emptyList()
        }
    }
}
