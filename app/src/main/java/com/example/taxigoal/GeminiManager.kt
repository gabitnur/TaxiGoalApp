package com.example.taxigoal

import android.content.Context

object GeminiManager {
    // Постоянная рабочая модель на backend
    const val WORKING_MODEL_NAME = "gemini-flash-latest"

    /**
     * ПРИМЕЧАНИЕ: Начиная с версии 1.0.25, API ключи хранятся ТОЛЬКО на Firebase Backend.
     * Android-приложение больше не имеет прямого доступа к ключам Gemini.
     */
    fun isConfigured(context: Context): Boolean {
        // Мы считаем сервис настроенным, если пользователь авторизован в Firebase
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    }

    // Сохраняем для обратной совместимости, но логика теперь пустая
    fun getEffectiveApiKey(context: Context): String = "[REDACTED_MOVED_TO_SERVER]"
}
