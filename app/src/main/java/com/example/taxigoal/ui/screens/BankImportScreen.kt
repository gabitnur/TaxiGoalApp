package com.example.taxigoal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxigoal.data.entities.FinancialTransaction
import com.example.taxigoal.ui.theme.*
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.BankImportState
import com.example.taxigoal.viewmodel.BankImportViewModel
import com.example.taxigoal.viewmodel.MainViewModel

private const val SCREEN = "BankImportScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankImportScreen(navController: NavController) {
    val importViewModel: BankImportViewModel = viewModel()
    val state by importViewModel.state.collectAsState()
    val context = LocalContext.current
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            selectedFileUri = uri
            uri?.let {
                AppLogger.info(SCREEN, "PDF_SELECTED", it.toString())
                importViewModel.parsePdf(it)
            }
        }
    )

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт выписки банка", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            when (val current = state) {
                is BankImportState.Idle -> {
                    UploadZone { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                }
                is BankImportState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandPurple)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Парсинг выписки...")
                        }
                    }
                }
                is BankImportState.Success -> {
                    PreviewZone(current.transactions, 
                        onConfirm = { 
                            importViewModel.saveTransactions(current.transactions)
                            navController.popBackStack()
                        }, 
                        onCancel = { importViewModel.reset() }
                    )
                }
                is BankImportState.Error -> {
                    ErrorZone(current.message) { importViewModel.reset() }
                }
            }
        }
    }
}

@Composable
fun UploadZone(onSelect: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .clickable { onSelect() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Загрузите PDF выписку", fontWeight = FontWeight.Bold)
                Text("Каспи, Халык и другие банки", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onSelect) { Text("Выбрать файл") }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Что будет извлечено:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(16.dp))
        
        listOf("Доходы (пополнения)", "Расходы (покупки)", "Даты операций", "Категории").forEach { field ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(field, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PreviewZone(list: List<FinancialTransaction>, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("Найдено операций: ${list.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(Modifier.weight(1f)) {
            items(list) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (tx.type == "INCOME") "+" else "-",
                            color = if (tx.type == "INCOME") SuccessGreen else Color.Red,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tx.description, maxLines = 1, fontSize = 13.sp)
                            Text(tx.category, fontSize = 11.sp, color = Color.Gray)
                        }
                        Text("${tx.amount.toInt()} ₸", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Импортировать все", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Отмена", color = Color.Gray)
        }
    }
}

@Composable
fun ErrorZone(msg: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(64.dp))
        Text(msg, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) { Text("Попробовать снова") }
    }
}
