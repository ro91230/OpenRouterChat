package com.personal.chat.di

import android.content.Context
import com.personal.chat.data.database.AppDatabase
import com.personal.chat.data.network.OpenRouterService
import com.personal.chat.data.repository.AssetOverrideManager
import com.personal.chat.data.repository.ChatRepository
import com.personal.chat.data.repository.ModelRepository
import com.personal.chat.domain.usecase.ProcessChatUseCase

class AppContainer(private val context: Context) {

    private val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    private val openRouterService: OpenRouterService by lazy {
        OpenRouterService()
    }

    val modelRepository: ModelRepository by lazy {
        ModelRepository(database, openRouterService)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database, openRouterService)
    }

    val assetOverrideManager: AssetOverrideManager by lazy {
        AssetOverrideManager(context)
    }

    val processChatUseCase: ProcessChatUseCase by lazy {
        ProcessChatUseCase(chatRepository)
    }

    val apiKeyDao by lazy { database.apiKeyDao() }
}
