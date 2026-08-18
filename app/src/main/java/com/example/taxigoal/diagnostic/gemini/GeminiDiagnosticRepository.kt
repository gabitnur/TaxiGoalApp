package com.example.taxigoal.diagnostic.gemini

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date
import kotlin.system.measureTimeMillis

data class GeminiTestKey(val id: String, val value: String)

data class DiagnosticResult(
    val keyId: String,
    val modelName: String,
    val status: String, // WORKING, ERROR, TIMEOUT
    val errorCode: String? = null,
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiDiagnosticRepository(private val context: Context) {
    
    private val TEST_MODELS = listOf(
        com.example.taxigoal.GeminiManager.WORKING_MODEL_NAME,
        "gemini-2.0-flash-exp",
        "gemini-1.5-flash",
        "gemini-1.5-flash-8b",
        "gemini-1.5-pro"
    )

    suspend fun testCombination(keyId: String, apiKey: String, modelName: String): DiagnosticResult {
        AppLogger.info("GeminiDiag", "COMBINATION_TEST_START", "Testing $keyId with $modelName")
        
        var resultStatus = "ERROR"
        var errCode: String? = null
        var timeTaken = 0L

        try {
            timeTaken = measureTimeMillis {
                val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
                val response = model.generateContent("ping")
                if (response.text != null) {
                    resultStatus = "WORKING"
                } else {
                    errCode = "EMPTY_RESPONSE"
                }
            }
            AppLogger.info("GeminiDiag", "COMBINATION_TEST_SUCCESS", "$keyId + $modelName took ${timeTaken}ms")
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown"
            errCode = if (msg.contains("404")) "404" else if (msg.contains("401")) "401" else msg
            AppLogger.error("GeminiDiag", "COMBINATION_TEST_ERROR", "Failed $keyId + $modelName", code = errCode)
        }

        return DiagnosticResult(
            keyId = keyId,
            modelName = modelName,
            status = resultStatus,
            errorCode = errCode,
            duration = timeTaken
        )
    }

    fun getTestModels() = TEST_MODELS
}
