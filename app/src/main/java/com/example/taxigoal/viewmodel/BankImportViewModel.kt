package com.example.taxigoal.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.data.entities.FinancialTransaction
import com.example.taxigoal.data.repository.TaxiRepository
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.services.pdf.PdfParser
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BankImportState {
    object Idle : BankImportState()
    object Loading : BankImportState()
    data class Success(val transactions: List<FinancialTransaction>) : BankImportState()
    data class Error(val message: String) : BankImportState()
}

class BankImportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TaxiRepository
    private val _state = MutableStateFlow<BankImportState>(BankImportState.Idle)
    val state = _state.asStateFlow()

    init {
        val taxiDao = TaxiDatabase.getDatabase(application).taxiDao()
        repository = TaxiRepository(taxiDao)
    }

    fun parsePdf(uri: Uri) {
        viewModelScope.launch {
            _state.value = BankImportState.Loading
            AppLogger.info("BankImport", "START", "Parsing PDF: $uri")
            
            val result = PdfParser.parseBankPdf(getApplication(), uri)
            
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _state.value = BankImportState.Error("Не удалось найти операции в файле.")
                } else {
                    _state.value = BankImportState.Success(list)
                }
            }.onFailure { e ->
                _state.value = BankImportState.Error(e.message ?: "Ошибка чтения PDF")
            }
        }
    }

    fun saveTransactions(list: List<FinancialTransaction>) {
        viewModelScope.launch {
            try {
                list.forEach { repository.insertTransaction(it) }
                AppLogger.info("BankImport", "SAVE_SUCCESS", "Imported ${list.size} operations")
                _state.value = BankImportState.Idle
            } catch (e: Exception) {
                AppLogger.error("BankImport", "SAVE_FAILED", e.message ?: "")
            }
        }
    }

    fun reset() {
        _state.value = BankImportState.Idle
    }
}
