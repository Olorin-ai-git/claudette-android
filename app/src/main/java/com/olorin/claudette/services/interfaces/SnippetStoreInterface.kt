package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.PromptSnippet

interface SnippetStoreInterface {
    suspend fun loadSnippets(): List<PromptSnippet>
    suspend fun saveSnippets(snippets: List<PromptSnippet>)
}
