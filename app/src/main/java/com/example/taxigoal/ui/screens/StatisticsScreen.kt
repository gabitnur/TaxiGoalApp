package com.example.taxigoal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import com.example.taxigoal.viewmodel.MonthlySummary
import java.util.*

private const val SCREEN = "StatisticsScreen"

@Composable
fun StatisticsScreen(viewModel: MainViewModel) {
    val shifts by viewModel.shifts.collectAsState()
    val monthlySummary by viewModel.monthlyStats.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf("Месяц") }
    var showPeriodMenu by remember { mutableStateOf(false) }
    val periods = listOf("День", "Неделя", "Месяц", "Год")

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Статистика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            
            Box {
                AssistChip(
                    onClick = { showPeriodMenu = true },
                    label = { Text(selectedPeriod) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                )
                DropdownMenu(
                    expanded = showPeriodMenu,
                    onDismissRequest = { showPeriodMenu = false }
                ) {
                    periods.forEach { period ->
                        DropdownMenuItem(
                            text = { Text(period) },
                            onClick = {
                                selectedPeriod = period
                                showPeriodMenu = false
                                AppLogger.info(SCREEN, "PERIOD_CHANGED", period)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Profit Chart Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Чистая прибыль", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(text = "${monthlySummary.profit.toInt()} ₸", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ProfitLineChart(shifts)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Metrics Grid ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatGridCard(Modifier.weight(1f), "Выручка", "${monthlySummary.gross.toInt()} ₸", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFFE8F5E9))
            StatGridCard(Modifier.weight(1f), "Расходы", "${(monthlySummary.gross - monthlySummary.profit).toInt()} ₸", Icons.AutoMirrored.Filled.TrendingDown, Color(0xFFFFEBEE))
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatGridCard(Modifier.weight(1f), "Средний доход", "${if (monthlySummary.count > 0) (monthlySummary.profit / monthlySummary.count).toInt() else 0} ₸", Icons.AutoMirrored.Filled.TrendingUp, Color.White)
            StatGridCard(Modifier.weight(1f), "Смены", "${monthlySummary.count}", Icons.AutoMirrored.Filled.TrendingUp, Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Категории расходов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                ExpenseItem("Топливо", monthlySummary.fuel, FuelOrange)
                ExpenseItem("Комиссии", monthlySummary.commissions, YandexYellow)
                ExpenseItem("Прочее", monthlySummary.gross - monthlySummary.profit - monthlySummary.fuel - monthlySummary.commissions, Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ProfitLineChart(shifts: List<com.example.taxigoal.data.entities.Shift>) {
    val points = remember(shifts) {
        shifts.take(10).reversed().map { it.netProfit.toFloat() }
    }

    if (points.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Недостаточно данных для\nотображения графика",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)) {
        val width = size.width
        val height = size.height
        val maxProfit = points.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        
        val spaceX = width / (points.size.coerceAtLeast(2) - 1)
        val path = Path()
        
        points.forEachIndexed { index, profit ->
            val x = index * spaceX
            val y = height - (profit / maxProfit * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = BrandPurple,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun StatGridCard(modifier: Modifier, title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = if (bgColor == Color.White) TextSecondary else TextPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 11.sp, color = TextSecondary)
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }
    }
}

@Composable
fun ExpenseItem(name: String, amount: Double, color: Color) {
    if (amount <= 0) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text(name, fontSize = 14.sp, modifier = Modifier.weight(1f), color = TextPrimary)
        Text("${amount.toInt()} ₸", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
    }
}
