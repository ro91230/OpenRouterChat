package com.personal.chat.data.repository

import com.personal.chat.data.database.AppDatabase
import com.personal.chat.data.database.ModelEntity
import com.personal.chat.data.network.OpenRouterService
import kotlinx.coroutines.flow.Flow

class ModelRepository(
    private val db: AppDatabase,
    private val api: OpenRouterService
) {
    private val dao = db.modelDao()

    fun getCachedModels(): Flow<List<ModelEntity>> = dao.getModelsFlow()

    suspend fun synchronizeModels() {
        try {
            val freshList = api.fetchModels()
            val existingFavs = dao.getModels().associate { it.id to Pair(it.isFavorite, it.customOrder) }

            val entities = freshList.map { item ->
                val favState = existingFavs[item.id] ?: Pair(false, 0)
                ModelEntity(
                    id = item.id,
                    name = item.name,
                    contextLength = item.contextLength,
                    promptPricing = item.promptPricing,
                    completionPricing = item.completionPricing,
                    isFavorite = favState.first,
                    customOrder = favState.second
                )
            }
            if (entities.isNotEmpty()) {
                dao.clearCache()
                dao.insertModels(entities)
            }
        } catch (e: Exception) {
            throw Exception("Failed execution sequence: ${e.message}")
        }
    }

    suspend fun setFavorite(modelId: String, isFav: Boolean) {
        dao.updateFavorite(modelId, isFav)
    }

    suspend fun updateCustomOrdering(modelId: String, order: Int) {
        dao.updateOrder(modelId, order)
    }
}
