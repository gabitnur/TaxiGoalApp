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
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            findViewById<TextView>(R.id.tvAppVersion).text = "Версия: ${pInfo.versionName}"
        } catch (e: Exception) {}

        // 4. Gemini Key + Verification (Restore robust logic)
        val etApiKey = findViewById<EditText>(R.id.etGeminiApiKey)
        etApiKey.setText(prefs.getString("gemini_api_key", ""))
        
        findViewById<Button>(R.id.btnVerifyApiKey).setOnClickListener {
            val key = etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, getString(R.string.key_empty), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val progress = AlertDialog.Builder(this)
                .setMessage(getString(R.string.key_verify_progress))
                .setCancelable(false)
                .show()
            
            MainScope().launch {
                var foundWorking = false
                val candidates = GeminiManager.getCandidateModels()
                
                for (modelName in candidates) {
                    try {
                        val testModel = com.google.ai.client.generativeai.GenerativeModel(modelName = modelName, apiKey = key)
                        val response = withContext(Dispatchers.IO) {
                            testModel.generateContent("Say hello")
                        }
                        
                        if (response.text != null) {
                            foundWorking = true
                            GeminiManager.saveWorkingModel(this@SettingsActivity, modelName)
                            prefs.edit().putString("gemini_api_key", key).apply()
                            
                            runOnUiThread {
                                progress.dismiss()
                                Toast.makeText(this@SettingsActivity, "✅ Ключ принят! Модель: $modelName", Toast.LENGTH_LONG).show()
                            }
                            break 
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: ""
                        if (errorMsg.contains("invalid", true) || errorMsg.contains("API_KEY_INVALID", true)) {
                             // Если ключ в принципе неверный - нет смысла перебирать дальше
                             break
                        }
                    }
                }
                
                if (!foundWorking) {
                    runOnUiThread {
                        progress.dismiss()
                        Toast.makeText(this@SettingsActivity, "❌ Ошибка: Ключ не принят сервером или нет сети", Toast.LENGTH_LONG).show()
                    }
                }
            }
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
