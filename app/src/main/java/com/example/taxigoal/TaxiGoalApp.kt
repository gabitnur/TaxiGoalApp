package com.example.taxigoal

import android.app.Application
import com.example.taxigoal.utils.AppLogger

class TaxiGoalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. Mandatory Logger Initialization
        AppLogger.init(this)
        AppLogger.info("SYSTEM", "APP_LAUNCH", "My Income version 1.0.17 started")
        
        // 2. Global Crash Monitoring
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val details = AppLogger.getErrorDetails(throwable)
                AppLogger.fatal("CRASH", "UNCAUGHT", "Thread: ${thread.name}", details = details)
            } catch (e: Exception) { /* Fail-safe */ }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // 3. Initialize Services
        try {
            FirebaseSyncManager.init(this)
            CommissionManager.init()
            scheduleBackgroundTasks()
        } catch (e: Exception) {
            AppLogger.error("SYSTEM", "SERVICE_INIT_FAIL", "Failed to start core services", details = AppLogger.getErrorDetails(e))
        }
    }

    private fun scheduleBackgroundTasks() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        
        // 1. Daily Update Check
        val updateRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.taxigoal.workers.UpdateCheckWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()
        
        workManager.enqueueUniquePeriodicWork(
            "DAILY_UPDATE_CHECK",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )

        // 2. Periodic Auto-Backup (example: every 24h)
        // This is usually configured by user in UI, but we can set a default here or in UI.
        // com.example.taxigoal.workers.AutoBackupWorker.scheduleBackup(this, com.example.taxigoal.workers.AutoBackupWorker.BackupInterval.HOURS_12)
    }
}
