package com.olorin.claudette.services.interfaces

import com.olorin.claudette.models.PromptSnippet

interface SnippetStoreInterface {
    fun loadSnippets(): List<PromptSnippet>
    fun saveSnippets(snippets: List<PromptSnippet>)
}
