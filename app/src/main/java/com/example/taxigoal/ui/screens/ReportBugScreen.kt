package com.example.taxigoal.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBugScreen(navController: NavController, mainViewModel: MainViewModel) {
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var attachLogs by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var reportId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val categories = listOf("UPDATE", "GEMINI", "NETWORK", "DATABASE", "AUTH", "BACKUP", "OCR", "UI", "OTHER")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Сообщить об ошибке", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (reportId == null) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Что произошло?") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Категория", modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold)
                
                categories.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = attachLogs, onCheckedChange = { attachLogs = it })
                    Text("Прикрепить технический журнал", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            // TODO: Implement real submission via mainViewModel and Firebase Functions
                            kotlinx.coroutines.delay(2000)
                            reportId = "BUG-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = description.isNotBlank() && !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Отправить отчёт", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Отчёт содержит техническую информацию о приложении и устройстве. Секреты и личные данные не отправляются.",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            } else {
                Text("✅ Отчёт отправлен", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Report ID:", color = Color.Gray)
                Text(reportId!!, fontWeight = FontWeight.Black, fontSize = 24.sp, color = BrandPurple)
                
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Вернуться")
                }
            }
        }
    }
}
