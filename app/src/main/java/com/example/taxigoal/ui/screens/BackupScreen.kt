package com.example.taxigoal.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
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
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.BackupState
import com.example.taxigoal.viewmodel.BackupViewModel
import com.example.taxigoal.viewmodel.MainViewModel
import com.example.taxigoal.workers.AutoBackupWorker
import com.google.firebase.auth.FirebaseAuth

private const val SCREEN = "BackupScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(navController: NavController, mainViewModel: MainViewModel) {
    val backupViewModel: BackupViewModel = viewModel()
    val state by backupViewModel.state.collectAsState()
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    
    var showRestoreDialog by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf(AutoBackupWorker.Companion.BackupInterval.DISABLED) }

    val authLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            backupViewModel.createBackup()
        } else {
            backupViewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        backupViewModel.authIntentEvent.collect { intent ->
            authLauncher.launch(intent)
        }
    }

    LaunchedEffect(state) {
        if (state is BackupState.BackupListLoaded) {
            showRestoreDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Резервное копирование", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CloudUpload, 
                null, 
                modifier = Modifier.size(80.dp),
                tint = if (user?.isAnonymous == true) Color.Gray else BrandPurple
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (user?.isAnonymous == true) "Войдите через Google для работы с облаком" else "Данные сохраняются в ваш Google Drive",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            if (user != null && !user.isAnonymous) {
                Text(
                    text = user.email ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            when (val current = state) {
                is BackupState.Idle, is BackupState.BackupListLoaded -> {
                    Button(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_CREATE_BACKUP")
                            backupViewModel.createBackup() 
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        enabled = user != null && !user.isAnonymous
                    ) {
                        Text("Создать резервную копию", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { 
                            AppLogger.buttonClick(SCREEN, "BTN_LOAD_BACKUP_LIST")
                            backupViewModel.loadBackupList()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = user != null && !user.isAnonymous
                    ) {
                        Icon(Icons.Default.Restore, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Восстановить из списка", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Auto-backup interval selector
                    Text("Автоматическое копирование", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedInterval.title,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AutoBackupWorker.Companion.BackupInterval.entries.forEach { interval ->
                                DropdownMenuItem(
                                    text = { Text(interval.title) },
                                    onClick = {
                                        selectedInterval = interval
                                        expanded = false
                                        AutoBackupWorker.scheduleBackup(context, interval)
                                    }
                                )
                            }
                        }
                    }
                }
                is BackupState.Loading -> {
                    CircularProgressIndicator(color = BrandPurple)
                    Text("Обработка запроса...", modifier = Modifier.padding(top = 16.dp))
                }
                is BackupState.Success -> {
                    Text(
                        text = "🟢 ${current.message}",
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { backupViewModel.resetState() }) { Text("OK") }
                }
                is BackupState.Error -> {
                    Text(
                        text = "🔴 ${current.message}",
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { backupViewModel.resetState() }) { Text("Попробовать снова") }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Резервная копия содержит ваши смены, цели и расходы. API ключи не сохраняются.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showRestoreDialog) {
        val currentState = state
        if (currentState is BackupState.BackupListLoaded) {
            AlertDialog(
                onDismissRequest = { 
                    showRestoreDialog = false
                    backupViewModel.resetState()
                },
                title = { Text("Выберите точку восстановления") },
                text = {
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentState.files) { file ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            showRestoreDialog = false
                                            backupViewModel.restoreBackupById(file.id) 
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(file.formattedDate, fontWeight = FontWeight.Bold)
                                        Text(file.name, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showRestoreDialog = false
                        backupViewModel.resetState()
                    }) { Text("Отмена") }
                }
            )
        }
    }
}
