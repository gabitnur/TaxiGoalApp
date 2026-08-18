package com.example.taxigoal

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "TAXI_GOAL_DEBUG"
    private const val LOG_FILE_NAME = "app_debug_logs.txt"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logError(context: Context?, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val version = getVersion(context)
        val fullMessage = "[$timestamp] [v$version] ERROR: $message"
        
        Log.e(TAG, fullMessage, throwable)

        scope.launch {
            val logText = if (throwable != null) {
                "$fullMessage\nException: ${throwable.message}\n${Log.getStackTraceString(throwable)}"
            } else {
                fullMessage
            }
            writeToFile(context, logText)
            logToFirestore(message, "ERROR", throwable)
        }
    }

    fun logInfo(context: Context?, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val version = getVersion(context)
        val fullMessage = "[$timestamp] [v$version] INFO: $message"
        
        Log.d(TAG, fullMessage)

        scope.launch {
            writeToFile(context, fullMessage)
            logToFirestore(message, "INFO")
        }
    }

    private fun getVersion(context: Context?): String {
        return try {
            context?.packageManager?.getPackageInfo(context.packageName, 0)?.versionName ?: "unknown"
        } catch (e: Exception) { "unknown" }
    }

    private fun writeToFile(context: Context?, text: String) {
        if (context == null) return
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            
            // Простая ротация: если файл больше 1МБ, удаляем старый
            if (logFile.exists() && logFile.length() > 1024 * 1024) {
                logFile.delete()
            }
            
            logFile.appendText("$text\n-------------------\n")
        } catch (e: Exception) {
            Log.e(TAG, "File write failed", e)
        }
    }

    private fun logToFirestore(message: String, level: String, throwable: Throwable? = null) {
        val uid = FirebaseSyncManager.getUserId() ?: return
        val logData = mutableMapOf<String, Any>(
            "message" to message,
            "level" to level,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})"
        )
        
        if (throwable != null) {
            logData["error"] = throwable.message ?: "Unknown"
            logData["stacktrace"] = Log.getStackTraceString(throwable)
        }

        try {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("logs").add(logData)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore log failed: ${e.message}")
        }
    }

    fun getLogFileContent(context: Context): String {
        return try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) logFile.readText() else "Лог-файл пуст."
        } catch (e: Exception) {
            "Ошибка чтения: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists()) logFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Clear logs failed", e)
        }
    }
}
