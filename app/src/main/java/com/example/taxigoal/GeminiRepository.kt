package com.example.taxigoal

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class GeminiRepository(private val context: Context) {

    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun sendPromptWithRetry(prompt: String, safeContext: String = "", retries: Int = 3): Result<String> {
        var lastException: Exception? = null
        
        repeat(retries) { attempt ->
            val requestId = "GEM-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}-${(100..999).random()}"
            
            try {
                AppLogger.info("Gemini", "DEBUG_REQUEST", "RequestId: $requestId | Backend: FIREBASE_FUNCTION | Attempt: ${attempt + 1}")
                
                val data = hashMapOf(
                    "message" to prompt,
                    "safeContext" to safeContext,
                    "requestId" to requestId
                )

                // Call the Firebase Function
                val result = try {
                    functions.getHttpsCallable("geminiChat").call(data).await()
                } catch (e: FirebaseFunctionsException) {
                    AppLogger.error("Gemini", "DEBUG_FIREBASE_ERROR", 
                        "RequestId: $requestId | Code: ${e.code} | Msg: ${e.message} | Details: ${e.details}")
                    throw e
                }

                val responseData = result.data as? Map<*, *>
                val success = responseData?.get("success") as? Boolean ?: false
                
                if (success) {
                    val reply = responseData?.get("reply") as? String
                    if (!reply.isNullOrBlank()) {
                        AppLogger.info("Gemini", "REQUEST_SUCCESS", "RequestId: $requestId | Reply received")
                        return Result.success(reply)
                    }
                }
                
                // If not success, look for debug info from backend
                val debug = responseData?.get("debug") as? Map<*, *>
                if (debug != null) {
                    AppLogger.error("Gemini", "DEBUG_ERROR", 
                        "RequestId: $requestId | HTTP: ${debug["httpStatus"]} | Status: ${debug["status"]} | Msg: ${debug["message"]}")
                }
                
                val errorType = responseData?.get("errorType") as? String ?: "UNKNOWN_ERROR"
                throw Exception(errorType)

            } catch (e: Exception) {
                lastException = e
                val msg = e.message ?: ""
                
                if (msg.contains("TEMPORARILY_UNAVAILABLE") || msg.contains("RATE_LIMITED") || msg.contains("503") || msg.contains("429")) {
                    AppLogger.warn("Gemini", "REQUEST_RETRY", "RequestId: $requestId | Server busy: $msg")
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                } else {
                    AppLogger.error("Gemini", "REQUEST_FAILED", "RequestId: $requestId | Fatal error: $msg")
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
            
            ПРАВИЛА:
            1. Верни СТРОГО JSON объект.
            2. Если поле не найдено в тексте, поставь null.
            3. Не выдумывай данные.
            4. Все суммы должны быть числами.
            
            ТЕКСТ: "$text"
        """.trimIndent()

        return try {
            val result = sendPromptWithRetry(prompt)
            result.map { reply ->
                val json = reply.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                val jsonObj = org.json.JSONObject(json)
                val map = mutableMapOf<String, Double?>()
                val keys = listOf("income", "fuel", "food", "mileage", "wash", "maintenance", "fines", "other")
                for (key in keys) {
                    map[key] = if (jsonObj.isNull(key)) null else jsonObj.optDouble(key)
                }
                map
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
