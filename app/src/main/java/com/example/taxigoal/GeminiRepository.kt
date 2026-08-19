package com.example.taxigoal

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import java.net.HttpURLConnection

class GeminiRepository(private val context: Context) {

    suspend fun sendPromptWithRetry(prompt: String, retries: Int = 3): Result<String> {
        var lastException: Exception? = null
        var delayMs = 1000L

        repeat(retries) { attempt ->
            try {
                val apiKey = GeminiManager.getEffectiveApiKey(context)
                val modelName = GeminiManager.WORKING_MODEL_NAME
                
                AppLogger.info("Gemini", "REQUEST_START", "Model: $modelName | Attempt: ${attempt + 1}")
                AppLogger.info("Gemini", "API_KEY_CONFIGURED", "Status: ${apiKey.isNotBlank()}")
                
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                
                val response = model.generateContent(prompt)
                val text = response.text
                
                if (!text.isNullOrBlank()) {
                    AppLogger.info("Gemini", "REQUEST_SUCCESS", "Response length: ${text.length}")
                    return Result.success(text)
                } else {
                    throw Exception("AI returned empty text")
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                
                // Handle 503 High Demand or 429 Rate Limit
                if (errorMsg.contains("503") || errorMsg.contains("demand", true) || errorMsg.contains("429")) {
                    AppLogger.warn("Gemini", "REQUEST_RETRY", "Server busy, retrying in ${delayMs}ms...")
                    delay(delayMs)
                    delayMs *= 2 // Exponential backoff
                } else {
                    // Critical errors (401, 403, etc.) - don't retry
                    val errorMessage = when {
                        errorMsg.contains("401") -> "Ошибка авторизации: проверьте API ключ."
                        errorMsg.contains("403") -> "Доступ запрещен: проверьте лимиты."
                        else -> "Ошибка AI: $errorMsg"
                    }
                    AppLogger.error("Gemini", "REQUEST_FAILED", errorMessage)
                    return Result.failure(Exception(errorMessage))
                }
            }
        }
        
        AppLogger.error("Gemini", "RETRY_EXHAUSTED", "Failed after $retries attempts")
        return Result.failure(lastException ?: Exception("Unknown error"))
    }

    suspend fun sendMessageWithRetry(chat: com.google.ai.client.generativeai.Chat, text: String, retries: Int = 3): Result<String> {
        var delayMs = 1000L
        var lastException: Exception? = null

        repeat(retries) { attempt ->
            try {
                val response = chat.sendMessage(text)
                return Result.success(response.text ?: "AI не вернул текст.")
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("503") == true || e.message?.contains("demand", true) == true) {
                    delay(delayMs)
                    delayMs *= 2
                } else {
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(lastException ?: Exception("Ошибка чата"))
    }
}
