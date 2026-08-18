package com.example.taxigoal

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel

object GeminiManager {
    // Постоянная рабочая модель
    const val WORKING_MODEL_NAME = "gemini-flash-latest"

    fun getEffectiveApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val userKey = prefs.getString("gemini_api_key", "")
        return if (!userKey.isNullOrBlank()) userKey else BuildConfig.GEMINI_API_KEY
    }

    // Для совместимости со старым кодом
    fun getModel(context: Context): GenerativeModel {
        val apiKey = getEffectiveApiKey(context)
        return GenerativeModel(modelName = WORKING_MODEL_NAME, apiKey = apiKey)
    }

    fun getCandidateModels() = listOf(WORKING_MODEL_NAME)

    fun saveWorkingModel(context: Context, modelName: String) {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("working_gemini_model", modelName).apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getEffectiveApiKey(context).isNotBlank()
    }
}
