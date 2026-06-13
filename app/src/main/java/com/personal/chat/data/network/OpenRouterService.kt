package com.personal.chat.data.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class OpenRouterService {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    suspend fun fetchModels(): List<JsonModelResponse> {
        val response: HttpResponse = client.get("https://openrouter.ai/api/v1/models")
        if (response.status == HttpStatusCode.OK) {
            val bodyString = response.bodyAsText()
            val parsed = Json.parseToJsonElement(bodyString).jsonObject
            val dataArray = parsed["data"]?.jsonArray ?: return emptyList()
            
            return dataArray.map { element ->
                val obj = element.jsonObject
                val pricingObj = obj["pricing"]?.jsonObject
                JsonModelResponse(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "Unknown Model",
                    contextLength = obj["context_length"]?.jsonPrimitive?.intOrNull ?: 4096,
                    promptPricing = pricingObj?.get("prompt")?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    completionPricing = pricingObj?.get("completion")?.jsonPrimitive?.doubleOrNull ?: 0.0
                )
            }
        } else {
            throw Exception("Failed to fetch model catalog from OpenRouter: Error ${response.status}")
        }
    }

    suspend fun validateApiKey(key: String): Boolean {
        val response = client.get("https://openrouter.ai/api/v1/auth/key") {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        return response.status == HttpStatusCode.OK
    }

    fun streamChat(
        apiKey: String,
        payload: ChatRequestPayload
    ): Flow<String> = flow {
        val payloadJsonString = Json.encodeToString(ChatRequestPayload.serializer(), payload)
        
        client.preparePost("https://openrouter.ai/api/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header("HTTP-Referer", "https://github.com/personal-chat")
            header("X-Title", "Personal Ultra Chat")
            setBody(payloadJsonString)
        }.execute { httpResponse ->
            if (httpResponse.status != HttpStatusCode.OK) {
                val errText = httpResponse.bodyAsText()
                throw Exception("API Error: HTTP ${httpResponse.status.value} - $errText")
            }
            val channel: ByteReadChannel = httpResponse.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (line.startsWith("data:")) {
                    val content = line.removePrefix("data:").trim()
                    if (content == "[DONE]") {
                        break
                    }
                    try {
                        val parsed = Json.parseToJsonElement(content).jsonObject
                        val choices = parsed["choices"]?.jsonArray
                        if (choices != null && choices.isNotEmpty()) {
                            val delta = choices[0].jsonObject["delta"]?.jsonObject
                            val text = delta?.get("content")?.jsonPrimitive?.content
                            if (!text.isNullOrEmpty()) {
                                emit(text)
                            }
                        }
                    } catch (e: Exception) {
                        // Suppressed exception to process subsequent stream payloads
                    }
                }
            }
        }
    }
}

@Serializable
data class JsonModelResponse(
    val id: String,
    val name: String,
    val contextLength: Int,
    val promptPricing: Double,
    val completionPricing: Double
)

@Serializable
data class ChatRequestPayload(
    val model: String,
    val messages: List<RequestMessage>,
    val temperature: Float,
    val max_tokens: Int,
    val stream: Boolean = true
)

@Serializable
data class RequestMessage(
    val role: String,
    val content: String
)
