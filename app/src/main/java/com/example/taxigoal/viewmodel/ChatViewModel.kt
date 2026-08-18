package com.example.taxigoal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.GeminiManager
import com.example.taxigoal.GeminiRepository
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GeminiRepository(application)
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Привет! Я ваш финансовый помощник. Задайте вопрос о ваших доходах, расходах или целях.", false)
    ))
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Убрана блокировка, так как теперь всегда есть EffectiveApiKey
        if (!GeminiManager.isConfigured(getApplication())) {
            _messages.value = _messages.value + ChatMessage("Сервис Gemini временно недоступен.", false)
            return
        }

        viewModelScope.launch {
            AppLogger.info("Chat", "GEMINI_REQUEST_START", "Sending user message")
            _isLoading.value = true
            _error.value = null
            
            val userMsg = ChatMessage(text, true)
            _messages.value = _messages.value + userMsg
            
            try {
                // In a real chat we would use generativeModel.startChat()
                val result = repository.sendPromptWithRetry(text)
                
                result.onSuccess { reply ->
                    _messages.value = _messages.value + ChatMessage(reply, false)
                    AppLogger.info("Chat", "GEMINI_REQUEST_SUCCESS", "AI reply received")
                }.onFailure { e ->
                    _error.value = "Ошибка: ${e.message}"
                    AppLogger.error("Chat", "GEMINI_REQUEST_ERROR", "AI request failed", details = e.message)
                }
            } catch (e: Exception) {
                _error.value = "Критическая ошибка: ${e.message}"
                AppLogger.error("Chat", "GEMINI_REQUEST_ERROR", "Exception in chat", details = e.message)
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
