package com.example.taxigoal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.taxigoal.data.dao.LogDao
import com.example.taxigoal.data.dao.TaxiDao
import com.example.taxigoal.data.entities.*

@Database(
    entities = [Shift::class, Goal::class, FinancialTransaction::class, ErrorLog::class, AppLog::class, UserProfile::class, Vehicle::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDao(): TaxiDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: TaxiDatabase? = null

        fun getDatabase(context: Context): TaxiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaxiDatabase::class.java,
                    "taxi_income_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
