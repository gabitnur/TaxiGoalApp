package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxigoal.diagnostic.gemini.GeminiDiagnosticViewModel
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiDiagnosticScreen(navController: NavController) {
    val viewModel: GeminiDiagnosticViewModel = viewModel()
    val keys by viewModel.testKeys.collectAsState()
    val results by viewModel.results.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val progress by viewModel.progress.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newKeyText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Диагностика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteKeys() }, enabled = !isTesting) {
                        Icon(Icons.Default.Delete, "Удалить все", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            if (!isTesting) {
                FloatingActionButton(onClick = { viewModel.runAllTests() }, containerColor = BrandPurple) {
                    Icon(Icons.Default.PlayArrow, "Старт", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- Keys Section ---
            Text("Тестовые ключи", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newKeyText,
                    onValueChange = { newKeyText = it },
                    placeholder = { Text("Вставьте API Key") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { 
                    if (newKeyText.isNotBlank()) {
                        viewModel.addKey(newKeyText)
                        newKeyText = ""
                    }
                }) {
                    Icon(Icons.Default.Add, null, tint = BrandPurple)
                }
            }
            
            Text("${keys.size} ключей добавлено", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))

            // --- Results Section ---
            if (isTesting) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = BrandPurple
                )
                Text("Проверка комбинаций: ${(progress * 100).toInt()}%", modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { res ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(res.modelName, fontWeight = FontWeight.Bold)
                                Text("Key ID: ${res.keyId}", fontSize = 12.sp, color = Color.Gray)
                            }
                            
                            if (res.status == "WORKING") {
                                Text("${res.duration}ms", color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.setActive(res) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                    Text("ИСПОЛЬЗОВАТЬ", fontSize = 10.sp)
                                }
                            } else {
                                Text("Error: ${res.errorCode}", color = Color.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
