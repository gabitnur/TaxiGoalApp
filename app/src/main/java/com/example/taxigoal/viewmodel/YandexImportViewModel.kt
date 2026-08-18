package com.example.taxigoal.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.data.entities.Shift
import com.example.taxigoal.data.repository.TaxiRepository
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

sealed class YandexImportState {
    object Idle : YandexImportState()
    object Loading : YandexImportState()
    data class Success(val data: YandexDeductions) : YandexImportState()
    data class Error(val message: String) : YandexImportState()
}

data class YandexDeductions(
    val commission: Double,
    val serviceFee: Double,
    val paidOptions: Double,
    val other: Double
) {
    val total: Double get() = commission + serviceFee + paidOptions + other
}

class YandexImportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaxiRepository
    private val _state = MutableStateFlow<YandexImportState>(YandexImportState.Idle)
    val state = _state.asStateFlow()

    init {
        val taxiDao = TaxiDatabase.getDatabase(application).taxiDao()
        repository = TaxiRepository(taxiDao)
    }

    fun processScreenshot(uri: Uri) {
        viewModelScope.launch {
            _state.value = YandexImportState.Loading
            AppLogger.info("YandexImport", "OCR_START", "Processing URI: $uri")
            
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val size = inputStream?.available() ?: 0
                inputStream?.close()

                if (size > 10 * 1024 * 1024) {
                    AppLogger.warn("YandexImport", "FILE_REJECTED", "Too large: $size")
                    _state.value = YandexImportState.Error("Файл слишком большой (>10 МБ).")
                    return@launch
                }

                // AI Recognition Logic (Simulated for this version)
                delay(1500)
                val recognizedData = YandexDeductions(4800.0, 150.0, 300.0, 0.0)
                
                AppLogger.info("YandexImport", "OCR_SUCCESS", "Extracted: ${recognizedData.total} ₸")
                _state.value = YandexImportState.Success(recognizedData)
                
            } catch (e: Exception) {
                val details = AppLogger.getErrorDetails(e)
                AppLogger.error("YandexImport", "OCR_FAIL", "Exception during parsing", details = details)
                _state.value = YandexImportState.Error("Ошибка распознавания. Проверьте формат файла.")
            }
        }
    }

    fun confirmAndSave(data: YandexDeductions) {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
                val shift = Shift(
                    userId = uid,
                    date = Date(),
                    grossIncome = 0.0,
                    fuelCost = 0.0,
                    mileage = 0.0,
                    maintenanceCost = 0.0,
                    fineCost = 0.0,
                    otherExpenses = data.total,
                    commissions = data.commission + data.serviceFee,
                    netProfit = -(data.total)
                )
                repository.insertShift(shift)
                AppLogger.info("YandexImport", "DB_SAVE", "Deductions recorded")
                _state.value = YandexImportState.Idle
            } catch (e: Exception) {
                AppLogger.error("YandexImport", "SAVE_FAIL", AppLogger.getErrorDetails(e))
            }
        }
    }

    fun reset() {
        _state.value = YandexImportState.Idle
    }
}
