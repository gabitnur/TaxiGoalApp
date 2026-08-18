package com.example.taxigoal.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.taxigoal.data.entities.AppLog
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val SCREEN = "ErrorLogScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorLogScreen(navController: NavController, viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()) }
    val context = LocalContext.current
    
    var selectedLogDetails by remember { mutableStateOf<AppLog?>(null) }
    var selectedLevel by remember { mutableStateOf("Все") }

    // Multi-selection state
    val selectedIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode by remember { derivedStateOf { selectedIds.isNotEmpty() } }

    val filteredLogs = remember(logs, selectedLevel) {
        if (selectedLevel == "Все") logs
        else {
            val levelFilter = when(selectedLevel) {
                "Ошибки" -> listOf("ERROR", "FATAL")
                "Инфо" -> listOf("INFO")
                else -> listOf("WARN")
            }
            logs.filter { it.level in levelFilter }
        }
    }

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) Text("${selectedIds.size} выбрано") 
                    else Text("Лог ошибок", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (isSelectionMode) selectedIds.clear() 
                        else navController.popBackStack() 
                    }) {
                        Icon(if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            val allIds = filteredLogs.map { it.id }
                            if (selectedIds.size == allIds.size) selectedIds.clear()
                            else {
                                selectedIds.clear()
                                selectedIds.addAll(allIds)
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, "Выделить все")
                        }
                        IconButton(onClick = { 
                            val logsToProcess = logs.filter { it.id in selectedIds }
                            copyLogsToClipboard(context, logsToProcess)
                        }) {
                            Icon(Icons.Default.ContentCopy, "Копировать")
                        }
                        IconButton(onClick = { 
                            val logsToProcess = logs.filter { it.id in selectedIds }
                            shareDiagnosticLog(context, logsToProcess)
                        }) {
                            Icon(Icons.Default.Share, "Поделиться")
                        }
                        IconButton(onClick = { 
                            viewModel.deleteLogsByIds(selectedIds.toList())
                            selectedIds.clear()
                        }) {
                            Icon(Icons.Default.Delete, "Удалить", tint = Color.Red)
                        }
                    } else {
                        IconButton(onClick = { copyLogsToClipboard(context, logs) }) {
                            Icon(Icons.Default.ContentCopy, "Копировать", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { shareDiagnosticLog(context, logs) }) {
                            Icon(Icons.Default.Share, "Поделиться", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.clearLogs() }) {
                            Icon(Icons.Default.DeleteSweep, "Очистить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                .padding(horizontal = 16.dp)
        ) {
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelChip("Все", selectedLevel) { selectedLevel = it }
                LevelChip("Ошибки", selectedLevel) { selectedLevel = it }
                LevelChip("Инфо", selectedLevel) { selectedLevel = it }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    val isSelected = selectedIds.contains(log.id)
                    LogEntryItem(
                        log = log, 
                        dateFormat = dateFormat,
                        isSelected = isSelected,
                        onLongClick = {
                            if (!selectedIds.contains(log.id)) selectedIds.add(log.id)
                        },
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedIds.remove(log.id) else selectedIds.add(log.id)
                            } else {
                                selectedLogDetails = log
                            }
                        }
                    )
                }
            }
        }
    }

    // Диалог с подробностями ошибки
    selectedLogDetails?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogDetails = null },
            title = { Text(log.title) },
            text = {
                Column {
                    Text("Событие: ${log.event}", fontWeight = FontWeight.Bold)
                    Text("Экран: ${log.screen}")
                    Text("Session: ${log.sessionId.take(8)}...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Сообщение:", fontWeight = FontWeight.Bold)
                    Text(log.message)
                    log.details?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Детали (Error):", fontWeight = FontWeight.Bold, color = Color.Red)
                        Text(it, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedLogDetails = null }) { Text("Закрыть") }
            }
        )
    }
}

@Composable
fun LevelChip(label: String, selected: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = label == selected,
        onClick = { onSelect(label) },
        label = { Text(label) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogEntryItem(
    log: AppLog, 
    dateFormat: SimpleDateFormat, 
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val color = when(log.level) {
        "ERROR", "FATAL" -> Color.Red
        "WARN" -> Color(0xFFFF9800)
        else -> Color(0xFF007AFF)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
            Column {
                Text(
                    text = "[${log.screen}] ${log.title}", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.message, 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(log.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun formatLogsForExport(logs: List<AppLog>): String {
    val sb = StringBuilder()
    sb.append("================================================\n")
    sb.append("MY INCOME — DIAGNOSTIC LOG\n")
    sb.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
    sb.append("Count: ${logs.size}\n")
    sb.append("================================================\n\n")
    
    logs.forEach { log ->
        val safeMsg = AppLogger.sanitize(log.message)
        val safeDetails = log.details?.let { AppLogger.sanitize(it) }

        sb.append("[${SimpleDateFormat("HH:mm:ss").format(log.timestamp)}] ")
        sb.append("${log.level} | ${log.screen} | ${log.event}\n")
        sb.append("Msg: $safeMsg\n")
        if (safeDetails != null) sb.append("Details: $safeDetails\n")
        sb.append("Session: ${log.sessionId}\n")
        sb.append("------------------------------------------------\n")
    }
    return sb.toString()
}

private fun copyLogsToClipboard(context: Context, logs: List<AppLog>) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = formatLogsForExport(logs)
    val clip = ClipData.newPlainText("Diagnostic Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Лог скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
}

private fun shareDiagnosticLog(context: Context, logs: List<AppLog>) {
    val text = formatLogsForExport(logs)
    val file = File(context.cacheDir, "diagnostic_log.txt")
    file.writeText(text)
    
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Отправить диагностический лог"))
}
