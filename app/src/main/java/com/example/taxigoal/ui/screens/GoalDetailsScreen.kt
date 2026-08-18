package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.taxigoal.ui.components.GoalCard
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel

private const val SCREEN = "GoalDetailsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailsScreen(navController: NavController, viewModel: MainViewModel, goalId: Long?) {
    val goals by viewModel.goals.collectAsState()
    val goal = goals.find { it.id == goalId }
    
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var addAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal?.title ?: "Детали цели") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(padding)
                .padding(16.dp)
        ) {
            if (goal != null) {
                GoalCard(goal = goal, onEditClick = { })
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Действия", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showAddMoneyDialog = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Внести деньги")
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_DELETE_GOAL")
                            // Logic to delete would be here in VM
                            navController.popBackStack()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Удалить цель")
                    }
                }
            } else {
                Text("Цель не найдена", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    if (showAddMoneyDialog && goal != null) {
        AlertDialog(
            onDismissRequest = { showAddMoneyDialog = false },
            title = { Text("Внести деньги в цель") },
            text = {
                OutlinedTextField(
                    value = addAmount,
                    onValueChange = { addAmount = it },
                    label = { Text("Сумма (₸)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amount = addAmount.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        viewModel.updateGoalAccumulated(goal.id, amount)
                        AppLogger.buttonClick(SCREEN, "BTN_CONFIRM_ADD_MONEY")
                        showAddMoneyDialog = false
                    }
                }) {
                    Text("Внести")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMoneyDialog = false }) { Text("Отмена") }
            }
        )
    }
}
