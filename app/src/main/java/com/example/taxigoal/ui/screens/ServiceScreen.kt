package com.example.taxigoal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.viewmodel.TaxiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ServiceScreen(viewModel: TaxiViewModel) {
    var type by remember { mutableStateOf("OIL") }
    var amount by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    val costs by viewModel.allServiceCosts.collectAsState(initial = emptyList())
    val rawMileage by viewModel.lastMileage.collectAsState()
    val currentMileage = rawMileage ?: 0.0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("🔧 ТО и Затраты", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // Reminder Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (currentMileage > 100000) Color(0xFFFFCCBC) else Color(0xFFE8F5E9)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Напоминание о масле", fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Текущий пробег: ${currentMileage.toInt()} км", color = Color.Black)
                val remaining = (10000 - (currentMileage % 10000)).toInt()
                Text("Следующая замена через: $remaining км", color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input form
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val btnModifier = Modifier.weight(1f)
                    FilterChip(selected = type == "OIL", onClick = { type = "OIL" }, label = { Text("Масло") })
                    FilterChip(selected = type == "REPAIR", onClick = { type = "REPAIR" }, label = { Text("Ремонт") })
                    FilterChip(selected = type == "FUEL", onClick = { type = "FUEL" }, label = { Text("Бензин") })
                    FilterChip(selected = type == "OTHER", onClick = { type = "OTHER" }, label = { Text("Разное") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text("Сумма (₸)") }, modifier = Modifier.fillMaxWidth())
                if (type != "OTHER") {
                    OutlinedTextField(mileage, { mileage = it }, label = { Text("Пробег на момент ТО") }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { 
                        viewModel.addServiceCost(type, amount.toDoubleOrNull() ?: 0.0, mileage.toDoubleOrNull() ?: 0.0)
                        amount = ""; mileage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = amount.isNotBlank()
                ) {
                    Text("ДОБАВИТЬ В РАСХОДЫ (МИНУС ИЗ ЦЕЛИ)")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("📊 История расходов", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(costs) { cost ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val emoji = when(cost.type) {
                            "OIL" -> "🛢️"
                            "REPAIR" -> "🛠️"
                            "FUEL" -> "⛽"
                            else -> "💸"
                        }
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cost.type, fontWeight = FontWeight.Bold)
                            Text(SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(cost.date), fontSize = 12.sp)
                        }
                        Text("-${cost.amount.toInt()} ₸", fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                }
            }
        }
    }
}
