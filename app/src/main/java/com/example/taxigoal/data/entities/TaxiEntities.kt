package com.example.taxigoal.data.entities

import androidx.room.*
import java.util.Date

// 1. Смены
@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String, 
    val date: Date = Date(),
    val grossIncome: Double = 0.0,
    val fuelCost: Double = 0.0,
    val mileage: Double = 0.0,
    val maintenanceCost: Double = 0.0,
    val fineCost: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val commissions: Double = 0.0,
    val netProfit: Double = 0.0,
    val goalId: Long? = null 
)

// 2. Цели
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val accumulatedAmount: Double = 0.0,
    val createdDate: Date = Date(),
    val targetDate: Date? = null,
    val description: String = "",
    val isActive: Boolean = false,
    val iconType: String = "CAR"
)

// 3. Универсальные транзакции
@Entity(tableName = "financial_transactions")
data class FinancialTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val date: Date = Date(),
    val amount: Double,
    val category: String,
    val description: String = "",
    val type: String, // INCOME, EXPENSE
    val source: String // MANUAL, BANK, YANDEX
)

// 4. Лог ошибок
@Entity(tableName = "error_logs")
data class ErrorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String?,
    val timestamp: Date = Date(),
    val level: String, // INFO, WARN, ERROR
    val title: String,
    val message: String,
    val details: String = ""
)

// 5. Профиль пользователя
@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val accountType: String, // GOOGLE, GUEST
    val createdAt: Date = Date(),
    val lastLoginAt: Date = Date()
)

class DateConverters {
    @TypeConverter fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter fun dateToTimestamp(date: Date?): Long? = date?.time
}
