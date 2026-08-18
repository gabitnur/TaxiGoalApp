package com.example.taxigoal.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "app_logs")
data class AppLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Date = Date(),
    val level: String, // DEBUG, INFO, WARN, ERROR, FATAL
    val screen: String,
    val event: String,
    val title: String,
    val message: String,
    val errorCode: String? = null,
    val sessionId: String,
    val userIdHash: String,
    val details: String? = null
)
