package com.example.taxigoal

import android.content.Context
import android.util.Log
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository(private val context: Context) {

    // Vercel Endpoint from BuildConfig (Must end with /)
    private val BASE_URL = if (BuildConfig.BASE_URL.endsWith("/")) BuildConfig.BASE_URL else "${BuildConfig.BASE_URL}/" 
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendPromptWithRetry(prompt: String, safeContext: String = "", retries: Int = 3): Result<String> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(retries) { attempt ->
            val requestId = "VER-${System.currentTimeMillis()}-${(100..999).random()}"
            
            try {
                AppLogger.info("Gemini", "DEBUG_REQUEST", "RequestId: $requestId | Backend: VERCEL | Attempt: ${attempt + 1}")
                
                // 1. Get Fresh Firebase ID Token
                val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("AUTH_REQUIRED")
                val tokenResult = user.getIdToken(true).await()
                val idToken = tokenResult.token ?: throw Exception("TOKEN_MISSING")

                // 2. Prepare Request Body
                val bodyJson = JSONObject().apply {
                    put("message", prompt)
                    put("safeContext", safeContext)
                    put("requestId", requestId)
                }

                // Path must match exactly with Vercel function: api/gemini.ts
                val request = Request.Builder()
                    .url("$BASE_URL/api/gemini")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(bodyJson.toString().toRequestBody(jsonMediaType))
                    .build()

                // 3. Execute Call
                client.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        Log.e("Gemini", "Эндпоинт Vercel не найден (HTTP 404). Проверьте URL в GeminiRepository. URL: ${request.url}")
                        return@withContext Result.failure(Exception("BACKEND_ROUTE_NOT_FOUND"))
                    }

                    val responseBody = response.body?.string() ?: ""
                    val responseJson = if (responseBody.startsWith("{")) JSONObject(responseBody) else JSONObject()

                    if (response.isSuccessful) {
                        val reply = responseJson.optString("reply")
                        if (reply.isNotBlank()) {
                            AppLogger.info("Gemini", "REQUEST_SUCCESS", "RequestId: $requestId | Reply received")
                            return@withContext Result.success(reply)
                        }
                    }

                    // Handle Structured Errors from Vercel
                    val errorType = responseJson.optString("errorType", "UNKNOWN_ERROR")
                    val message = responseJson.optString("message", "HTTP ${response.code}")
                    
                    AppLogger.error("Gemini", "DEBUG_ERROR", "RequestId: $requestId | Type: $errorType | Msg: $message")
                    
                    if (errorType == "TEMPORARILY_UNAVAILABLE" || errorType == "RATE_LIMITED" || response.code == 503 || response.code == 429) {
                        throw Exception("RETRYABLE|$errorType")
                    } else {
                        return@withContext Result.failure(Exception(errorType))
                    }
                }

            } catch (e: Exception) {
                lastException = e
                if (e.message?.startsWith("RETRYABLE") == true) {
                    AppLogger.warn("Gemini", "REQUEST_RETRY", "RequestId: $requestId | Retrying...")
                    kotlinx.coroutines.delay(1000L * (attempt + 1))
                } else {
                    return@withContext Result.failure(e)
                }
            }
        }
        
        Result.failure(lastException ?: Exception("RETRY_EXHAUSTED"))
    }

    suspend fun submitReport(report: JSONObject): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: throw Exception("AUTH_REQUIRED")
            val idToken = user.getIdToken(true).await().token ?: throw Exception("TOKEN_MISSING")

            val request = Request.Builder()
                .url("$BASE_URL/api/report")
                .addHeader("Authorization", "Bearer $idToken")
                .post(report.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    Log.e("Gemini", "Эндпоинт Vercel (Report) не найден (HTTP 404).")
                    return@withContext Result.failure(Exception("BACKEND_ROUTE_NOT_FOUND"))
                }
                
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val json = JSONObject(body)
                    Result.success(json.optString("reportId", "OK"))
                } else {
                    Result.failure(Exception("Submission failed: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
}
