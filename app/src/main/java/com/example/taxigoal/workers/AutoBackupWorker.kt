package com.example.taxigoal.workers

import android.content.Context
import androidx.work.*
import com.example.taxigoal.services.BackupManager
import com.example.taxigoal.utils.AppLogger
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.info("AutoBackup", "WORKER_START", "Running scheduled background backup")
        val result = BackupManager.createBackup(context)
        return if (result.isSuccess) {
            AppLogger.info("AutoBackup", "WORKER_SUCCESS", "Background backup completed successfully")
            Result.success()
        } else {
            AppLogger.error("AutoBackup", "WORKER_FAILED", result.exceptionOrNull()?.message ?: "Unknown error")
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "AUTO_BACKUP_PERIODIC_WORK"

        enum class BackupInterval(val hours: Long, val title: String) {
            HOURS_12(12, "Каждые 12 часов"),
            DAYS_7(7 * 24, "Каждые 7 дней"),
            DAYS_14(14 * 24, "Каждые 14 дней"),
            DAYS_30(30 * 24, "Каждые 30 дней"),
            DISABLED(0, "Отключено")
        }

        fun scheduleBackup(context: Context, interval: BackupInterval) {
            val workManager = WorkManager.getInstance(context)

            if (interval == BackupInterval.DISABLED) {
                workManager.cancelUniqueWork(WORK_NAME)
                AppLogger.info("AutoBackup", "CANCELLED", "Scheduled auto-backup disabled")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(interval.hours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request
            )
            AppLogger.info("AutoBackup", "SCHEDULED", "Auto-backup scheduled: ${interval.title}")
        }
    }
}
