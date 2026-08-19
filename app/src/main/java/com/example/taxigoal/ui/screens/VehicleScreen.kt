package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.data.entities.Vehicle
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleScreen(navController: NavController, viewModel: MainViewModel) {
    val vehicle by viewModel.vehicle.collectAsState()
    
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var consumption by remember { mutableStateOf("") }
    var fuelPrice by remember { mutableStateOf("") }
    var depreciation by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf("") }
    var tires by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }

    LaunchedEffect(vehicle) {
        vehicle?.let {
            brand = it.brand
            model = it.model
            consumption = it.fuelConsumption.toString()
            fuelPrice = it.fuelPrice.toString()
            depreciation = it.depreciationPerKm.toString()
            maintenance = it.maintenancePerKm.toString()
            tires = it.tiresPerKm.toString()
            other = it.otherPerKm.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мой автомобиль", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateVehicle(Vehicle(
                            userId = "",
                            brand = brand,
                            model = model,
                            fuelConsumption = consumption.toDoubleOrNull() ?: 0.0,
                            fuelPrice = fuelPrice.toDoubleOrNull() ?: 0.0,
                            depreciationPerKm = depreciation.toDoubleOrNull() ?: 0.0,
                            maintenancePerKm = maintenance.toDoubleOrNull() ?: 0.0,
                            tiresPerKm = tires.toDoubleOrNull() ?: 0.0,
                            otherPerKm = other.toDoubleOrNull() ?: 0.0
                        ))
                        navController.popBackStack()
                    }) {
                        Text("Сохранить", color = BrandPurple, fontWeight = FontWeight.Bold)
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = BrandPurple, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Профиль авто", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Для точного расчета прибыли", color = Color.Gray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Характеристики", fontWeight = FontWeight.Bold)
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Марка") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Модель") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Расходы на 1 км", fontWeight = FontWeight.Bold)
            Text("Эти суммы будут автоматически вычитаться из прибыли", fontSize = 11.sp, color = Color.Gray)
            
            OutlinedTextField(value = depreciation, onValueChange = { depreciation = it }, label = { Text("Амортизация (₸/км)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = maintenance, onValueChange = { maintenance = it }, label = { Text("Масло и ТО (₸/км)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tires, onValueChange = { tires = it }, label = { Text("Шины (₸/км)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = other, onValueChange = { other = it }, label = { Text("Прочее (₸/км)") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}
