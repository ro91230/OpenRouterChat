package com.personal.chat.domain.model

data class PromptPreset(
    val name: String,
    val systemPrompt: String,
    val description: String,
    val temperature: Float,
    val maxTokens: Int,
    val contextCap: Int
)
