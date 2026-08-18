package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(navController: NavController, viewModel: MainViewModel) {
    var gross by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf("") }
    var fines by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    
    val activeGoal by viewModel.activeGoal.collectAsState()

    val grossVal = gross.toDoubleOrNull() ?: 0.0
    val commissions = grossVal * 0.18
    val fuelVal = fuel.toDoubleOrNull() ?: 0.0
    val maintenanceVal = maintenance.toDoubleOrNull() ?: 0.0
    val finesVal = fines.toDoubleOrNull() ?: 0.0
    val otherVal = other.toDoubleOrNull() ?: 0.0
    
    val netProfit = grossVal - commissions - fuelVal - maintenanceVal - finesVal - otherVal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { 
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") 
            }
            Text(
                text = "Отчёт за смену",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = {
                    viewModel.saveShift(
                        gross = grossVal,
                        fuel = fuelVal,
                        mileage = mileage.toDoubleOrNull() ?: 0.0,
                        maintenance = maintenanceVal,
                        fines = finesVal,
                        other = otherVal,
                        goalId = activeGoal?.id
                    )
                    navController.popBackStack()
                },
                enabled = gross.isNotBlank() && mileage.isNotBlank()
            ) { 
                Text("Сохранить", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) 
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("Дата смены", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = "17 августа 2026", 
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Доходы", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ShiftInputField("Выручка (брутто) (₸)", gross, { gross = it }, Icons.Default.Payments)

        Spacer(modifier = Modifier.height(24.dp))
        Text("Расходы", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ShiftInputField("Топливо (₸)", fuel, { fuel = it }, Icons.Default.LocalGasStation)
        ShiftInputField("ТО и масло (₸)", maintenance, { maintenance = it }, Icons.Default.Build)
        ShiftInputField("Штрафы (₸)", fines, { fines = it }, Icons.Default.Gavel)
        ShiftInputField("Прочие расходы (₸)", other, { other = it }, Icons.Default.MoreHoriz)

        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Чистая прибыль", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${netProfit.toInt()} ₸", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                viewModel.saveShift(
                    gross = grossVal,
                    fuel = fuelVal,
                    mileage = mileage.toDoubleOrNull() ?: 0.0,
                    maintenance = maintenanceVal,
                    fines = finesVal,
                    other = otherVal,
                    goalId = activeGoal?.id
                )
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = gross.isNotBlank() && mileage.isNotBlank()
        ) {
            Text("Сохранить смену", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ShiftInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
