package com.example.taxigoal

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyAppTheme()
        super.onCreate(savedInstanceState)
    }

    private fun applyAppTheme() {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val themeStyle = prefs.getString("app_theme_style", "TAXI") ?: "TAXI"
        
        val themeResId = when (themeStyle) {
            "TAXI" -> R.style.Theme_TaxiGoal_Taxi
            "OCEAN" -> R.style.Theme_TaxiGoal_Ocean
            "ICE" -> R.style.Theme_TaxiGoal_Ice
            "MIDNIGHT" -> R.style.Theme_TaxiGoal_Midnight
            "FOREST" -> R.style.Theme_TaxiGoal_Forest
            "ROYAL" -> R.style.Theme_TaxiGoal_Royal
            else -> R.style.Theme_TaxiGoal_Taxi
        }
        setTheme(themeResId)
    }

    /**
     * Применяет кастомный цвет шрифта, если он выбран пользователем.
     */
    fun applyCustomFontColor(vararg views: TextView) {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val customColor = prefs.getInt("custom_font_color", 0)
        if (customColor != 0) {
            views.forEach { it.setTextColor(customColor) }
        }
    }
}
