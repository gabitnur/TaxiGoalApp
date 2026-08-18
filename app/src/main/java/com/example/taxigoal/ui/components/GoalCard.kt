package com.example.taxigoal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.taxigoal.data.entities.Goal
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.BrandPurpleLight
import com.example.taxigoal.ui.theme.ProfitGreen

@Composable
fun GoalCard(
    goal: Goal,
    onEditClick: () -> Unit,
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
            Text(text = goal.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatColumn(label = "Цель", value = "${goal.targetAmount.toInt()} ₸", valueColor = BrandPurple)
                StatColumn(label = "Накоплено", value = "${goal.accumulatedAmount.toInt()} ₸", valueColor = ProfitGreen)
                StatColumn(label = "Осталось", value = "${(goal.targetAmount - goal.accumulatedAmount).coerceAtLeast(0.0).toInt()} ₸", valueColor = Color.Red)
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
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPurple
                )
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
