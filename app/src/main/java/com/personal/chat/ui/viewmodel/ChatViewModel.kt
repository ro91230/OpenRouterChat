package com.personal.chat.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.chat.data.database.ApiKeyDao
import com.personal.chat.data.database.ConversationEntity
import com.personal.chat.data.database.MessageEntity
import com.personal.chat.data.repository.ChatRepository
import com.personal.chat.domain.usecase.ProcessChatUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val useCase: ProcessChatUseCase,
    private val apiKeyDao: ApiKeyDao
) : ViewModel() {

    val activeConversation = mutableStateOf<ConversationEntity?>(null)
    val activeMessages = mutableStateListOf<MessageEntity>()
    val inputBuffer = mutableStateOf("")
    val streamingTokenAccumulator = mutableStateOf("")
    val isNetworkStreaming = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    val activeApiKey: StateFlow<Boolean> = apiKeyDao.getActiveKeyFlow()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadConversation(convo: ConversationEntity) {
        activeConversation.value = convo
        errorMessage.value = null
        viewModelScope.launch {
            repository.getMessagesFlow(convo.id).collect { list ->
                activeMessages.clear()
                activeMessages.addAll(list)
            }
        }
    }

    fun submitUserPrompt() {
        val convo = activeConversation.value ?: return
        val rawPrompt = inputBuffer.value.trim()
        if (rawPrompt.isEmpty() || isNetworkStreaming.value) return

        inputBuffer.value = ""
        errorMessage.value = null

        viewModelScope.launch {
            try {
                useCase.appendUserQuery(convo.id, rawPrompt)
                isNetworkStreaming.value = true
                streamingTokenAccumulator.value = ""

                val assistantPlaceholder = useCase.appendAssistantPlaceholder(convo.id, "")

                useCase.execute(convo.id, rawPrompt).catch { exception ->
                    isNetworkStreaming.value = false
                    errorMessage.value = exception.localizedMessage ?: "Connection dropped."
                    repository.deleteMessage(assistantPlaceholder.id)
                }.collect { streamToken ->
                    streamingTokenAccumulator.value += streamToken
                    repository.updateMessage(
                        assistantPlaceholder.copy(content = streamingTokenAccumulator.value)
                    )
                }
            } catch (e: Exception) {
                errorMessage.value = e.localizedMessage ?: "Critical API Transaction Error."
            } finally {
                isNetworkStreaming.value = false
            }
        }
    }

    fun applyMessageModification(msg: MessageEntity, text: String, applyToContext: Boolean) {
        viewModelScope.launch {
            useCase.applyEditToMessage(msg, text, applyToContext)
        }
    }

    fun removeMessage(msgId: String) {
        viewModelScope.launch {
            repository.deleteMessage(msgId)
        }
    }

    fun toggleMessagePin(msgId: String, currentState: Boolean) {
        viewModelScope.launch {
            repository.updateMessagePinnedStatus(msgId, !currentState)
        }
    }
}
