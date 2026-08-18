package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.taxigoal.ui.components.GeminiIcon
import com.example.taxigoal.ui.theme.AppBackground
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.BrandPurpleLight
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.ChatMessage
import com.example.taxigoal.viewmodel.ChatViewModel
import com.example.taxigoal.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

private const val SCREEN = "AIChatScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(navController: NavController, mainViewModel: MainViewModel) {
    val chatViewModel: ChatViewModel = viewModel()
    val messages by chatViewModel.messages.collectAsState()
    val isLoading by chatViewModel.isLoading.collectAsState()
    val error by chatViewModel.error.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) { AppLogger.screenOpen(SCREEN) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // --- Header ---
        TopAppBar(
            title = { Text("AI Помощник", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                TextButton(onClick = { chatViewModel.clearHistory() }) {
                    Text("История", color = BrandPurple)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
        )

        // --- Messages List ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message)
            }
            
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TaxiBot думает...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            
            error?.let {
                item {
                    Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }

        // --- Suggestion Chips ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionChip(onClick = { chatViewModel.sendMessage("Статистика за месяц") }, label = { Text("Статистика за месяц") })
            SuggestionChip(onClick = { chatViewModel.sendMessage("Прогресс по цели") }, label = { Text("Прогресс по цели") })
        }

        // --- Input Area ---
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Задайте вопрос...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BrandPurpleLight,
                        focusedContainerColor = BrandPurpleLight
                    ),
                    enabled = !isLoading,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { 
                        AppLogger.buttonClick(SCREEN, "BTN_SEND_MSG")
                        chatViewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    modifier = Modifier.background(if (inputText.isBlank() || isLoading) Color.Gray else BrandPurple, CircleShape),
                    enabled = inputText.isNotBlank() && !isLoading
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) BrandPurpleLight else Color.White
    val shape = if (message.isUser) 
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) 
    else 
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(horizontalAlignment = alignment, modifier = Modifier.fillMaxWidth()) {
        if (!message.isUser) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeminiIcon(size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Gemini", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Box(
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(16.dp)
        ) {
            Text(text = message.text, fontSize = 14.sp, color = Color.Black)
        }
        Text(text = timeStr, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
    }
}
