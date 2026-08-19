package com.example.taxigoal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        
        val rgThemes = findViewById<RadioGroup>(R.id.rgThemes)
        val btnBack = findViewById<Button>(R.id.btnBackFromSettings)
        val btnLanguage = findViewById<Button>(R.id.btnSelectLanguage)

        // 1. Управление темами
        val currentTheme = prefs.getString("app_theme_style", "TAXI") ?: "TAXI"
        when (currentTheme) {
            "TAXI" -> rgThemes.check(R.id.rbTaxi)
            "OCEAN" -> rgThemes.check(R.id.rbOcean)
            "ICE" -> rgThemes.check(R.id.rbIce)
            "MIDNIGHT" -> rgThemes.check(R.id.rbMidnight)
            "FOREST" -> rgThemes.check(R.id.rbForest)
            "ROYAL" -> rgThemes.check(R.id.rbRoyal)
        }

        rgThemes.setOnCheckedChangeListener { _, id ->
            val themeName = when (id) {
                R.id.rbTaxi -> "TAXI"
                R.id.rbOcean -> "OCEAN"
                R.id.rbIce -> "ICE"
                R.id.rbMidnight -> "MIDNIGHT"
                R.id.rbForest -> "FOREST"
                R.id.rbRoyal -> "ROYAL"
                else -> "TAXI"
            }
            prefs.edit().putString("app_theme_style", themeName).apply()
            recreate()
        }

        // 2. Управление цветом шрифта
        setupColorPicker(prefs)

        // 3. Партнеры (Alaman)
        findViewById<Button>(R.id.btnOpenAlamanApp).setOnClickListener {
            startActivity(Intent(this, AlamanPartnerActivity::class.java))
        }
        findViewById<Button>(R.id.btnOpenAlamanNews).setOnClickListener {
            openTelegramLink("https://t.me/AlamanNews")
        }

        // Set Version Text
        findViewById<TextView>(R.id.tvAppVersion).text = "Версия: ${BuildConfig.VERSION_NAME}"

        // 4. Gemini AI (Backend Managed)
        findViewById<Button>(R.id.btnVerifyApiKey)?.apply {
            text = "AI Помощник активен ✅"
            isEnabled = false
        }

        // 5. Язык
        val currentLang = prefs.getString("app_language", "ru") ?: "ru"
        btnLanguage.text = if (currentLang == "kk") "Қазақша" else "Русский"
        btnLanguage.setOnClickListener {
            val languages = arrayOf("Русский", "Қазақша")
            AlertDialog.Builder(this).setTitle(getString(R.string.language)).setItems(languages) { _, which ->
                prefs.edit().putString("app_language", if (which == 1) "kk" else "ru").apply()
                recreate()
            }.show()
        }

        findViewById<Button>(R.id.btnViewLogs).setOnClickListener {
            startActivity(Intent(this, LogViewerActivity::class.java))
        }

        findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            FirebaseSyncManager.signOut(this)
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun setupColorPicker(prefs: android.content.SharedPreferences) {
        val colors = mapOf(
            R.id.colorWhite to R.color.picker_white,
            R.id.colorGold to R.color.picker_gold,
            R.id.colorBlue to R.color.picker_blue,
            R.id.colorGreen to R.color.picker_green,
            R.id.colorPink to R.color.picker_pink
        )

        colors.forEach { (viewId, colorRes) ->
            findViewById<View>(viewId).setOnClickListener {
                val color = ContextCompat.getColor(this, colorRes)
                prefs.edit().putInt("custom_font_color", color).apply()
                recreate()
            }
        }

        findViewById<View>(R.id.colorReset).setOnClickListener {
            prefs.edit().putInt("custom_font_color", 0).apply()
            recreate()
        }
    }

    private fun openTelegramLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Telegram не установлен", Toast.LENGTH_SHORT).show()
        }
    }
}
