package com.example.taxigoal.data.local

import androidx.room.*
import java.util.Date
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date = Date(),
    val grossIncome: Double,
    val fuelCost: Double,
    val maintenanceCost: Double = 0.0,
    val fineCost: Double = 0.0,
    val otherExpenses: Double = 0.0,
    val commissions: Double,
    val netProfit: Double,
    val mileage: Double = 0.0
)

@Entity(tableName = "service_costs")
data class ServiceCostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // OIL, WASH, REPAIR, INSURANCE, OTHER
    val amount: Double,
    val currentMileage: Double = 0.0,
    val date: Date = Date(),
    val note: String = ""
)

@Entity(tableName = "fines")
data class FineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val amount: Double,
    val isPaid: Boolean = false,
    val date: Date = Date()
)

class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

@Dao
interface TaxiDao {
    @Query("SELECT * FROM shifts ORDER BY date DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceCost(cost: ServiceCostEntity)
    @Query("SELECT * FROM service_costs ORDER BY date DESC")
    fun getAllServiceCosts(): Flow<List<ServiceCostEntity>>
    @Query("SELECT MAX(currentMileage) FROM service_costs")
    fun getLastMileage(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFine(fine: FineEntity)
    @Query("SELECT * FROM fines ORDER BY date DESC")
    fun getAllFines(): Flow<List<FineEntity>>
}

@Database(entities = [ShiftEntity::class, ServiceCostEntity::class, FineEntity::class], version = 4, exportSchema = false)
@TypeConverters(DateConverters::class)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDao(): TaxiDao

    companion object {
        @Volatile
        private var INSTANCE: TaxiDatabase? = null
        fun getDatabase(context: android.content.Context): TaxiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaxiDatabase::class.java,
                    "taxi_goal_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
