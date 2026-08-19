package com.example.taxigoal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.ui.theme.SuccessGreen
import com.example.taxigoal.utils.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositDialog(
    goalId: Long,
    todayProfit: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var customAmount by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отложить с прибыли") },
        text = {
            Column {
                Text("Чистая прибыль сегодня:", fontSize = 12.sp, color = Color.Gray)
                Text(CurrencyFormatter.format(todayProfit), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SuccessGreen)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Выберите сумму:", fontSize = 14.sp)
                
                val percentages = listOf(10, 20, 30, 50)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    percentages.forEach { p ->
                        val amount = (todayProfit * p / 100.0).coerceAtLeast(0.0)
                        InputChip(
                            selected = false,
                            onClick = { onConfirm(amount) },
                            label = { Text("$p% (${amount.toInt()})") }
                        )
                    }
                }
                
                OutlinedTextField(
                    value = customAmount,
                    onValueChange = { customAmount = it },
                    label = { Text("Своя сумма (₸)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val amount = customAmount.toDoubleOrNull() ?: 0.0
                    if (amount > 0) onConfirm(amount)
                },
                enabled = customAmount.isNotBlank()
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
