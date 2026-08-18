package com.example.taxigoal

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel

object GeminiManager {
    // Постоянная рабочая модель
    const val WORKING_MODEL_NAME = "gemini-flash-latest"
    
    // Встроенный резервный API-ключ по умолчанию
    private const val DEFAULT_API_KEY = "AQ.Ab8RN6Lv5sujWCaYVYviQgAweVjfpnDQArGt0BRe-8H3sSH-RQ"

    fun getEffectiveApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val userKey = prefs.getString("gemini_api_key", "")
        return if (!userKey.isNullOrBlank()) userKey else DEFAULT_API_KEY
    }

    // Для совместимости со старым кодом
    fun getModel(context: Context): GenerativeModel {
        val apiKey = getEffectiveApiKey(context)
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val modelName = prefs.getString("working_gemini_model", WORKING_MODEL_NAME) ?: WORKING_MODEL_NAME
        return GenerativeModel(modelName = modelName, apiKey = apiKey)
    }

    fun getCandidateModels() = listOf(WORKING_MODEL_NAME, "gemini-pro-latest", "gemini-2.5-flash")

    fun saveWorkingModel(context: Context, modelName: String) {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("working_gemini_model", modelName).apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getEffectiveApiKey(context).isNotBlank()
    }
}
