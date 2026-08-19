package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.ui.components.DepositDialog
import com.example.taxigoal.ui.components.GoalCard
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.viewmodel.MainViewModel

@Composable
fun GoalsScreen(navController: NavController, viewModel: MainViewModel) {
    val goals by viewModel.goals.collectAsState()
    val monthlyStats by viewModel.monthlyStats.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalIdForDeposit by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Мои цели", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Добавить", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (goals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("У вас пока нет целей", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingPadding(bottom = 100.dp)
            ) {
                items(goals) { goal ->
                    GoalCard(
                        goal = goal,
                        avgDailyProfit = monthlyStats.avgDailyProfit,
                        onEditClick = { navController.navigate("goal_details/${goal.id}") },
                        onDepositClick = { selectedGoalIdForDeposit = goal.id }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount ->
                viewModel.addGoal(title, amount)
                showAddDialog = false
            }
        )
    }

    if (selectedGoalIdForDeposit != null) {
        DepositDialog(
            goalId = selectedGoalIdForDeposit!!,
            todayProfit = monthlyStats.profit,
            onDismiss = { selectedGoalIdForDeposit = null },
            onConfirm = { amount ->
                viewModel.depositToGoal(selectedGoalIdForDeposit!!, amount)
                selectedGoalIdForDeposit = null
            }
        )
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая цель") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (например, Покупка машины)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма цели (₸)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, amount.toDoubleOrNull() ?: 0.0) },
                enabled = title.isNotBlank() && amount.isNotBlank()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun PaddingPadding(bottom: androidx.compose.ui.unit.Dp) = PaddingValues(bottom = bottom)
