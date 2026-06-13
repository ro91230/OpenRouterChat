package com.personal.chat.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey val alias: String,
    val encryptedKey: String,
    val iv: String,
    val status: String,
    val lastValidated: Long,
    val isActive: Boolean
)

@Entity(tableName = "models_cache")
data class ModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val contextLength: Int,
    val promptPricing: Double,
    val completionPricing: Double,
    val isFavorite: Boolean = false,
    val customOrder: Int = 0,
    val dateCached: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val systemPrompt: String,
    val temperature: Float,
    val maxTokens: Int,
    val contextCap: Int,
    val modelId: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val editMode: String,
    val editHistoryJson: String
)

@Entity(tableName = "api_logs")
data class ApiLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val modelId: String,
    val activeKeyAlias: String,
    val requestPayload: String,
    val responseBody: String?,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalCost: Double,
    val isSuccess: Boolean,
    val errorMessage: String?
)
