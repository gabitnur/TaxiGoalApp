package com.example.taxigoal.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.taxigoal.MainActivity
import com.example.taxigoal.R
import com.example.taxigoal.services.UpdateManager
import com.example.taxigoal.utils.AppLogger

class UpdateCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        AppLogger.info("UpdateWorker", "WORKER_START", "Running background update check")
        val result = UpdateManager.checkUpdate(context)
        
        result.onSuccess { info ->
            if (info != null) {
                AppLogger.info("UpdateWorker", "UPDATE_FOUND", "v${info.versionName}")
                showNotification(info.versionName)
            } else {
                AppLogger.info("UpdateWorker", "UP_TO_DATE", "No update found")
            }
        }.onFailure { e ->
            AppLogger.error("UpdateWorker", "WORKER_ERROR", e.message ?: "Unknown error")
        }

        return Result.success()
    }

    private fun showNotification(versionName: String) {
        val channelId = "app_updates_channel"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Обновления приложения"
            val descriptionText = "Уведомления о новых версиях приложения"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CHECK_UPDATE", true)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Доступна новая версия МОЙ ДОХОД v$versionName")
            .setContentText("Нажмите, чтобы скачать и установить обновление")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                @Suppress("MissingPermission")
                notify(notificationId, builder.build())
            } catch (e: Exception) {
                AppLogger.error("UpdateWorker", "NOTIFY_FAIL", e.message ?: "")
            }
        }
    }
}
