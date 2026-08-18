package com.example.taxigoal.diagnostic.gemini

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeminiDiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = GeminiDiagnosticRepository(application)
    private val prefs = application.getSharedPreferences("GeminiDiagPrefs", Context.MODE_PRIVATE)

    private val _testKeys = MutableStateFlow<List<GeminiTestKey>>(loadKeys())
    val testKeys = _testKeys.asStateFlow()

    private val _results = MutableStateFlow<List<DiagnosticResult>>(emptyList())
    val results = _results.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    fun addKey(value: String) {
        val nextId = "KEY_${String.format("%02d", _testKeys.value.size + 1)}"
        val newList = _testKeys.value + GeminiTestKey(nextId, value)
        _testKeys.value = newList
        saveKeys(newList)
    }

    fun deleteKeys() {
        _testKeys.value = emptyList()
        saveKeys(emptyList())
        AppLogger.info("GeminiDiag", "TEST_DATA_DELETED", "All test keys removed")
    }

    fun runAllTests() {
        if (_testKeys.value.isEmpty()) {
            android.widget.Toast.makeText(getApplication(), "Добавьте хотя бы один ключ", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            _isTesting.value = true
            _results.value = emptyList()
            AppLogger.info("GeminiDiag", "DIAGNOSTIC_START", "Starting all combinations test")
            
            val models = repository.getTestModels()
            val keys = _testKeys.value
            val total = (models.size * keys.size).toFloat()
            var current = 0

            for (key in keys) {
                for (model in models) {
                    val res = repository.testCombination(key.id, key.value, model)
                    _results.value = _results.value + res
                    current++
                    _progress.value = current / total
                }
            }
            
            _isTesting.value = false
            AppLogger.info("GeminiDiag", "DIAGNOSTIC_COMPLETE", "Finished testing all combinations")
        }
    }

    fun setActive(result: DiagnosticResult) {
        val mainPrefs = getApplication<Application>().getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)
        val keyVal = _testKeys.value.find { it.id == result.keyId }?.value ?: ""
        mainPrefs.edit()
            .putString("gemini_api_key", keyVal)
            .putString("working_gemini_model", result.modelName)
            .apply()
        AppLogger.info("GeminiDiag", "COMBINATION_ACTIVATED", "${result.keyId} + ${result.modelName}")
        
        android.widget.Toast.makeText(getApplication(), "Активировано: ${result.modelName}", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun saveKeys(keys: List<GeminiTestKey>) {
        val set = keys.map { "${it.id}|${it.value}" }.toSet()
        prefs.edit().putStringSet("test_keys", set).apply()
    }

    private fun loadKeys(): List<GeminiTestKey> {
        val set = prefs.getStringSet("test_keys", emptySet()) ?: emptySet()
        return set.map {
            val parts = it.split("|")
            GeminiTestKey(parts[0], parts[1])
        }.sortedBy { it.id }
    }
}
