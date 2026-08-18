package com.example.taxigoal

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun wrap(context: Context): Context {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    // Still keep this for non-activity contexts if needed, but wrap is preferred
    fun applyLocale(context: Context) {
        val prefs = context.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val language = prefs.getString("app_language", "ru") ?: "ru"
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
