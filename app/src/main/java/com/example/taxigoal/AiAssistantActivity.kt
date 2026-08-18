package com.example.taxigoal

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class AiAssistantActivity : BaseActivity() {

    private val scope = MainScope()
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvMessage: TextView
    private val repository by lazy { GeminiRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_assistant)

        tvMessage = findViewById(R.id.tvAiMessage)
        pbLoading = findViewById(R.id.pbAiLoading)
        val btnGoal = findViewById<Button>(R.id.btnAnalyzeGoal)
        val btnFuel = findViewById<Button>(R.id.btnAnalyzeFuel)
        val btnBack = findViewById<Button>(R.id.btnBackFromAi)

        btnBack.setOnClickListener { finish() }

        btnGoal.setOnClickListener {
            analyzeData("Анализируй мой прогресс к цели. Когда я ее достигну и какие советы дашь по доходам?")
        }

        btnFuel.setOnClickListener {
            analyzeData("Дай совет по расходам на топливо и оптимизации затрат на машину.")
        }
    }

    private fun analyzeData(question: String) {
        val prefs = getSharedPreferences("TaxiGoalPrefs", MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            tvMessage.text = "Ошибка: API ключ не настроен в Настройках"
            return
        }

        val acc = prefs.getFloat("total_accumulated", 0f).toInt()
        val target = prefs.getFloat("goal_target", 298000f).toInt()
        val history = prefs.getString("shift_history", "{}") ?: "{}"

        val prompt = """
            Ты экспертный ассистент водителя такси. 
            Данные пользователя:
            - Общая цель: $target тенге.
            - Накоплено: $acc тенге.
            - История заказов и расходов по дням: $history
            
            Запрос пользователя: $question
            Отвечай кратко, профессионально, на русском языке. Используй эмодзи.
        """.trimIndent()

        tvMessage.text = getString(R.string.ai_thinking)
        pbLoading.visibility = View.VISIBLE

        scope.launch {
            val result = repository.sendPromptWithRetry(prompt)
            runOnUiThread {
                pbLoading.visibility = View.GONE
                result.onSuccess { text ->
                    tvMessage.text = text
                }.onFailure { e ->
                    val errorMsg = e.message ?: ""
                    if (errorMsg.contains("503") || errorMsg.contains("demand", true)) {
                        tvMessage.text = "🤖 Сервер Google временно перегружен. Пожалуйста, попробуйте еще раз через минуту."
                    } else {
                        tvMessage.text = "Ошибка: ${e.localizedMessage}"
                    }
                }
            }
        }
    }
}
