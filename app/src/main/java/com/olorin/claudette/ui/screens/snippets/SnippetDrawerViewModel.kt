package com.olorin.claudette.ui.screens.snippets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olorin.claudette.config.LoggerFactory
import com.olorin.claudette.models.PromptSnippet
import com.olorin.claudette.models.SnippetCategory
import com.olorin.claudette.services.interfaces.SnippetStoreInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SnippetDrawerViewModel @Inject constructor(
    private val snippetStore: SnippetStoreInterface
) : ViewModel() {

    private val _snippets = MutableStateFlow<List<PromptSnippet>>(emptyList())
    val snippets: StateFlow<List<PromptSnippet>> = _snippets.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: MutableStateFlow<String> = _searchQuery

    val filteredSnippets: StateFlow<List<PromptSnippet>> = combine(
        _snippets,
        _searchQuery
    ) { allSnippets, query ->
        if (query.isBlank()) {
            allSnippets
        } else {
            val lowerQuery = query.lowercase()
            allSnippets.filter { snippet ->
                snippet.label.lowercase().contains(lowerQuery) ||
                    snippet.command.lowercase().contains(lowerQuery) ||
                    snippet.category.displayName.lowercase().contains(lowerQuery)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
        initialValue = emptyList()
    )

    init {
        loadSnippets()
    }

    fun addSnippet(label: String, command: String, category: SnippetCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            val newSnippet = PromptSnippet(
                label = label,
                command = command,
                category = category,
                isBuiltIn = false
            )
            val updated = _snippets.value + newSnippet
            snippetStore.saveSnippets(updated.filter { !it.isBuiltIn })
            _snippets.value = updated
            LoggerFactory.i(LOG_TAG, "Added custom snippet: $label")
        }
    }

    fun deleteSnippet(snippetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val target = _snippets.value.find { it.id == snippetId } ?: return@launch
            if (target.isBuiltIn) {
                LoggerFactory.w(LOG_TAG, "Cannot delete built-in snippet: ${target.label}")
                return@launch
            }
            val updated = _snippets.value.filter { it.id != snippetId }
            snippetStore.saveSnippets(updated.filter { !it.isBuiltIn })
            _snippets.value = updated
            LoggerFactory.i(LOG_TAG, "Deleted snippet: ${target.label}")
        }
    }

    private fun loadSnippets() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = snippetStore.loadSnippets()
            _snippets.value = loaded
            LoggerFactory.d(LOG_TAG, "Loaded ${loaded.size} snippets")
        }
    }

    companion object {
        private const val LOG_TAG = "SnippetDrawerVM"
        private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
