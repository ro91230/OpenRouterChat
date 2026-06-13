package com.personal.chat.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.chat.data.database.ModelEntity
import com.personal.chat.data.repository.ModelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModelViewModel(private val repository: ModelRepository) : ViewModel() {

    val availableModels: StateFlow<List<ModelEntity>> = repository.getCachedModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSyncing = mutableStateOf(false)
    val syncErrorMessage = mutableStateOf<String?>(null)

    fun startManualSync() {
        viewModelScope.launch {
            isSyncing.value = true
            syncErrorMessage.value = null
            try {
                repository.synchronizeModels()
            } catch (e: Exception) {
                syncErrorMessage.value = e.localizedMessage
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun toggleFavorite(modelId: String, currentFavState: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(modelId, !currentFavState)
        }
    }

    fun adjustManualOrdering(modelId: String, newIndex: Int) {
        viewModelScope.launch {
            repository.updateCustomOrdering(modelId, newIndex)
        }
    }
}
