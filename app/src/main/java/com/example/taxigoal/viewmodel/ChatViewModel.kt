package com.example.taxigoal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.GeminiManager
import com.example.taxigoal.GeminiRepository
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.data.repository.AuthRepository
import com.example.taxigoal.data.repository.TaxiRepository
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val geminiRepository = GeminiRepository(application)
    private val taxiRepository: TaxiRepository
    private val authRepository = AuthRepository(FirebaseAuth.getInstance())
    
    init {
        val taxiDao = TaxiDatabase.getDatabase(application).taxiDao()
        taxiRepository = TaxiRepository(taxiDao)
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Привет! Я ваш финансовый помощник. Задайте вопрос о ваших доходах, расходах или целях.", false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private suspend fun getSystemContext(): String = withContext(Dispatchers.IO) {
        val userId = authRepository.getUserId() ?: return@withContext "Пользователь не авторизован."
        val activeGoal = taxiRepository.getActiveGoalSync(userId)
        val allShifts = taxiRepository.getAllShiftsSync(userId)
        
        // ARCHITECTURE FIX: Correct average income calculation based on actual period
        val avgIncome = if (allShifts.isNotEmpty()) {
            val firstDate = allShifts.last().date
            val lastDate = allShifts.first().date
            val diffMs = lastDate.time - firstDate.time
            val days = TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(1)
            
            // Limit to last 30 days of data for calculation if period is longer
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
            val recentShifts = allShifts.filter { it.date.after(thirtyDaysAgo) }
            val recentDays = if (days > 30) 30L else days
            
            recentShifts.sumOf { it.netProfit } / recentDays.toDouble()
        } else 0.0

        val goalName = activeGoal?.title ?: "Нет активной цели"
        val targetAmount = activeGoal?.targetAmount ?: 0.0
        val currentSaved = activeGoal?.accumulatedAmount ?: 0.0
        val remaining = if (targetAmount > currentSaved) targetAmount - currentSaved else 0.0

        """
            Ты — финансовый аналитик в приложении "Мой доход". 

            ТЕКУЩИЕ ДАННЫЕ ПОЛЬЗОВАТЕЛЯ ИЗ БАЗЫ ДАННЫХ:
            - Активная цель: $goalName
            - Нужная сумма: ${targetAmount.toInt()} тг
            - Накоплено сейчас: ${currentSaved.toInt()} тг
            - Осталось накопить: ${remaining.toInt()} тг
            - Средний доход в день (на основе реальных данных): ${avgIncome.toInt()} тг

            ИНСТРУКЦИИ ДЛЯ РАСЧЕТОВ:
            1. При вопросах "сколько накопил / сколько осталось": используй сухие цифры из блока выше.
            2. При вопросах "за сколько дней накоплю": если средний доход > 0, дели (Остаток / Средний доход в день). Выводи точное количество дней и примерную дату достижения цели. Если средний доход 0, скажи, что нужно больше данных о сменах.
            3. Категорически запрещено писать фразы вроде "у меня нет доступа к вашим данным".
            4. Отвечай кратко, строго по делу, без приветствий и сносок.
        """.trimIndent()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        if (!GeminiManager.isConfigured(getApplication())) {
            _messages.value = _messages.value + ChatMessage("Сервис Gemini временно недоступен.", false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val userMsg = ChatMessage(text, true)
            _messages.value = _messages.value + userMsg
            
            try {
                // ANR FIX: Database calls now happen on Dispatchers.IO inside getSystemContext()
                val context = getSystemContext()
                val fullPrompt = "$context\n\nВОПРОС ПОЛЬЗОВАТЕЛЯ: $text"
                
                val result = geminiRepository.sendPromptWithRetry(fullPrompt)
                
                result.onSuccess { reply ->
                    _messages.value = _messages.value + ChatMessage(reply, false)
                    AppLogger.info("Gemini", "REQUEST_SUCCESS", "AI reply received")
                }.onFailure { e ->
                    _error.value = "Ошибка: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Критическая ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage("Привет! История очищена. Чем могу помочь?", false)
        )
        AppLogger.info("Chat", "HISTORY_CLEAR", "User cleared chat history")
    }
}
