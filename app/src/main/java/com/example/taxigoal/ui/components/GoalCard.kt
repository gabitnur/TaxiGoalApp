package com.example.taxigoal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.data.entities.Goal
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.BrandPurpleLight
import com.example.taxigoal.ui.theme.ProfitGreen
import com.example.taxigoal.utils.CurrencyFormatter
import java.util.*
import kotlin.math.ceil

@Composable
fun GoalCard(
    goal: Goal,
    avgDailyProfit: Double = 0.0,
    onEditClick: () -> Unit,
    onDepositClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 Финансовая цель",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                TextButton(onClick = onEditClick) {
                    Text("Изменить", color = BrandPurple)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = goal.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.Black)
            
            if (goal.description.isNotBlank()) {
                Text(text = goal.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(label = "Цель", value = CurrencyFormatter.format(goal.targetAmount), valueColor = BrandPurple)
                StatColumn(label = "Накоплено", value = CurrencyFormatter.format(goal.accumulatedAmount), valueColor = ProfitGreen)
                val remaining = (goal.targetAmount - goal.accumulatedAmount).coerceAtLeast(0.0)
                StatColumn(label = "Осталось", value = CurrencyFormatter.format(remaining), valueColor = if (remaining > 0) Color.Red else ProfitGreen)
            }

            val progress = if (goal.targetAmount > 0) (goal.accumulatedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = BrandPurple,
                trackColor = BrandPurpleLight,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                // Forecast logic
                if (progress >= 1f) {
                    Text("🎯 Цель достигнута!", color = ProfitGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else if (avgDailyProfit > 0) {
                    val remaining = goal.targetAmount - goal.accumulatedAmount
                    val days = ceil(remaining / avgDailyProfit).toInt()
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.DAY_OF_YEAR, days)
                    val dateStr = java.text.SimpleDateFormat("MMM yyyy", Locale("ru")).format(calendar.time)
                    
                    Text(
                        text = "Прогноз: ~ $days дн. (до $dateStr)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                } else {
                    Text("Начните смены для прогноза", fontSize = 11.sp, color = Color.Gray)
                }
                
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDepositClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurpleLight, contentColor = BrandPurple)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отложить с прибыли", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
