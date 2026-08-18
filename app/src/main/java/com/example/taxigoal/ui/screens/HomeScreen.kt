package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.data.entities.Shift
import com.example.taxigoal.ui.components.GeminiIcon
import com.example.taxigoal.ui.components.GoalCard
import com.example.taxigoal.ui.components.MetricItem
import com.example.taxigoal.ui.components.QuickActionCard
import com.example.taxigoal.ui.navigation.Screen
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

private const val SCREEN = "HomeScreen"

@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {
    val activeGoal by viewModel.activeGoal.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val lastShift = shifts.firstOrNull()
    
    val user = FirebaseAuth.getInstance().currentUser
    val userName = remember(user) {
        if (user?.isAnonymous == true) "Гость"
        else user?.displayName?.substringBefore(" ") ?: "Пользователь"
    }

    LaunchedEffect(Unit) {
        AppLogger.screenOpen(SCREEN)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Привет, $userName! 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Хорошего дня и прибыльных смен!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { AppLogger.buttonClick(SCREEN, "BTN_NOTIFICATIONS") },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Active Goal ---
        activeGoal?.let { goal ->
            GoalCard(
                goal = goal,
                onEditClick = { 
                    AppLogger.buttonClick(SCREEN, "BTN_EDIT_GOAL")
                    navController.navigate("goal_details/${goal.id}") 
                }
            )
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .clickable { 
                        AppLogger.buttonClick(SCREEN, "BTN_CREATE_GOAL_PROMPT")
                        navController.navigate(Screen.Goals.route) 
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("У вас пока нет активной цели", color = Color.Gray, fontSize = 14.sp)
                    Text("Нажмите, чтобы создать 🎯", fontWeight = FontWeight.Bold, color = BrandPurple)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Core Action Grid ---
        Text("Добавить данные", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Смена",
                subtitle = "Вручную",
                icon = Icons.Default.Add,
                iconColor = BrandPurple,
                onClick = { navController.navigate(Screen.Shift.route) },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Яндекс Про",
                subtitle = "Скриншот",
                icon = Icons.Default.TaxiAlert,
                iconColor = YandexYellow,
                onClick = { navController.navigate(Screen.YandexImport.route) },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "Импорт банка",
                subtitle = "Выписка PDF",
                icon = Icons.Default.AccountBalance,
                iconColor = SuccessGreen,
                onClick = { navController.navigate(Screen.BankImport.route) },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Gemini AI",
                subtitle = "Помощник",
                icon = Icons.Default.AutoAwesome,
                iconColor = Color(0xFF9B72CB),
                onClick = { navController.navigate(Screen.AIChat.route) },
                modifier = Modifier.weight(1f),
                customIcon = { GeminiIcon(size = 24.dp) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Monthly Snapshot ---
        Text("Статистика за месяц", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Чистая прибыль", fontSize = 12.sp, color = Color.Gray)
                        Text("${monthlyStats.profit.toInt()} ₸", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Box(Modifier.size(40.dp).background(BrandPurpleLight, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TrendingUp, null, tint = BrandPurple)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem("Выручка", "${monthlyStats.gross.toInt()} ₸", Color.Gray)
                    MetricItem("Смены", "${monthlyStats.count}", Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Последняя смена", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))

        lastShift?.let { shift ->
            ShiftSummaryCard(shift) {
                AppLogger.buttonClick(SCREEN, "BTN_LAST_SHIFT_DETAILS")
                navController.navigate(Screen.History.route)
            }
        } ?: Text("Записей пока нет", color = Color.Gray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ShiftSummaryCard(shift: Shift, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val cal = Calendar.getInstance().apply { time = shift.date }
                    Text(text = cal.get(Calendar.DAY_OF_MONTH).toString(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    Text(text = SimpleDateFormat("MMM", Locale("ru")).format(shift.date), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = dateFormat.format(shift.date), fontSize = 12.sp, color = Color.Gray)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem("Чистая", "${shift.netProfit.toInt()} ₸", SuccessGreen)
                    MetricItem("Выручка", "${shift.grossIncome.toInt()} ₸", Color.Gray)
                }
            }
        }
    }
}
