package com.example.taxigoal.services

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.taxigoal.utils.AppLogger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: List<String>
)

object UpdateManager {

    private const val VERSION_JSON_URL = "https://raw.githubusercontent.com/gabitnur/TaxiGoalApp/main/version.json"

    suspend fun checkUpdate(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            AppLogger.info("Update", "CHECK_START", "Checking update from GitHub: $VERSION_JSON_URL")
            
            val connection = URL(VERSION_JSON_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                AppLogger.info("Update", "HTTP_404", "Version JSON not found (treating as no update)")
                return@withContext Result.success(null)
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(200)
                AppLogger.error("Update", "HTTP_ERROR", "Code: $responseCode | Msg: $errorBody")
                throw Exception("Сервер обновлений недоступен (HTTP $responseCode)")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            
            if (!body.startsWith("{") || !body.contains("versionCode")) {
                AppLogger.error("Update", "INVALID_RESPONSE_FORMAT", "Expected JSON from GitHub, but received: ${body.take(100)}")
                throw Exception("Не удалось получить информацию об обновлении. Проверьте подключение к интернету.")
            }

            val info = try {
                Gson().fromJson(body, UpdateInfo::class.java)
            } catch (e: JsonSyntaxException) {
                AppLogger.error("Update", "GSON_PARSE_FAILED", AppLogger.getErrorDetails(e))
                throw Exception("Ошибка в данных обновления на сервере.")
            }
            
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            if (info.versionCode > currentVersion) {
                AppLogger.info("Update", "AVAILABLE", "New version detected: ${info.versionName}")
                Result.success(info)
            } else {
                AppLogger.info("Update", "UP_TO_DATE", "Current version is latest")
                Result.success(null)
            }
        } catch (e: Exception) {
            val details = AppLogger.getErrorDetails(e)
            AppLogger.error("Update", "CHECK_FAILED", "Exception occurred", details = details)
            Result.failure(e)
        }
    }

    fun startDownload(context: Context, apkUrl: String) {
        AppLogger.info("Update", "DOWNLOAD_INIT", "Starting download: $apkUrl")
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Обновление приложения «МОЙ ДОХОД»")
                .setDescription("Загрузка новой версии с GitHub...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        AppLogger.info("Update", "DOWNLOAD_COMPLETE", "APK downloaded successfully")
                        installApk(ctx, destination)
                        ctx.unregisterReceiver(this)
                    }
                }
            }
            
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(onComplete, filter)
            }
        } catch (e: Exception) {
            AppLogger.error("Update", "DOWNLOAD_ERROR", AppLogger.getErrorDetails(e))
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            AppLogger.info("Update", "INSTALL_LAUNCH", "Opening Package Installer")
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.error("Update", "INSTALL_FAILED", AppLogger.getErrorDetails(e))
        }
    }
}
