package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.SuccessGreen
import com.example.taxigoal.diagnostic.gemini.GeminiDiagnosticViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiDiagnosticScreen(navController: NavController) {
    val viewModel: GeminiDiagnosticViewModel = viewModel()
    val results by viewModel.results.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Диагностика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            if (!isRunning) {
                FloatingActionButton(onClick = { viewModel.runDiagnostic() }, containerColor = BrandPurple) {
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
            Text("Проверка связи с Firebase Backend", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(24.dp))

            if (isRunning) {
                CircularProgressIndicator(color = BrandPurple)
                Text("Выполняю диагностику...", modifier = Modifier.padding(top = 16.dp))
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
                                Text("Backend Service", fontWeight = FontWeight.Bold)
                                Text("Model: ${res.modelName}", fontSize = 12.sp, color = Color.Gray)
                            }
                            
                            if (res.status == "WORKING") {
                                Text("ОК (${res.duration}ms)", color = SuccessGreen, fontWeight = FontWeight.Bold)
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
