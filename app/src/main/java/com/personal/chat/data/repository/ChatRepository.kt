package com.personal.chat.data.repository

import com.personal.chat.data.database.*
import com.personal.chat.data.network.ChatRequestPayload
import com.personal.chat.data.network.OpenRouterService
import com.personal.chat.data.network.RequestMessage
import com.personal.chat.data.security.KeyStoreHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID

class ChatRepository(
    private val db: AppDatabase,
    private val openRouterService: OpenRouterService
) {
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val apiKeyDao = db.apiKeyDao()
    private val logDao = db.apiLogDao()

    fun getConversationsFlow() = conversationDao.getConversationsFlow()

    fun getMessagesFlow(convoId: String) = messageDao.getMessagesFlow(convoId)

    suspend fun createConversation(
        title: String,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        contextCap: Int,
        modelId: String
    ): String {
        val id = UUID.randomUUID().toString()
        val convo = ConversationEntity(
            id = id,
            title = title,
            createdAt = System.currentTimeMillis(),
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxTokens = maxTokens,
            contextCap = contextCap,
            modelId = modelId
        )
        conversationDao.insertConversation(convo)
        return id
    }

    suspend fun saveMessage(
        conversationId: String,
        role: String,
        content: String,
        editMode: String = "APPLY_TO_CONTEXT",
        history: List<String> = emptyList()
    ): MessageEntity {
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = System.currentTimeMillis(),
            isPinned = false,
            editMode = editMode,
            editHistoryJson = Json.encodeToString(ListSerializer(String.serializer()), history)
        )
        messageDao.insertMessage(msg)
        return msg
    }

    suspend fun updateMessage(msg: MessageEntity) {
        messageDao.insertMessage(msg)
    }

    suspend fun deleteMessage(msgId: String) {
        messageDao.deleteMessage(msgId)
    }

    suspend fun updateMessagePinnedStatus(msgId: String, isPinned: Boolean) {
        messageDao.updatePinned(msgId, isPinned)
    }

    fun executeStreamingChat(
        conversationId: String,
        userMessageText: String
    ): Flow<String> = flow {
        val activeKeyEntity = apiKeyDao.getActiveKey() ?: throw Exception("No active API keys found.")
        val decryptedApiKey = KeyStoreHelper.decrypt(activeKeyEntity.encryptedKey, activeKeyEntity.iv)

        val convo = conversationDao.getConversationById(conversationId) ?: throw Exception("Session untraceable.")
        val savedHistory = messageDao.getMessages(conversationId)

        val apiMessages = mutableListOf<RequestMessage>()
        
        if (convo.systemPrompt.isNotEmpty()) {
            apiMessages.add(RequestMessage(role = "system", content = convo.systemPrompt))
        }

        val cappedContext = savedHistory.takeLast(convo.contextCap)
        for (msg in cappedContext) {
            val contentToSend = if (msg.role == "assistant" && msg.editMode == "DISPLAY_ONLY") {
                val histList = Converters.fromJsonString(msg.editHistoryJson)
                if (histList.isNotEmpty()) histList.first() else msg.content
            } else {
                msg.content
            }
            apiMessages.add(RequestMessage(role = msg.role, content = contentToSend))
        }

        apiMessages.add(RequestMessage(role = "user", content = userMessageText))

        val payload = ChatRequestPayload(
            model = convo.modelId,
            messages = apiMessages,
            temperature = convo.temperature,
            max_tokens = convo.maxTokens,
            stream = true
        )

        var accumulatedResponse = ""
        var successStatus = true
        var executionError: String? = null

        try {
            openRouterService.streamChat(decryptedApiKey, payload).collect { token ->
                accumulatedResponse += token
                emit(token)
            }
        } catch (e: Exception) {
            successStatus = false
            executionError = e.localizedMessage ?: "Streaming interrupted."
            throw e
        } finally {
            val calculatedCost = estimatePayloadCost(
                modelId = convo.modelId,
                inTokens = estimateTokens(apiMessages.joinToString { it.content }),
                outTokens = estimateTokens(accumulatedResponse)
            )

            logDao.insertLog(
                ApiLogEntity(
                    timestamp = System.currentTimeMillis(),
                    modelId = convo.modelId,
                    activeKeyAlias = activeKeyEntity.alias,
                    requestPayload = Json.encodeToString(ChatRequestPayload.serializer(), payload),
                    responseBody = if (accumulatedResponse.isNotEmpty()) accumulatedResponse else null,
                    inputTokens = estimateTokens(apiMessages.joinToString { it.content }),
                    outputTokens = estimateTokens(accumulatedResponse),
                    totalCost = calculatedCost,
                    isSuccess = successStatus,
                    errorMessage = executionError
                )
            )
        }
    }

    private fun estimateTokens(text: String): Int {
        return (text.length / 4.1).toInt().coerceAtLeast(1)
    }

    private suspend fun estimatePayloadCost(modelId: String, inTokens: Int, outTokens: Int): Double {
        val model = db.modelDao().getModels().find { it.id == modelId } ?: return 0.0
        val inCost = (inTokens.toDouble() / 1_000_000.0) * model.promptPricing
        val outCost = (outTokens.toDouble() / 1_000_000.0) * model.completionPricing
        return inCost + outCost
    }
}
