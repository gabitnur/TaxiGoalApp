package com.example.taxigoal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxigoal.R
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.YandexDeductions
import com.example.taxigoal.viewmodel.YandexImportState
import com.example.taxigoal.viewmodel.YandexImportViewModel

private const val SCREEN = "YandexImportScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YandexImportScreen(navController: NavController) {
    val importViewModel: YandexImportViewModel = viewModel()
    val state by importViewModel.state.collectAsState()
    val context = LocalContext.current
    
    var showInfoDialog by remember { mutableStateOf(false) }
    var showExampleDialog by remember { mutableStateOf(false) }
    
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                AppLogger.info(SCREEN, "YANDEX_IMAGE_SELECTED", it.toString())
                importViewModel.processScreenshot(it)
            }
        }
    )

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт из Яндекс Про", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) { 
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instruction Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).background(Color.Black, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Text("📄", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Скриншот вычета заказов", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Сделайте скриншот экрана «Вычеты»", fontSize = 12.sp, color = Color.Black)
                        TextButton(onClick = { showExampleDialog = true }) { 
                            Text("👁 Пример скриншота", color = Color.Black, fontWeight = FontWeight.ExtraBold) 
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (val current = state) {
                is YandexImportState.Idle -> {
                    YandexUploadArea { pickerLauncher.launch("image/*") }
                }
                is YandexImportState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandPurple)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Распознавание данных...", color = Color.Gray)
                        }
                    }
                }
                is YandexImportState.Success -> {
                    YandexResultArea(current.data, onConfirm = { 
                        importViewModel.confirmAndSave(current.data)
                        navController.popBackStack()
                    }, onRetry = { importViewModel.reset() })
                }
                is YandexImportState.Error -> {
                    YandexErrorArea(current.message) { importViewModel.reset() }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Что будет распознано:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            
            val fields = listOf("Комиссия Яндекс", "Сервисный сбор", "Платные опции", "Прочие вычеты")
            fields.forEach { field ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(field)
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("О импорте") },
            text = { Text("Зайдите в Яндекс Про → Доход → Детализация → Вычеты. Сделайте скриншот и загрузите его здесь.") },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Понятно") } }
        )
    }

    if (showExampleDialog) {
        AlertDialog(
            onDismissRequest = { showExampleDialog = false },
            title = { Text("Пример скриншота") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.LightGray, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Интерфейс «Вычеты»\nЯндекс Про", textAlign = TextAlign.Center)
                }
            },
            confirmButton = { TextButton(onClick = { showExampleDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
fun YandexUploadArea(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Выбрать скриншот", fontWeight = FontWeight.Bold)
            Text("PNG, JPG до 10 МБ", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun YandexResultArea(data: YandexDeductions, onConfirm: () -> Unit, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Распознанные вычеты", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            ResultRow("Комиссия", data.commission)
            ResultRow("Сервис", data.serviceFee)
            ResultRow("Опции", data.paidOptions)
            
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ИТОГО", fontWeight = FontWeight.Bold)
                Text("${data.total.toInt()} ₸", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                Text("Сохранить в базу")
            }
            TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Другой файл", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ResultRow(label: String, value: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text("${value.toInt()} ₸", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun YandexErrorArea(msg: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Text("Ошибка анализа", fontWeight = FontWeight.Bold, color = Color.Red)
        Text(msg, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { 
            Icon(Icons.Default.Refresh, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Повторить") 
        }
    }
}
