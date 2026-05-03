package com.stocknews.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            val result = repository.loadHeadlines()
            _state.value = if (result.articles.isEmpty()) {
                UiState.Empty(result.errors)
            } else {
                UiState.Content(result.articles, result.errors)
            }
        }
    }

    sealed interface UiState {
        data object Loading : UiState
        data class Content(val articles: List<NewsArticle>, val errors: List<SourceError>) : UiState
        data class Empty(val errors: List<SourceError>) : UiState
    }
}
