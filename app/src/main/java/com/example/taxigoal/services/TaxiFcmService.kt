package com.example.taxigoal.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.taxigoal.MainActivity
import com.example.taxigoal.R
import com.example.taxigoal.utils.AppLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TaxiFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        AppLogger.info("FCM", "MESSAGE_RECEIVED", "From: ${message.from}")
        
        message.notification?.let {
            showNotification(it.title ?: "Обновление", it.body ?: "Доступна новая версия")
        }
    }

    override fun onNewToken(token: String) {
        AppLogger.info("FCM", "NEW_TOKEN", "Token received")
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "updates"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Updates", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        @Suppress("MissingPermission")
        notificationManager.notify(100, notification)
    }
}
