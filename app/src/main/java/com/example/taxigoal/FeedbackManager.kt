package com.example.taxigoal

import android.content.Context
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object FeedbackManager {

    private val db by lazy { FirebaseFirestore.getInstance() }

    suspend fun fixError(
        context: Context,
        date: String,
        currentJson: String,
        feedbackText: String,
        onComplete: (Boolean, String?) -> Unit
    ) = withContext(Dispatchers.IO) {
        val repository = GeminiRepository(context)
        val prompt = """
            Current record for date $date: $currentJson
            User wants to correct it: $feedbackText
            Commission rules: Yandex=${CommissionManager.getYandexPercent()}%, Park=${CommissionManager.getParkPercent()}%, SocialFee=${CommissionManager.getSocialFee()}.
            Recalculate net profit using the formula: Net = (Card + Cash) - (YandexComm + ParkComm) - (Expenses).
            Return ONLY a valid JSON object with fields: "card", "cash", "expenses", "net".
        """.trimIndent()

        try {
            val result = repository.sendPromptWithRetry(prompt)
            
            result.onSuccess { reply ->
                val cleanJson = reply.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                val correctedJson = JSONObject(cleanJson)
                
                // Log to Firestore
                val logData = mapOf(
                    "date" to date,
                    "original" to currentJson,
                    "feedback" to feedbackText,
                    "corrected" to cleanJson,
                    "timestamp" to System.currentTimeMillis(),
                    "user_id" to FirebaseSyncManager.getUserId()
                )
                db.collection("feedback_logs").add(logData)

                withContext(Dispatchers.Main) {
                    applyCorrection(context, date, correctedJson)
                    onComplete(true, null)
                }
            }.onFailure { e ->
                throw e
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onComplete(false, e.message)
            }
        }
    }

    private suspend fun applyCorrection(context: Context, date: String, correctedJson: JSONObject) {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val history = JSONObject(prefs.getString("shift_history", "{}") ?: "{}")
        
        val oldNet = history.optJSONObject(date)?.optDouble("net", 0.0) ?: 0.0
        val newNet = correctedJson.optDouble("net", 0.0)
        
        // Сохраняем историю транзакций, если она была, и добавляем запись о корректировке
        val oldTransactions = history.optJSONObject(date)?.optJSONArray("transactions") ?: org.json.JSONArray()
        val correctionNote = JSONObject().apply {
            put("time", java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            put("card", 0.0)
            put("cash", 0.0)
            put("expenses", 0.0)
            put("source", "AI Исправление")
            put("note", "Данные пересчитаны")
        }
        oldTransactions.put(correctionNote)
        correctedJson.put("transactions", oldTransactions)

        history.put(date, correctedJson)
        
        val totalGoal = prefs.getFloat("total_accumulated", 0f) - oldNet.toFloat() + newNet.toFloat()

        prefs.edit()
            .putString("shift_history", history.toString())
            .putFloat("total_accumulated", totalGoal.coerceAtLeast(0f))
            .apply()

        FirebaseSyncManager.uploadDayData(
            context, date,
            (correctedJson.optDouble("card") + correctedJson.optDouble("cash")).toInt(),
            correctedJson.optInt("expenses"),
            newNet.toInt(),
            correctedJson.optInt("card"),
            correctedJson.optInt("cash"),
            correctedJson.toString()
        )
    }
}
