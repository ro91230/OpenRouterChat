package com.personal.chat.data.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromJsonString(value: String): List<String> {
        return try {
            Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(String.serializer()), value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    @JvmStatic
    fun toJsonString(list: List<String>): String {
        return Json.encodeToString(kotlinx.serialization.builtins.ListSerializer(String.serializer()), list)
    }
}
