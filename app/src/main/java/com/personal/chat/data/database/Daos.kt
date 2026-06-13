package com.personal.chat.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys")
    fun getAllKeysFlow(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys")
    suspend fun getAllKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveKey(): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE isActive = 1 LIMIT 1")
    fun getActiveKeyFlow(): Flow<ApiKeyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity)

    @Query("UPDATE api_keys SET isActive = 0")
    suspend fun deactivateAllKeys()

    @Transaction
    suspend fun setActiveKey(alias: String) {
        deactivateAllKeys()
        val key = getKeyByAlias(alias)
        if (key != null) {
            insertKey(key.copy(isActive = true))
        }
    }

    @Query("SELECT * FROM api_keys WHERE alias = :alias LIMIT 1")
    suspend fun getKeyByAlias(alias: String): ApiKeyEntity?

    @Query("DELETE FROM api_keys WHERE alias = :alias")
    suspend fun deleteKey(alias: String)
}

@Dao
interface ModelDao {
    @Query("SELECT * FROM models_cache ORDER BY isFavorite DESC, customOrder ASC, name ASC")
    fun getModelsFlow(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models_cache")
    suspend fun getModels(): List<ModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("UPDATE models_cache SET isFavorite = :isFav WHERE id = :modelId")
    suspend fun updateFavorite(modelId: String, isFav: Boolean)

    @Query("UPDATE models_cache SET customOrder = :order WHERE id = :modelId")
    suspend fun updateOrder(modelId: String, order: Int)

    @Query("DELETE FROM models_cache")
    suspend fun clearCache()
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY createdAt DESC")
    fun getConversationsFlow(): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(convo: ConversationEntity)

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convoId ORDER BY timestamp ASC")
    fun getMessagesFlow(convoId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :convoId ORDER BY timestamp ASC")
    suspend fun getMessages(convoId: String): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :msgId")
    suspend fun deleteMessage(msgId: String)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :msgId")
    suspend fun updatePinned(msgId: String, isPinned: Boolean)
}

@Dao
interface ApiLogDao {
    @Query("SELECT * FROM api_logs ORDER BY timestamp DESC")
    fun getLogsFlow(): Flow<List<ApiLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ApiLogEntity)
}
