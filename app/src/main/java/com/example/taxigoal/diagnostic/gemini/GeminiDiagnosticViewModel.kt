package com.example.taxigoal.diagnostic.gemini

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeminiDiagnosticViewModel(private val repository: GeminiDiagnosticRepository) : ViewModel() {
    
    private val _results = MutableStateFlow<List<DiagnosticResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    fun runDiagnostic() {
        viewModelScope.launch {
            _isRunning.value = true
            _results.value = emptyList()
            
            val result = repository.testService()
            _results.value = listOf(result)
            
            _isRunning.value = false
        }
    }
}
