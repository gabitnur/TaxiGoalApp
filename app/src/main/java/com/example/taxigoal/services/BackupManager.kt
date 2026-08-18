package com.example.taxigoal.services

import android.content.Context
import com.example.taxigoal.data.database.TaxiDatabase
import com.example.taxigoal.data.entities.FinancialTransaction
import com.example.taxigoal.data.entities.Goal
import com.example.taxigoal.data.entities.Shift
import com.example.taxigoal.utils.AppLogger
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Полный payload со всеми данными пользователя из Room DB
data class UserBackupPayload(
    val uid: String,
    val timestamp: Long,
    val appVersion: String,
    val info: String = "User backup data",
    val shifts: List<Shift> = emptyList(),
    val transactions: List<FinancialTransaction> = emptyList(),
    val goals: List<Goal> = emptyList()
)

data class BackupFileInfo(
    val id: String,
    val name: String,
    val formattedDate: String,
    val createdTimeMs: Long
)

object BackupManager {

    private const val FOLDER_NAME = "MY_INCOME_BACKUPS"
    private const val MIME_TYPE_JSON = "application/json"

    // 1. Создание бэкапа с выгрузкой всех записей из Room DB
    suspend fun createBackup(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            AppLogger.info("Backup", "BACKUP_START", "Starting user data backup to Personal Drive")
            
            val driveService = getDriveService(context) ?: return@withContext Result.failure(
                Exception("Не удалось получить доступ к Google Drive. Выполните повторный вход в Google.")
            )
            
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                Exception("Пользователь не авторизован (UID отсутствует)")
            )

            // Чтение реальных данных пользователя из Room Database
            val db = TaxiDatabase.getDatabase(context)
            val userShifts = db.taxiDao().getAllShiftsSync()
            val userTransactions = db.taxiDao().getAllTransactionsSync()
            val userGoals = db.taxiDao().getAllGoalsSync()

            val payload = UserBackupPayload(
                uid = uid,
                timestamp = System.currentTimeMillis(),
                appVersion = "1.0.17",
                info = "User backup data",
                shifts = userShifts,
                transactions = userTransactions,
                goals = userGoals
            )
            
            val json = Gson().toJson(payload)
            val folderId = getOrCreateFolder(driveService)
            
            val fileMetadata = File().apply {
                name = "backup_${SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())}.json"
                parents = listOf(folderId)
            }
            
            val mediaContent = InputStreamContent(MIME_TYPE_JSON, ByteArrayInputStream(json.toByteArray()))
            driveService.files().create(fileMetadata, mediaContent).setFields("id").execute()
            
            AppLogger.info(
                "Backup", 
                "BACKUP_SUCCESS", 
                "Backup created: ${userShifts.size} shifts, ${userTransactions.size} transactions, ${userGoals.size} goals"
            )
            Result.success(Unit)

        } catch (e: UserRecoverableAuthIOException) {
            AppLogger.warn("Backup", "AUTH_RECOVERABLE", "User consent required for Drive API")
            Result.failure(e)

        } catch (e: Exception) {
            val errorDetails = e.localizedMessage ?: e.message ?: e.cause?.message ?: e.javaClass.simpleName
            AppLogger.error("Backup", "BACKUP_FAILED", "Exception during backup: $errorDetails", details = AppLogger.getErrorDetails(e))
            Result.failure(Exception("Ошибка резервного копирования: $errorDetails"))
        }
    }

    // 2. Извлечение списка всех файлов бэкапов с форматированием дат
    suspend fun getBackupFilesList(context: Context): Result<List<BackupFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context) ?: return@withContext Result.failure(
                Exception("Авторизуйтесь в Google для просмотра резервных копий.")
            )

            val folderId = getOrCreateFolder(driveService)
            val queryFiles = "'$folderId' in parents and mimeType = '$MIME_TYPE_JSON' and trashed = false"
            
            val filesResult = driveService.files().list()
                .setQ(queryFiles)
                .setOrderBy("createdTime desc")
                .setFields("files(id, name, createdTime)")
                .execute()

            val filesList = filesResult.files?.map { file ->
                val timestamp = file.createdTime?.value ?: 0L
                val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru")).format(Date(timestamp))
                BackupFileInfo(
                    id = file.id,
                    name = file.name,
                    formattedDate = dateStr,
                    createdTimeMs = timestamp
                )
            } ?: emptyList()

            AppLogger.info("Backup", "LIST_FETCHED", "Found backups count: ${filesList.size}")
            Result.success(filesList)

        } catch (e: UserRecoverableAuthIOException) {
            AppLogger.warn("Backup", "AUTH_RECOVERABLE", "User consent required for Drive API")
            Result.failure(e)
        } catch (e: Exception) {
            val details = AppLogger.getErrorDetails(e)
            AppLogger.error("Backup", "LIST_FAILED", "Error fetching list", details = details)
            Result.failure(e)
        }
    }

    // 3. Восстановление данных из выбранного JSON-файла в Room DB
    suspend fun restoreBackupById(context: Context, fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            AppLogger.info("Restore", "RESTORE_BY_ID_START", "Restoring file ID: $fileId")

            val driveService = getDriveService(context) ?: return@withContext Result.failure(
                Exception("Авторизуйтесь в Google для восстановления.")
            )

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val jsonContent = outputStream.toString("UTF-8")

            val backupPayload = Gson().fromJson(jsonContent, UserBackupPayload::class.java)

            if (backupPayload != null) {
                val db = TaxiDatabase.getDatabase(context)

                if (backupPayload.shifts.isNotEmpty()) {
                    db.taxiDao().insertAllShifts(backupPayload.shifts)
                }
                if (backupPayload.transactions.isNotEmpty()) {
                    db.taxiDao().insertAllTransactions(backupPayload.transactions)
                }
                if (backupPayload.goals.isNotEmpty()) {
                    db.taxiDao().insertAllGoals(backupPayload.goals)
                }

                AppLogger.info(
                    "Restore", 
                    "RESTORE_SUCCESS", 
                    "Restored: ${backupPayload.shifts.size} shifts, ${backupPayload.transactions.size} transactions"
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            val details = AppLogger.getErrorDetails(e)
            AppLogger.error("Restore", "RESTORE_FAILED", "Restore error for file $fileId", details = details)
            Result.failure(Exception("Ошибка восстановления: ${e.localizedMessage ?: "сбой чтения"}"))
        }
    }

    private fun getDriveService(context: Context): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account.account
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("МОЙ ДОХОД").build()
    }

    private fun getOrCreateFolder(service: Drive): String {
        val query = "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val result = service.files().list().setQ(query).setSpaces("drive").execute()
        val folders = result.files
        
        if (!folders.isNullOrEmpty()) {
            return folders[0].id
        }
        
        AppLogger.info("Backup", "FOLDER_CREATE", "Creating folder: $FOLDER_NAME")
        val metadata = File().apply {
            name = FOLDER_NAME
            mimeType = "application/vnd.google-apps.folder"
        }
        return service.files().create(metadata).setFields("id").execute().id
    }
}
