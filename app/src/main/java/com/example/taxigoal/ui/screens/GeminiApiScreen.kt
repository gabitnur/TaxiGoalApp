package com.example.taxigoal.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.ui.components.GeminiIcon
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.GeminiApiStatus
import com.example.taxigoal.viewmodel.MainViewModel

private const val SCREEN = "GeminiApiScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiApiScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("TaxiGoalPrefs", android.content.Context.MODE_PRIVATE) }
    
    var key by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var visible by remember { mutableStateOf(false) }
    
    val status by viewModel.geminiStatus.collectAsState()
    val isVerifying by viewModel.isGeminiVerifying.collectAsState()
    val verifyError by viewModel.geminiError.collectAsState()
    
    val workingModel = remember(status) { prefs.getString("working_gemini_model", "gemini-1.5-flash") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Настройка Gemini AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GeminiIcon(size = 64.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Искусственный интеллект",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "По умолчанию используется встроенный ключ. Вы можете указать собственный ключ для использования личной квоты.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- API Key Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Ваш персональный API Key (необязательно)", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = key,
                    onValueChange = { 
                        key = it
                        viewModel.resetGeminiStatus()
                    },
                    placeholder = { Text("Вставьте ваш ключ Google AI") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isVerifying,
                    isError = status == GeminiApiStatus.ERROR
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Показываем статус (если ключ не введен, пишем про встроенный)
                if (key.isBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🟢 Используется встроенный ключ", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 14.sp)
                    }
                } else {
                    StatusIndicator(status, workingModel, verifyError)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_VERIFY_GEMINI")
                            viewModel.verifyAndSaveGeminiKey(key) { success ->
                                if (success) Toast.makeText(context, "Ключ успешно проверен", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        enabled = key.isNotBlank() && !isVerifying
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Проверить мой ключ", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (key.isNotBlank()) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.deleteGeminiKey()
                                key = "" 
                            },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(0.3f))
                        ) {
                            Icon(Icons.Default.DeleteOutline, null)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(
            onClick = { /* Link to AI Studio */ }, 
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Как получить свой API ключ бесплатно? ↗", color = BrandPurple, fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun StatusIndicator(status: GeminiApiStatus, model: String?, error: String?) {
    val (color, text) = when(status) {
        GeminiApiStatus.WORKING -> SuccessGreen to "🟢 Ваш API ключ работает"
        GeminiApiStatus.ERROR -> Color.Red to "🔴 Ваш API ключ недействителен"
        GeminiApiStatus.CHECKING -> WarningOrange to "🟡 Выполняется проверка..."
        GeminiApiStatus.NOT_CHECKED -> Color.Gray to "⚪ Ключ изменен, требуется проверка"
        GeminiApiStatus.NOT_CONFIGURED -> SuccessGreen to "🟢 Используется встроенный ключ"
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
        
        if (status == GeminiApiStatus.WORKING) {
            Text("Активная модель: $model", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
        
        if (status == GeminiApiStatus.ERROR && error != null) {
            Text("Ошибка: $error", fontSize = 12.sp, color = Color.Red.copy(0.8f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}
