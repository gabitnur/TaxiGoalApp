package com.example.taxigoal.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxigoal.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class GoalInfo(
    val target: Double,
    val accumulated: Double,
    val remaining: Double,
    val progress: Float
)

class TaxiViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TaxiDatabase.getDatabase(application).taxiDao()
    private val prefs = application.getSharedPreferences("TaxiGoalPrefs", Context.MODE_PRIVATE)

    // Goal Target
    private val _goalTarget = MutableStateFlow(prefs.getFloat("goal_target", 500000f).toDouble())
    val goalTarget = _goalTarget.asStateFlow()

    fun updateGoalTarget(newTarget: Double) {
        _goalTarget.value = newTarget
        prefs.edit().putFloat("goal_target", newTarget.toFloat()).apply()
    }

    // Data Streams
    val allShifts = dao.getAllShifts()
    val allServiceCosts = dao.getAllServiceCosts()
    val allStandaloneFines = dao.getAllFines()

    // Calculated Goal Info (Integration with all expenses)
    val goalInfo: StateFlow<GoalInfo> = combine(
        goalTarget, allShifts, allServiceCosts, allStandaloneFines
    ) { target, shifts, serviceCosts, fines ->
        // 1. Плюсы от смен (netProfit уже за вычетом расходов внутри смены)
        val shiftAccumulated = shifts.sumOf { it.netProfit }
        
        // 2. Минусы от ОТДЕЛЬНО записанных расходов (не в отчете смены)
        val standaloneServiceExpenses = serviceCosts.sumOf { it.amount }
        val standaloneFineExpenses = fines.sumOf { it.amount }
        
        val totalAccumulated = shiftAccumulated - standaloneServiceExpenses - standaloneFineExpenses
        
        val remaining = (target - totalAccumulated).coerceAtLeast(0.0)
        val progress = if (target > 0) (totalAccumulated / target).toFloat().coerceIn(0f, 1f) else 0f
        
        GoalInfo(target, totalAccumulated, remaining, progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalInfo(500000.0, 0.0, 500000.0, 0f))

    // Actions
    fun saveShiftWithDetails(
        gross: Double,
        fuel: Double,
        mileage: Double,
        maintenance: Double,
        fines: Double,
        other: Double
    ) {
        viewModelScope.launch {
            val commissions = gross * 0.21
            val net = gross - commissions - fuel - maintenance - fines - other
            
            val shift = ShiftEntity(
                grossIncome = gross,
                fuelCost = fuel,
                mileage = mileage,
                maintenanceCost = maintenance,
                fineCost = fines,
                otherExpenses = other,
                commissions = commissions,
                netProfit = net,
                date = Date()
            )
            dao.insertShift(shift)
        }
    }

    fun addServiceCost(type: String, amount: Double, mileage: Double = 0.0, note: String = "") {
        viewModelScope.launch {
            dao.insertServiceCost(ServiceCostEntity(type = type, amount = amount, currentMileage = mileage, note = note))
        }
    }

    fun recordStandaloneFine(type: String, amount: Double) {
        viewModelScope.launch {
            dao.insertFine(FineEntity(type = type, amount = amount, isPaid = true))
        }
    }

    val lastMileage = dao.getLastMileage().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Fines Search (Mock)
    private val _finesSearchList = MutableStateFlow<List<Fine>>(emptyList())
    val finesSearchList = _finesSearchList.asStateFlow()

    fun searchFines(iin: String, grnz: String) {
        viewModelScope.launch {
            _finesSearchList.value = listOf(
                Fine("1", "Превышение скорости", 18460.0, 5),
                Fine("2", "Разметка", 11076.0, 14)
            )
        }
    }
}

data class Fine(val id: String, val type: String, val amount: Double, val daysLeft: Int)
