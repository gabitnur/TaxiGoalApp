package com.example.taxigoal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.taxigoal.GeminiRepository
import com.example.taxigoal.VoiceAiManager
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.utils.CurrencyFormatter
import com.example.taxigoal.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geminiRepository = remember { GeminiRepository(context) }
    
    var gross by remember { mutableStateOf("") }
    var fuel by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var food by remember { mutableStateOf("") }
    var wash by remember { mutableStateOf("") }
    var maintenance by remember { mutableStateOf("") }
    var fines by remember { mutableStateOf("") }
    var other by remember { mutableStateOf("") }
    
    var isProcessingVoice by remember { mutableStateOf(false) }
    
    val activeGoal by viewModel.activeGoal.collectAsState()
    val vehicle by viewModel.vehicle.collectAsState()

    val grossVal = gross.toDoubleOrNull() ?: 0.0
    val fuelVal = fuel.toDoubleOrNull() ?: 0.0
    val mileageVal = mileage.toDoubleOrNull() ?: 0.0
    val foodVal = food.toDoubleOrNull() ?: 0.0
    val washVal = wash.toDoubleOrNull() ?: 0.0
    val maintenanceVal = maintenance.toDoubleOrNull() ?: 0.0
    val finesVal = fines.toDoubleOrNull() ?: 0.0
    val otherVal = other.toDoubleOrNull() ?: 0.0
    
    val comms = grossVal * 0.18
    val calcDepreciation = if (vehicle != null) mileageVal * (vehicle!!.depreciationPerKm + vehicle!!.tiresPerKm + vehicle!!.otherPerKm) else 0.0
    val netProfit = grossVal - comms - fuelVal - foodVal - washVal - maintenanceVal - finesVal - otherVal - calcDepreciation

    val voiceManager = remember {
        VoiceAiManager(context) { text ->
            isProcessingVoice = true
            scope.launch {
                val result = geminiRepository.parseVoiceInput(text)
                result.onSuccess { map ->
                    if (map["income"] != null) gross = map["income"]!!.toInt().toString()
                    if (map["fuel"] != null) fuel = map["fuel"]!!.toInt().toString()
                    if (map["food"] != null) food = map["food"]!!.toInt().toString()
                    if (map["mileage"] != null) mileage = map["mileage"]!!.toInt().toString()
                    if (map["wash"] != null) wash = map["wash"]!!.toInt().toString()
                    if (map["maintenance"] != null) maintenance = map["maintenance"]!!.toInt().toString()
                    if (map["fines"] != null) fines = map["fines"]!!.toInt().toString()
                    if (map["other"] != null) other = map["other"]!!.toInt().toString()
                }.onFailure {
                    Toast.makeText(context, "Не удалось распознать данные", Toast.LENGTH_SHORT).show()
                }
                isProcessingVoice = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) voiceManager.startListening()
    }

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
                text = "Новая смена",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            
            if (isProcessingVoice) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = {
                    val permission = Manifest.permission.RECORD_AUDIO
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        voiceManager.startListening()
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }) {
                    Icon(Icons.Default.Mic, "Голос", tint = BrandPurple)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ShiftInputField("Выручка (₸)", gross, { gross = it }, Icons.Default.Payments)
        ShiftInputField("Пробег (км)", mileage, { mileage = it }, Icons.Default.Route)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Расходы", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShiftInputField("Топливо", fuel, { fuel = it }, Icons.Default.LocalGasStation, Modifier.weight(1f))
            ShiftInputField("Еда", food, { food = it }, Icons.Default.Restaurant, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShiftInputField("Мойка", wash, { wash = it }, Icons.Default.Waves, Modifier.weight(1f))
            ShiftInputField("Ремонт", maintenance, { maintenance = it }, Icons.Default.Build, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShiftInputField("Штрафы", fines, { fines = it }, Icons.Default.Gavel, Modifier.weight(1f))
            ShiftInputField("Другое", other, { other = it }, Icons.Default.MoreHoriz, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                ResultRow("Комиссия (18%)", "- ${CurrencyFormatter.format(comms)}")
                if (calcDepreciation > 0) {
                    ResultRow("Амортизация", "- ${CurrencyFormatter.format(calcDepreciation)}")
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Чистая прибыль", fontWeight = FontWeight.Bold)
                    Text(CurrencyFormatter.format(netProfit), fontWeight = FontWeight.Bold, color = if (netProfit >= 0) SuccessGreen else Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                viewModel.saveShift(
                    gross = grossVal,
                    fuel = fuelVal,
                    mileage = mileageVal,
                    food = foodVal,
                    wash = washVal,
                    maintenance = maintenanceVal,
                    fines = finesVal,
                    other = otherVal,
                    goalId = activeGoal?.id
                )
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = gross.isNotBlank() && mileage.isNotBlank()
        ) {
            Text("Сохранить смену", fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ShiftInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp), tint = BrandPurple) },
        modifier = modifier.padding(top = 4.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
    )
}
