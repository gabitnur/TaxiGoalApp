package com.example.taxigoal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.services.UpdateInfo
import com.example.taxigoal.services.UpdateManager
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            AppLogger.info("Update", "UI_CHECK_START", "User triggered update check")
            
            val result = UpdateManager.checkUpdate(getApplication())
            
            result.onSuccess { info ->
                if (info != null) {
                    _state.value = UpdateState.Available(info)
                } else {
                    _state.value = UpdateState.UpToDate
                }
            }.onFailure { e ->
                _state.value = UpdateState.Error(e.message ?: "Не удалось получить данные")
            }
        }
    }

    fun startUpdate(info: UpdateInfo) {
        UpdateManager.startDownload(getApplication(), info.apkUrl)
    }
}
