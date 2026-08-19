package com.example.taxigoal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.data.entities.*
import com.example.taxigoal.data.repository.AuthRepository
import com.example.taxigoal.data.repository.TaxiRepository
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaxiRepository
    private val authRepository = AuthRepository(com.google.firebase.auth.FirebaseAuth.getInstance())

    init {
        val taxiDao = TaxiDatabase.getDatabase(application).taxiDao()
        repository = TaxiRepository(taxiDao)
        AppLogger.info("MainViewModel", "INIT", "ViewModel initialized with DB Version 2")
    }

    private fun getUserId(): String = authRepository.getUserId() ?: "anonymous"

    // --- State Streams (Reactive) ---
    
    val shifts = authRepository.observeAuthState().flatMapLatest { user ->
        if (user != null) repository.getAllShifts(user.uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals = authRepository.observeAuthState().flatMapLatest { user ->
        if (user != null) repository.getAllGoals(user.uid)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoal = authRepository.observeAuthState().flatMapLatest { user ->
        if (user != null) {
            TaxiDatabase.getDatabase(getApplication()).taxiDao().getActiveGoalFlow(user.uid)
        } else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs = authRepository.observeAuthState().flatMapLatest { user ->
        if (user != null) {
            TaxiDatabase.getDatabase(getApplication()).logDao().getAllLogs()
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyStats = shifts.map { list ->
        val now = Calendar.getInstance()
        val currentMonthShifts = list.filter {
            val cal = Calendar.getInstance().apply { time = it.date }
            cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && 
            cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }
        
        val totalGross = currentMonthShifts.sumOf { it.grossIncome }
        val totalProfit = currentMonthShifts.sumOf { it.netProfit }
        val fuel = currentMonthShifts.sumOf { it.fuelCost }
        val commissions = currentMonthShifts.sumOf { it.commissions }
        
        MonthlySummary(totalGross, totalProfit, fuel, commissions, currentMonthShifts.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    // --- Actions ---

    fun saveShift(
        gross: Double,
        fuel: Double,
        mileage: Double,
        maintenance: Double,
        fines: Double,
        other: Double,
        goalId: Long?
    ) {
        val currentUserId = getUserId()
        viewModelScope.launch {
            try {
                val comms = gross * 0.18
                val profit = gross - comms - fuel - maintenance - fines - other
                
                val shift = Shift(
                    userId = currentUserId,
                    date = Date(),
                    grossIncome = gross,
                    fuelCost = fuel,
                    mileage = mileage,
                    maintenanceCost = maintenance,
                    fineCost = fines,
                    otherExpenses = other,
                    commissions = comms,
                    netProfit = profit,
                    goalId = goalId
                )
                
                repository.insertShift(shift)
                AppLogger.info("Shift", "DB_INSERT", "Shift saved: +$profit ₸")
                
                goalId?.let { gid ->
                    val goal = goals.value.find { it.id == gid }
                    goal?.let {
                        repository.updateGoal(it.copy(accumulatedAmount = it.accumulatedAmount + profit))
                        AppLogger.info("Goal", "PROGRESS_UPDATE", "Updated goal: ${it.title}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.error("Shift", "DB_SAVE_FAILED", "Failed to save shift", details = AppLogger.getErrorDetails(e))
            }
        }
    }

    fun updateGoalAccumulated(goalId: Long, amount: Double) {
        viewModelScope.launch {
            try {
                val goal = goals.value.find { it.id == goalId }
                goal?.let {
                    repository.updateGoal(it.copy(accumulatedAmount = it.accumulatedAmount + amount))
                    AppLogger.info("Goal", "MANUAL_UPDATE", "Added $amount to goal ${it.title}")
                }
            } catch (e: Exception) {
                AppLogger.error("Goal", "UPDATE_FAILED", AppLogger.getErrorDetails(e))
            }
        }
    }

    fun addGoal(title: String, target: Double) {
        viewModelScope.launch {
            try {
                val goal = Goal(
                    userId = getUserId(),
                    title = title,
                    targetAmount = target,
                    isActive = true
                )
                repository.insertGoal(goal)
                AppLogger.info("Goal", "DB_INSERT", "New goal added: $title")
            } catch (e: Exception) {
                AppLogger.error("Goal", "ADD_FAILED", AppLogger.getErrorDetails(e))
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            TaxiDatabase.getDatabase(getApplication()).logDao().clearAllLogs()
            AppLogger.info("Logs", "CLEAR", "History wiped by user")
        }
    }

    fun deleteLogsByIds(ids: List<Long>) {
        viewModelScope.launch {
            TaxiDatabase.getDatabase(getApplication()).logDao().deleteLogsByIds(ids)
            AppLogger.info("Logs", "BATCH_DELETE", "Deleted ${ids.size} logs")
        }
    }

    // --- Gemini Status (Backend managed) ---
    
    private val geminiPrefs = application.getSharedPreferences("TaxiGoalPrefs", android.content.Context.MODE_PRIVATE)
    
    private val _geminiStatus = MutableStateFlow(loadGeminiStatus())
    val geminiStatus = _geminiStatus.asStateFlow()

    private fun loadGeminiStatus(): GeminiApiStatus {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        return if (user != null) GeminiApiStatus.WORKING else GeminiApiStatus.NOT_CONFIGURED
    }

    fun resetGeminiStatus() {
        _geminiStatus.value = loadGeminiStatus()
    }

    fun deleteGeminiKey() {
        // No-op: Keys are on server now
    }
}

data class MonthlySummary(
    val gross: Double = 0.0,
    val profit: Double = 0.0,
    val fuel: Double = 0.0,
    val commissions: Double = 0.0,
    val count: Int = 0
)
