package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.data.entities.Shift
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.ProfitGreen
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val SCREEN = "HistoryScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, viewModel: MainViewModel) {
    val shifts by viewModel.shifts.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История смен", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        if (shifts.isEmpty()) {
            Box(Modifier.fillMaxSize().background(AppBackground).padding(padding), contentAlignment = Alignment.Center) {
                Text("У вас пока нет сохраненных смен", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shifts) { shift ->
                    ShiftHistoryCard(shift, dateFormat) {
                        AppLogger.buttonClick(SCREEN, "BTN_SHIFT_DETAILS_${shift.id}")
                        // Open details or edit
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryCard(shift: Shift, dateFormat: SimpleDateFormat, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(shift.date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    HistoryStat(label = "Выручка", value = "${shift.grossIncome.toInt()} ₸")
                    Spacer(modifier = Modifier.width(16.dp))
                    HistoryStat(label = "Расходы", value = "${(shift.grossIncome - shift.netProfit).toInt()} ₸")
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${shift.netProfit.toInt()} ₸",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ProfitGreen
                )
                Text(text = "чистая прибыль", fontSize = 10.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun HistoryStat(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
}
