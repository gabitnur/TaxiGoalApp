package com.example.taxigoal.services

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.taxigoal.BuildConfig
import com.example.taxigoal.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "UpdateManager"

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: List<String>
)

object UpdateManager {

    private const val BASE_URL = "https://raw.githubusercontent.com/gabitnur/TaxiGoalApp/main/version.json"
    private const val GITHUB_RELEASES_API = "https://api.github.com/repos/gabitnur/TaxiGoalApp/releases/latest"

    suspend fun checkUpdate(context: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            // 1. URL с анти-кэшем
            val urlWithCacheBuster = "$BASE_URL?t=${System.currentTimeMillis()}"
            Log.d(TAG, "Checking update from: $urlWithCacheBuster")
            
            val connection = URL(urlWithCacheBuster).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "version.json not found (404), falling back to GitHub API")
                return@withContext checkUpdateFromGitHubApi(context)
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP Error: $responseCode")
                throw Exception("HTTP $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            Log.d(TAG, "Received JSON: $body")
            
            // 2. Безопасный парсинг JSON через JSONObject
            val json = JSONObject(body)
            val remoteVersionCode = json.optInt("versionCode", 0)
            val remoteVersionName = json.optString("versionName", "Unknown")
            val remoteApkUrl = json.optString("apkUrl", json.optString("downloadUrl", ""))
            
            val notes = mutableListOf<String>()
            val notesArray = json.optJSONArray("releaseNotes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notes.add(notesArray.getString(i))
                }
            }

            val info = UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                apkUrl = remoteApkUrl,
                releaseNotes = notes
            )
            
            // 3. Сравнение версий по versionCode
            compareAndReturn(info)
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}")
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
            val json = JSONObject(body)
            
            val tagName = json.optString("tag_name", "").removePrefix("v")
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name") == "update.apk") {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (apkUrl.isEmpty()) return@withContext Result.success(null)

            val estimatedVersionCode = tagName.replace(".", "").toIntOrNull() ?: 0
            
            val info = UpdateInfo(
                versionCode = estimatedVersionCode,
                versionName = tagName,
                apkUrl = apkUrl,
                releaseNotes = listOf(json.optString("body", "").take(200))
            )
            
            compareAndReturn(info)
        } catch (e: Exception) {
            Log.e(TAG, "GitHub API fallback failed: ${e.message}")
            Result.success(null)
        }
    }

    private fun compareAndReturn(info: UpdateInfo): Result<UpdateInfo?> {
        val currentVersionCode = BuildConfig.VERSION_CODE
        Log.d(TAG, "Comparing: Remote VC=$${info.versionCode}, Local VC=$currentVersionCode")

        return if (info.versionCode > currentVersionCode) {
            Log.i(TAG, "Update AVAILABLE: v${info.versionName}")
            Result.success(info)
        } else {
            Log.d(TAG, "App is up to date")
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
            context.startActivity(intent)
        } catch (e: Exception) {
            AppLogger.error("Update", "INSTALL_FAILED", AppLogger.getErrorDetails(e))
        }
    }
}
