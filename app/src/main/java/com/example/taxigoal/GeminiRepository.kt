package com.example.taxigoal

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay

class GeminiRepository(private val context: Context) {

    suspend fun sendPromptWithRetry(prompt: String, retries: Int = 1): Result<String> {
        return generateResponse(prompt, context)
    }

    suspend fun generateResponse(prompt: String, context: Context): Result<String> {
        return try {
            val apiKey = GeminiManager.getEffectiveApiKey(context)
            val modelName = GeminiManager.WORKING_MODEL_NAME
            
            AppLogger.info("Gemini", "REQUEST_START", "Sending request to model: $modelName")
            
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )
            
            val response = model.generateContent(prompt)
            val text = response.text
            
            if (!text.isNullOrBlank()) {
                AppLogger.info("Gemini", "REQUEST_SUCCESS", "Response received from $modelName")
                Result.success(text)
            } else {
                Result.failure(Exception("Пустой ответ от AI"))
            }
        } catch (e: Exception) {
            val details = AppLogger.getErrorDetails(e)
            val errorMessage = when {
                e.message?.contains("401") == true -> "Ошибка авторизации: неверный API ключ. Проверьте настройки."
                e.message?.contains("403") == true -> "Доступ запрещен: проверьте лимиты или ограничения региона."
                e.message?.contains("429") == true -> "Слишком много запросов. Попробуйте позже."
                else -> e.message ?: "Неизвестная ошибка ИИ"
            }
            AppLogger.error("Gemini", "REQUEST_FAILED", errorMessage, details = details)
            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun sendMessageWithRetry(chat: com.google.ai.client.generativeai.Chat, text: String, retries: Int = 2): Result<String> {
        var delayMs = 2000L
        repeat(retries) { attempt ->
            try {
                val response = chat.sendMessage(text)
                return Result.success(response.text ?: "AI не вернул текст.")
            } catch (e: Exception) {
                if (attempt < retries - 1) {
                    delay(delayMs)
                    delayMs *= 2
                } else {
                    return Result.failure(e)
                }
            }
        }
        return Result.failure(Exception("Ошибка чата после повторов"))
    }
}
