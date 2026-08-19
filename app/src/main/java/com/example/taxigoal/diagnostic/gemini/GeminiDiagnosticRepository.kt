package com.example.taxigoal.diagnostic.gemini

import android.content.Context
import com.example.taxigoal.GeminiRepository
import com.example.taxigoal.utils.AppLogger
import kotlin.system.measureTimeMillis

data class DiagnosticResult(
    val keyId: String,
    val modelName: String,
    val status: String, // WORKING, ERROR
    val errorCode: String? = null,
    val duration: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class GeminiDiagnosticRepository(private val context: Context) {
    
    private val repository = GeminiRepository(context)

    suspend fun testService(): DiagnosticResult {
        AppLogger.info("GeminiDiag", "SERVICE_TEST_START", "Testing Backend Function")
        
        var resultStatus = "ERROR"
        var errCode: String? = null
        var timeTaken = 0L

        try {
            timeTaken = measureTimeMillis {
                val result = repository.sendPromptWithRetry("ping")
                if (result.isSuccess) {
                    resultStatus = "WORKING"
                } else {
                    errCode = result.exceptionOrNull()?.message
                }
            }
        } catch (e: Exception) {
            errCode = e.message
        }

        return DiagnosticResult(
            keyId = "BACKEND",
            modelName = "gemini-1.5-flash",
            status = resultStatus,
            errorCode = errCode,
            duration = timeTaken
        )
    }
}
