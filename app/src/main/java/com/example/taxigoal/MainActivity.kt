package com.example.taxigoal

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taxigoal.services.UpdateInfo
import com.example.taxigoal.services.UpdateManager
import com.example.taxigoal.ui.navigation.TaxiGoalNavigation
import com.example.taxigoal.ui.theme.MyIncomeTheme
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.utils.PreferenceManager

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AppLogger.info("MainActivity", "NOTIF_PERM", "POST_NOTIFICATIONS granted")
        } else {
            AppLogger.warn("MainActivity", "NOTIF_PERM", "POST_NOTIFICATIONS denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferenceManager = PreferenceManager(this)
        
        // Request Notification Permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by preferenceManager.themeFlow.collectAsState(initial = "SYSTEM")
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            
            LaunchedEffect(Unit) {
                // Check if launched from Update Notification
                val checkUpdateNow = intent.getBooleanExtra("CHECK_UPDATE", false)
                
                // Фоновая проверка обновлений при старте
                UpdateManager.checkUpdate(this@MainActivity).onSuccess { info ->
                    if (info != null) {
                        updateInfo = info
                    } else if (checkUpdateNow) {
                        // User clicked notification but maybe version already up to date?
                        // Just in case, we can show a toast or something.
                    }
                }
            }

            val isDark = when(themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyIncomeTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaxiGoalNavigation()
                    
                    updateInfo?.let { info ->
                        UpdateDialog(
                            info = info,
                            onDismiss = { updateInfo = null },
                            onUpdate = {
                                UpdateManager.startDownload(this@MainActivity, info.apkUrl)
                                updateInfo = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateDialog(info: UpdateInfo, onDismiss: () -> Unit, onUpdate: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Доступно обновление v${info.versionName}") },
        text = {
            Column {
                Text("Что нового:")
                Spacer(modifier = Modifier.height(8.dp))
                info.releaseNotes.forEach { note ->
                    Text("• $note", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text("Обновить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже")
            }
        }
    )
}
