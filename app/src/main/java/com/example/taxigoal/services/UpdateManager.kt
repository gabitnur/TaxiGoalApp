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
            val urlWithCacheBuster = "$BASE_URL?t=${System.currentTimeMillis()}"
            Log.d(TAG, "UpdateManager: Checking update from: $urlWithCacheBuster")
            
            val connection = URL(urlWithCacheBuster).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.d(TAG, "UpdateManager: version.json not found (404), falling back to GitHub API")
                return@withContext checkUpdateFromGitHubApi()
            }

            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "UpdateManager: HTTP Error: $responseCode")
                throw Exception("HTTP Error $responseCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }.trim()
            Log.d(TAG, "UpdateManager: Received JSON: $body")
            
            if (body.isEmpty() || !body.startsWith("{")) {
                throw Exception("Invalid JSON response from server")
            }

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

            if (remoteVersionCode == 0 || remoteApkUrl.isEmpty()) {
                throw Exception("Missing required fields in version.json")
            }

            val info = UpdateInfo(
                versionCode = remoteVersionCode,
                versionName = remoteVersionName,
                apkUrl = remoteApkUrl,
                releaseNotes = notes
            )
            
            compareAndReturn(info)
        } catch (e: Exception) {
            Log.e(TAG, "UpdateManager: checkUpdate failed: ${e.message}")
            // Fallback to GitHub API on any error
            try {
                checkUpdateFromGitHubApi()
            } catch (ex: Exception) {
                AppLogger.error("Update", "CHECK_FAILED", e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }

    private suspend fun checkUpdateFromGitHubApi(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "UpdateManager: Falling back to GitHub Releases API")
            val connection = URL(GITHUB_RELEASES_API).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "TaxiGoalApp")
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "UpdateManager: GitHub API returned ${connection.responseCode}")
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

            if (apkUrl.isEmpty()) {
                Log.d(TAG, "UpdateManager: No update.apk found in GitHub assets")
                return@withContext Result.success(null)
            }

            // Estimate versionCode from tag name (e.g., 1.0.22 -> 22)
            val estimatedVersionCode = tagName.split(".").lastOrNull()?.toIntOrNull() ?: 0
            
            val info = UpdateInfo(
                versionCode = estimatedVersionCode,
                versionName = tagName,
                apkUrl = apkUrl,
                releaseNotes = listOf(json.optString("body", "").take(200))
            )
            
            compareAndReturn(info)
        } catch (e: Exception) {
            Log.e(TAG, "UpdateManager: GitHub API fallback failed: ${e.message}")
            Result.success(null)
        }
    }

    private fun compareAndReturn(info: UpdateInfo): Result<UpdateInfo?> {
        val currentVersionCode = BuildConfig.VERSION_CODE
        Log.d(TAG, "UpdateManager: currentVersionCode=$currentVersionCode")
        Log.d(TAG, "UpdateManager: remoteVersionCode=${info.versionCode}")
        
        val updateAvailable = info.versionCode > currentVersionCode
        Log.d(TAG, "UpdateManager: updateAvailable=$updateAvailable")

        return if (updateAvailable) {
            Log.i(TAG, "UpdateManager: Update AVAILABLE: v${info.versionName}")
            AppLogger.info("Update", "AVAILABLE", "New version: ${info.versionName}")
            Result.success(info)
        } else {
            Log.d(TAG, "UpdateManager: App is up to date")
            Result.success(null)
        }
    }

    fun startDownload(context: Context, apkUrl: String) {
        Log.d(TAG, "UpdateManager: Starting download: $apkUrl")
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
            Log.d(TAG, "UpdateManager: downloadId=$downloadId")

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id == downloadId) {
                        Log.d(TAG, "UpdateManager: downloadStatus=SUCCESSFUL")
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
            Log.e(TAG, "UpdateManager: Download failed: ${e.message}")
            AppLogger.error("Update", "DOWNLOAD_ERROR", AppLogger.getErrorDetails(e))
        }
    }

    private fun installApk(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            Log.d(TAG, "UpdateManager: apkUri=$uri")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Log.d(TAG, "UpdateManager: Launching Package Installer")
            AppLogger.info("Update", "INSTALL_LAUNCH", "Opening Package Installer")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "UpdateManager: Installation failed: ${e.message}")
            AppLogger.error("Update", "INSTALL_FAILED", AppLogger.getErrorDetails(e))
        }
    }
}
