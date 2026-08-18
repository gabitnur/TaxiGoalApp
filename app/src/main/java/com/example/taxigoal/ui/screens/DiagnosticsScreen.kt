package com.example.taxigoal.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.taxigoal.ui.navigation.Screen
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val SCREEN = "DiagnosticsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val geminiStatus by viewModel.geminiStatus.collectAsState()

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диагностика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("🛠 Системная информация", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    DiagRow("Версия приложения", "1.0.17")
                    DiagRow("Build", "17")
                    DiagRow("Android", Build.VERSION.RELEASE)
                    DiagRow("Модель", "${Build.MANUFACTURER} ${Build.MODEL}")
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = AppBackground)
                    DiagRow("Firebase Auth", if (user != null) "🟢 OK" else "🔴 No user")
                    DiagRow("Account Type", if (user?.isAnonymous == true) "Guest" else "Google")
                    DiagRow("Gemini Status", geminiStatus.name)
                    DiagRow("UID", user?.uid?.take(8) ?: "N/A")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { exportDiagnosticLog(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Text("📤 Отправить лог разработчику", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { viewModel.clearLogs() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Очистить локальные логи")
            }
            
            Button(
                onClick = { navController.navigate(Screen.GeminiDiagnostic.route) },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("🧪 ТЕСТ МОДЕЛЕЙ GEMINI", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black)
    }
}

private fun exportDiagnosticLog(context: Context) {
    val sb = StringBuilder()
    sb.append("================================================\n")
    sb.append("MY INCOME — DIAGNOSTIC LOG\n")
    sb.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
    sb.append("App Version: 1.0.17\n")
    sb.append("Android: ${Build.VERSION.RELEASE}\n")
    sb.append("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
    sb.append("================================================\n\n")
    
    val logFile = File(context.filesDir, "app_debug_logs.txt")
    if (logFile.exists()) {
        sb.append(logFile.readText())
    } else {
        sb.append("Local debug log is empty.\n")
    }
    
    val exportFile = File(context.cacheDir, "diagnostic_log.txt")
    exportFile.writeText(sb.toString())
    
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить диагностический лог"))
}
