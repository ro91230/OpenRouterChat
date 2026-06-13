package com.personal.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.chat.data.database.ApiKeyDao
import com.personal.chat.data.database.ApiKeyEntity
import com.personal.chat.data.network.OpenRouterService
import com.personal.chat.data.security.KeyStoreHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyManagerViewModel(
    private val apiDao: ApiKeyDao,
    private val openRouterService: OpenRouterService
) : ViewModel() {

    val keysList: StateFlow<List<ApiKeyEntity>> = apiDao.getAllKeysFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun registerNewKey(alias: String, keyText: String) {
        if (alias.isBlank() || keyText.isBlank()) return
        viewModelScope.launch {
            val encrypted = KeyStoreHelper.encrypt(keyText)
            val newEntity = ApiKeyEntity(
                alias = alias,
                encryptedKey = encrypted.ciphertext,
                iv = encrypted.iv,
                status = "UNCHECKED",
                lastValidated = System.currentTimeMillis(),
                isActive = false
            )
            apiDao.insertKey(newEntity)
        }
    }

    fun triggerKeyValidation(alias: String) {
        viewModelScope.launch {
            val entity = apiDao.getKeyByAlias(alias) ?: return@launch
            try {
                val decrypted = KeyStoreHelper.decrypt(entity.encryptedKey, entity.iv)
                val isValid = openRouterService.validateApiKey(decrypted)
                val updatedStatus = if (isValid) "VALID" else "EXPIRED"
                apiDao.insertKey(entity.copy(status = updatedStatus, lastValidated = System.currentTimeMillis()))
            } catch (e: Exception) {
                apiDao.insertKey(entity.copy(status = "EXPIRED", lastValidated = System.currentTimeMillis()))
            }
        }
    }

    fun makeKeyActive(alias: String) {
        viewModelScope.launch {
            apiDao.setActiveKey(alias)
        }
    }

    fun removeKey(alias: String) {
        viewModelScope.launch {
            apiDao.deleteKey(alias)
        }
    }
}
