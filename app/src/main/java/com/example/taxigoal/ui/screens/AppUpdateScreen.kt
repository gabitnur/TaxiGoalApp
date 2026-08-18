package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import com.example.taxigoal.viewmodel.UpdateState
import com.example.taxigoal.viewmodel.UpdateViewModel

private const val SCREEN = "AppUpdateScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUpdateScreen(navController: NavController, mainViewModel: MainViewModel) {
    val updateViewModel: UpdateViewModel = viewModel()
    val state by updateViewModel.state.collectAsState()
    
    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Обновление", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.SystemUpdate, null, modifier = Modifier.size(80.dp), tint = BrandPurple)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "МОЙ ДОХОД",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(text = "Текущая версия 1.0.16", color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(48.dp))

            when (val current = state) {
                is UpdateState.Idle -> {
                    Button(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_CHECK_UPDATE")
                            updateViewModel.checkForUpdate() 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
                    ) {
                        Text("Проверить обновления", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateState.Checking -> {
                    CircularProgressIndicator(color = BrandPurple)
                    Text("Поиск новой версии...", modifier = Modifier.padding(top = 16.dp))
                }
                is UpdateState.Available -> {
                    Text("🚀 Доступно обновление!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Версия: ${current.info.versionName}", modifier = Modifier.padding(top = 8.dp))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Что улучшено:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            current.info.releaseNotes.forEach { note ->
                                Text("• $note", fontSize = 13.sp, color = Color.DarkGray, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_START_UPDATE")
                            updateViewModel.startUpdate(current.info) 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Обновить сейчас", fontWeight = FontWeight.Bold)
                    }
                }
                is UpdateState.UpToDate -> {
                    Text("🎉 У вас установлена актуальная версия")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { updateViewModel.checkForUpdate() },
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Проверить снова") 
                    }
                }
                is UpdateState.Error -> {
                    Text(
                        text = current.message, 
                        color = Color.Red, 
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { updateViewModel.checkForUpdate() },
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Повторить") 
                    }
                }
            }
        }
    }
}
