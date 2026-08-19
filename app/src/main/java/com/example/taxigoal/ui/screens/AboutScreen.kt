package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.BuildConfig
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.utils.AppLogger

private const val SCREEN = "AboutScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    AppLogger.screenOpen(SCREEN)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("О приложении") },
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
            Text(text = "💰", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "МОЙ ДОХОД", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(text = "Версия ${BuildConfig.VERSION_NAME}", color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Приложение для профессиональных водителей такси. Позволяет эффективно управлять доходами, расходами и достигать финансовых целей с помощью AI помощника.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Разработчик: Габит Нурмуханбетов", fontSize = 12.sp, color = Color.Gray)
            Text(text = "© 2026 Все права защищены", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.weight(1f))
            
            TextButton(onClick = { /* Privacy Policy URL */ }) {
                Text("Политика конфиденциальности", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
