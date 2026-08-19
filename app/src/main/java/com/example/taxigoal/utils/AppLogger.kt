package com.example.taxigoal.utils

import android.content.Context
import android.util.Log
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.data.entities.AppLog
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*

object AppLogger {
    private const val TAG = "AppLogger"
    private val sessionId = UUID.randomUUID().toString()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var database: TaxiDatabase? = null

    fun init(context: Context) {
        database = TaxiDatabase.getDatabase(context)
        info("SYSTEM", "LOG_INIT", "Logger initialized with Session ID: $sessionId")
    }

    private fun getUserIdHash(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "unauthenticated"
        return try {
            MessageDigest.getInstance("SHA-256")
                .digest(uid.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(12)
        } catch (e: Exception) { "anonymous" }
    }

    fun log(
        level: String,
        screen: String,
        event: String,
        message: String,
        details: String? = null,
        errorCode: String? = null
    ) {
        // SECURITY: Sanitize message and details before anything else
        val sanitizedMsg = sanitize(message)
        val sanitizedDetails = details?.let { sanitize(it) }

        // Local Logcat for developers (sanitized)
        Log.println(mapLevel(level), TAG, "[$screen] $event: $sanitizedMsg | Details: ${sanitizedDetails?.take(100)}")
        
        scope.launch {
            try {
                val logEntry = AppLog(
                    level = level,
                    screen = screen,
                    event = event,
                    title = event,
                    message = sanitizedMsg,
                    errorCode = errorCode,
                    sessionId = sessionId,
                    userIdHash = getUserIdHash(),
                    details = sanitizedDetails
                )
                database?.logDao()?.insertLog(logEntry)
                
                // Auto-cleanup old logs (older than 30 days)
                val threshold = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                database?.logDao()?.deleteOldLogs(threshold)
            } catch (e: Exception) {
                // Fail-safe: Avoid recursion if DB is down
            }
        }
    }

    /**
     * Extracts deep error details to avoid "null" messages.
     */
    fun getErrorDetails(e: Throwable): String {
        val stackTrace = Log.getStackTraceString(e).take(500)
        val cause = e.cause?.message ?: "No cause"
        return "Type: ${e.javaClass.simpleName} | Msg: ${e.message} | Cause: $cause | Trace: $stackTrace"
    }

    /**
     * Masks sensitive patterns: API Keys, Email addresses, Tokens.
     */
    fun sanitize(input: String): String {
        return input.replace(Regex("AIza[0-9A-Za-z-_]{35}"), "[REDACTED_API_KEY]")
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), "[REDACTED_EMAIL]")
            .replace(Regex("ya29\\.[0-9A-Za-z-_]+"), "[REDACTED_TOKEN]")
            .replace(Regex("Bearer\\s+[0-9A-Za-z-_.]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
            .replace(Regex("password=\\S+", RegexOption.IGNORE_CASE), "password=[REDACTED]")
    }

    // Convenience methods
    fun debug(screen: String, event: String, message: String) = log("DEBUG", screen, event, message)
    fun info(screen: String, event: String, message: String) = log("INFO", screen, event, message)
    fun warn(screen: String, event: String, message: String) = log("WARN", screen, event, message)
    fun error(screen: String, event: String, message: String, details: String? = null, code: String? = null) = 
        log("ERROR", screen, event, message, details, code)
    fun fatal(screen: String, event: String, message: String, details: String? = null) = 
        log("FATAL", screen, event, message, details)

    private fun mapLevel(level: String): Int = when(level) {
        "DEBUG" -> Log.DEBUG
        "INFO" -> Log.INFO
        "WARN" -> Log.WARN
        "ERROR" -> Log.ERROR
        "FATAL" -> Log.ERROR
        else -> Log.VERBOSE
    }

    fun screenOpen(name: String) = info(name, "SCREEN_OPEN", "User entered screen")
    fun buttonClick(screen: String, buttonId: String) = info(screen, "BUTTON_CLICK", "Clicked: $buttonId")
}
