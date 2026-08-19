package com.example.taxigoal

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import android.util.Log

object GeminiManager {
    // Постоянная рабочая модель
    const val WORKING_MODEL_NAME = "gemini-flash-latest"

    fun getEffectiveApiKey(context: Context): String {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val userKey = prefs.getString("gemini_api_key", "")
        
        val key = if (!userKey.isNullOrBlank()) userKey else BuildConfig.GEMINI_API_KEY
        
        // SAFE LOGGING: Only log the fact that the key is present
        if (key.isBlank()) {
            Log.w("Gemini", "GEMINI_API_KEY is EMPTY!")
        } else {
            Log.i("Gemini", "API_KEY_CONFIGURED=true")
        }
        Log.i("Gemini", "MODEL=$WORKING_MODEL_NAME")
        
        return key
    }

    // Для совместимости со старым кодом
    fun getModel(context: Context): GenerativeModel {
        val apiKey = getEffectiveApiKey(context)
        Log.i("Gemini", "CLIENT_INITIALIZED=true")
        return GenerativeModel(modelName = WORKING_MODEL_NAME, apiKey = apiKey)
    }

    fun getCandidateModels() = listOf(WORKING_MODEL_NAME, "gemini-flash-latest", "gemini-1.5-pro")

    fun saveWorkingModel(context: Context, modelName: String) {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("working_gemini_model", modelName).apply()
    }

    fun isConfigured(context: Context): Boolean {
        return getEffectiveApiKey(context).isNotBlank()
    }
}
