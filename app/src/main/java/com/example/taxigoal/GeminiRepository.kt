package com.example.taxigoal

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import org.json.JSONObject

class GeminiRepository(private val context: Context) {

    private val model = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = com.example.taxigoal.BuildConfig.GEMINI_API_KEY
    )

    suspend fun sendPromptWithRetry(prompt: String, safeContext: String = "", retries: Int = 3): Result<String> {
        var lastException: Exception? = null
        
        repeat(retries) { attempt ->
            try {
                AppLogger.info("Gemini", "REQUEST_START", "Direct SDK | Attempt: ${attempt + 1}")
                
                val fullPrompt = if (safeContext.isNotBlank()) "$safeContext\n\n$prompt" else prompt
                val response = model.generateContent(fullPrompt)
                val text = response.text
                
                if (!text.isNullOrBlank()) {
                    AppLogger.info("Gemini", "REQUEST_SUCCESS", "Response received")
                    return Result.success(text)
                } else {
                    throw Exception("AI returned empty text")
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                
                if (errorMsg.contains("503") || errorMsg.contains("demand", true) || errorMsg.contains("429")) {
                    AppLogger.warn("Gemini", "REQUEST_RETRY", "Server busy, retrying...")
                    delay(1000L * (attempt + 1))
                } else {
                    AppLogger.error("Gemini", "REQUEST_FAILED", "Fatal error: $errorMsg")
                    return Result.failure(e)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("RETRY_EXHAUSTED"))
    }

    suspend fun parseVoiceInput(text: String): Result<Map<String, Double?>> {
        val prompt = """
            Ты — парсер финансовых данных водителя такси.
            Распознай из текста следующие поля: выручка (income), топливо (fuel), еда (food), пробег (mileage), мойка (wash), ремонт (maintenance), штрафы (fines), другое (other).
            ПРАВИЛА: 1. Верни СТРОГО JSON. 2. Если поле не найдено, null. 3. Все суммы - числа.
            ТЕКСТ: "$text"
        """.trimIndent()

        return sendPromptWithRetry(prompt).map { reply ->
            val json = reply.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val jsonObj = JSONObject(json)
            val map = mutableMapOf<String, Double?>()
            val keys = listOf("income", "fuel", "food", "mileage", "wash", "maintenance", "fines", "other")
            for (key in keys) {
                map[key] = if (jsonObj.isNull(key)) null else jsonObj.optDouble(key)
            }
            map
        }
    }

    suspend fun submitReport(report: org.json.JSONObject): Result<String> {
        // Vercel backend is disabled in direct SDK mode. 
        // Returning the report ID locally as a fallback.
        return Result.success(report.optString("reportId", "LOCAL_REPORT"))
    }
}
