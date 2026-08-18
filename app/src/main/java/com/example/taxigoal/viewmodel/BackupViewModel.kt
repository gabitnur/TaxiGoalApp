package com.example.taxigoal.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.services.BackupFileInfo
import com.example.taxigoal.services.BackupManager
import com.example.taxigoal.utils.AppLogger
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
    data class BackupListLoaded(val files: List<BackupFileInfo>) : BackupState()
}

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state = _state.asStateFlow()

    private val _authIntentEvent = MutableSharedFlow<Intent>()
    val authIntentEvent = _authIntentEvent.asSharedFlow()

    fun createBackup() {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            AppLogger.info("Backup", "UI_START_BACKUP", "User requested backup")
            
            val result = BackupManager.createBackup(getApplication())
            
            result.onSuccess {
                _state.value = BackupState.Success("Резервная копия успешно создана в Google Drive")
            }.onFailure { e ->
                handleFailure(e)
            }
        }
    }

    fun loadBackupList() {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            AppLogger.info("Backup", "UI_LOAD_LIST", "User requested backup list")
            
            val result = BackupManager.getBackupFilesList(getApplication())
            
            result.onSuccess { files ->
                if (files.isEmpty()) {
                    _state.value = BackupState.Error("Резервные копии не найдены.")
                } else {
                    _state.value = BackupState.BackupListLoaded(files)
                }
            }.onFailure { e ->
                handleFailure(e)
            }
        }
    }

    fun restoreBackupById(fileId: String) {
        viewModelScope.launch {
            _state.value = BackupState.Loading
            AppLogger.info("Backup", "UI_RESTORE", "User requested restore ID: $fileId")
            
            val result = BackupManager.restoreBackupById(getApplication(), fileId)
            
            result.onSuccess {
                _state.value = BackupState.Success("Данные успешно восстановлены из облака")
            }.onFailure { e ->
                _state.value = BackupState.Error("Ошибка восстановления: ${e.message}")
            }
        }
    }

    private fun handleFailure(e: Throwable) {
        if (e is UserRecoverableAuthIOException) {
            viewModelScope.launch {
                _authIntentEvent.emit(e.intent)
            }
        } else {
            _state.value = BackupState.Error("Ошибка: ${e.message ?: "неизвестный сбой"}")
        }
    }

    fun resetState() {
        _state.value = BackupState.Idle
    }
}
