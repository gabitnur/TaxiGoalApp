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
import com.google.gson.JsonArray
import com.google.gson.JsonObject
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
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/gabitnur/TaxiGoalApp/releases/latest"

    suspend fun checkUpdate(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            AppLogger.info("Update", "CHECK_START", "Checking update from: $VERSION_JSON_URL")
            
            val connection = URL(VERSION_JSON_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                AppLogger.info("Update", "VERSION_JSON_404", "Falling back to GitHub Releases API")
                return@withContext checkUpdateFromGitHubApi(context)
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP Error $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            val info = Gson().fromJson(body, UpdateInfo::class.java)
            
            compareAndReturn(context, info)
        } catch (e: Exception) {
            // If primary source fails, try fallback one more time
            try {
                checkUpdateFromGitHubApi(context)
            } catch (ex: Exception) {
                AppLogger.error("Update", "CHECK_FAILED", e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    private suspend fun checkUpdateFromGitHubApi(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(GITHUB_RELEASES_API).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "TaxiGoalApp")
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.success(null)
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = Gson().fromJson(body, JsonObject::class.java)
            
            val tagName = json.get("tag_name").asString.removePrefix("v")
            val assets = json.getAsJsonArray("assets")
            var apkUrl = ""
            
            for (i in 0 until assets.size()) {
                val asset = assets.get(i).asJsonObject
                if (asset.get("name").asString == "update.apk") {
                    apkUrl = asset.get("browser_download_url").asString
                    break
                }
            }

            if (apkUrl.isEmpty()) return@withContext Result.success(null)

            // Since GitHub API doesn't provide versionCode directly, we estimate it from tag
            // or just rely on versionName comparison. For safety, we'll parse it as a simple Int if possible.
            val estimatedVersionCode = tagName.replace(".", "").toIntOrNull() ?: 0
            
            val info = UpdateInfo(
                versionCode = estimatedVersionCode,
                versionName = tagName,
                apkUrl = apkUrl,
                releaseNotes = listOf(json.get("body").asString.take(200))
            )
            
            compareAndReturn(context, info)
        } catch (e: Exception) {
            Result.success(null)
        }
    }

    private fun compareAndReturn(context: Context, info: UpdateInfo): Result<UpdateInfo?> {
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }

        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode
        }

        return if (info.versionCode > currentVersionCode || info.versionName != pInfo.versionName) {
            AppLogger.info("Update", "AVAILABLE", "New version: ${info.versionName}")
            Result.success(info)
        } else {
            Result.success(null)
        }
    }

    fun startDownload(context: Context, apkUrl: String) {
        AppLogger.info("Update", "DOWNLOAD_INIT", "Starting download: $apkUrl")
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (destination.exists()) destination.delete()

        try {
            val request = DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Обновление приложения «МОЙ ДОХОД»")
                .setDescription("Загрузка новой версии...")
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
