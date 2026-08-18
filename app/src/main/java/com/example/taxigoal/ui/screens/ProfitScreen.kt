package com.example.taxigoal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.data.local.ShiftEntity
import com.example.taxigoal.viewmodel.TaxiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfitScreen(viewModel: TaxiViewModel) {
    // Состояние формы ввода смены
    var gross by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf("") }
    var fine by remember { mutableStateOf("") }
    var otherExpenses by remember { mutableStateOf("") }

    val shifts by viewModel.allShifts.collectAsState(initial = emptyList())
    val goalInfo by viewModel.goalInfo.collectAsState()
    
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var selectedShiftForDetails by remember { mutableStateOf<ShiftEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- БЛОК 1: ЦЕЛЬ И НАКОПЛЕНИЯ ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯 Финансовая цель", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { showEditGoalDialog = true }) {
                            Text("Изменить")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Нужно накопить:")
                        Text("${goalInfo.target.toInt()} ₸", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Уже накоплено:")
                        Text("${goalInfo.accumulated.toInt()} ₸", fontWeight = FontWeight.Bold, color = Color(0xFF00C853))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Осталось накопить:", fontWeight = FontWeight.Bold)
                        Text("${goalInfo.remaining.toInt()} ₸", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { goalInfo.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }

        // --- БЛОК 2: ВВОД СМЕНЫ И РАСХОДОВ ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📝 Отчет за смену", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = gross, onValueChange = { gross = it },
                        label = { Text("Грязная выручка (₸)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fuel, onValueChange = { fuel = it },
                            label = { Text("Топливо (₸)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mileage, onValueChange = { mileage = it },
                            label = { Text("Пробег (км)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = maintenance, onValueChange = { maintenance = it },
                            label = { Text("ТО / Масло (₸)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fine, onValueChange = { fine = it },
                            label = { Text("Штрафы (₸)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = otherExpenses, onValueChange = { otherExpenses = it },
                        label = { Text("Разные расходы (₸)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.saveShiftWithDetails(
                                gross = gross.toDoubleOrNull() ?: 0.0,
                                fuel = fuel.toDoubleOrNull() ?: 0.0,
                                mileage = mileage.toDoubleOrNull() ?: 0.0,
                                maintenance = maintenance.toDoubleOrNull() ?: 0.0,
                                fines = fine.toDoubleOrNull() ?: 0.0,
                                other = otherExpenses.toDoubleOrNull() ?: 0.0
                            )
                            gross = ""; fuel = ""; mileage = ""; maintenance = ""; fine = ""; otherExpenses = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = gross.isNotBlank() && mileage.isNotBlank()
                    ) {
                        Text("РАССЧИТАТЬ И СОХРАНИТЬ")
                    }
                }
            }
        }

        // --- БЛОК 3: ИСТОРИЯ ---
        item {
            Text("📜 История смен", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        items(shifts) { shift ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(SimpleDateFormat("dd MMMM yyyy", Locale("ru")).format(shift.date), fontWeight = FontWeight.Medium)
                            Text("Выручка: ${shift.grossIncome.toInt()} ₸", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(
                            text = "${if (shift.netProfit >= 0) "+" else ""}${shift.netProfit.toInt()} ₸",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (shift.netProfit >= 0) Color(0xFF00C853) else Color(0xFFD50000)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { selectedShiftForDetails = shift },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ПОДРОБНО")
                    }
                }
            }
        }
    }

    // --- ДИАЛОГИ ---
    if (showEditGoalDialog) {
        var newGoalText by remember { mutableStateOf(goalInfo.target.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showEditGoalDialog = false },
            title = { Text("Изменить цель накопления") },
            text = {
                OutlinedTextField(
                    value = newGoalText,
                    onValueChange = { newGoalText = it },
                    label = { Text("Сумма цели (₸)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateGoalTarget(newGoalText.toDoubleOrNull() ?: goalInfo.target)
                    showEditGoalDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showEditGoalDialog = false }) { Text("Отмена") }
            }
        )
    }

    selectedShiftForDetails?.let { shift ->
        AlertDialog(
            onDismissRequest = { selectedShiftForDetails = null },
            title = { Text("Детализация смены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("➕ Заработано (грязными): ${shift.grossIncome.toInt()} ₸", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text("🔴 Разбор минусов (списания):", fontWeight = FontWeight.Bold)
                    Text("• Комиссии (Яндекс/Парк): -${shift.commissions.toInt()} ₸")
                    Text("• Бензин / Топливо: -${shift.fuelCost.toInt()} ₸")
                    if (shift.maintenanceCost > 0) Text("• ТО / Масло: -${shift.maintenanceCost.toInt()} ₸", color = Color.Red)
                    if (shift.fineCost > 0) Text("• Штрафы: -${shift.fineCost.toInt()} ₸", color = Color.Red)
                    if (shift.otherExpenses > 0) Text("• Разное: -${shift.otherExpenses.toInt()} ₸", color = Color.Red)
                    HorizontalDivider()
                    Text(
                        "🟢 Итоговый плюс в цель: ${shift.netProfit.toInt()} ₸",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedShiftForDetails = null }) { Text("Понятно") }
            }
        )
    }
}
