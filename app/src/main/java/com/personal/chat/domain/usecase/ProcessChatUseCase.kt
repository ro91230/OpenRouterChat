package com.personal.chat.domain.usecase

import com.personal.chat.data.database.MessageEntity
import com.personal.chat.data.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class ProcessChatUseCase(private val repository: ChatRepository) {

    fun execute(conversationId: String, promptText: String): Flow<String> {
        if (promptText.trim().isEmpty()) {
            throw IllegalArgumentException("Query text cannot be blank.")
        }
        return repository.executeStreamingChat(conversationId, promptText)
    }

    suspend fun appendUserQuery(conversationId: String, text: String): MessageEntity {
        return repository.saveMessage(conversationId, "user", text)
    }

    suspend fun appendAssistantPlaceholder(conversationId: String, text: String): MessageEntity {
        return repository.saveMessage(conversationId, "assistant", text)
    }

    suspend fun applyEditToMessage(
        message: MessageEntity, 
        updatedText: String, 
        applyToContext: Boolean
    ): MessageEntity {
        val historyList = try {
            com.personal.chat.data.database.Converters.fromJsonString(message.editHistoryJson).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }

        if (historyList.isEmpty()) {
            historyList.add(message.content)
        }
        
        if (historyList.size >= 20) {
            historyList.removeAt(0)
        }
        historyList.add(updatedText)

        val updatedMsg = message.copy(
            content = updatedText,
            editMode = if (applyToContext) "APPLY_TO_CONTEXT" else "DISPLAY_ONLY",
            editHistoryJson = com.personal.chat.data.database.Converters.toJsonString(historyList)
        )
        repository.updateMessage(updatedMsg)
        return updatedMsg
    }
}
