package com.example.taxigoal.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.taxigoal.BuildConfig
import com.example.taxigoal.ui.navigation.Screen
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.utils.PreferenceManager
import com.example.taxigoal.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private const val SCREEN_NAME = "SettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val preferenceManager = PreferenceManager(context)
    val themeMode by preferenceManager.themeFlow.collectAsState(initial = "SYSTEM")
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Profile Header ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { navController.navigate(Screen.Profile.route) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(text = user?.displayName ?: if (user?.isAnonymous == true) "Гость" else "Пользователь", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = user?.email ?: "user@gmail.com", fontSize = 12.sp, color = Color.Gray)
                    Text(text = if (user?.isAnonymous == true) "Гостевой аккаунт" else "Google Account", fontSize = 10.sp, color = BrandPurple, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = "Внешний вид") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Тема приложения", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Row {
                    ThemeOption("Светлая", themeMode == "LIGHT") { scope.launch { preferenceManager.setTheme("LIGHT") } }
                    Spacer(modifier = Modifier.width(8.dp))
                    ThemeOption("Темная", themeMode == "DARK") { scope.launch { preferenceManager.setTheme("DARK") } }
                    Spacer(modifier = Modifier.width(8.dp))
                    ThemeOption("Авто", themeMode == "SYSTEM") { scope.launch { preferenceManager.setTheme("SYSTEM") } }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Инструменты") {
            SettingsItem("Мой автомобиль", Icons.Default.DirectionsCar) { navController.navigate("vehicle") }
            SettingsItem("Цели", Icons.Default.EmojiEvents) { navController.navigate(Screen.Goals.route) }
            SettingsItem("AI Помощник", Icons.Default.SmartToy) { navController.navigate(Screen.AIChat.route) }
            SettingsItem("Gemini API", Icons.Default.VpnKey) { navController.navigate(Screen.GeminiApi.route) }
            SettingsItem("Google Backup", Icons.Default.CloudUpload) { navController.navigate(Screen.Backup.route) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSection(title = "Приложение") {
            SettingsItem("Лог ошибок", Icons.Default.BugReport) { navController.navigate(Screen.ErrorLog.route) }
            SettingsItem("Сообщить об ошибке", Icons.Default.Report) { navController.navigate("report_bug") }
            SettingsItem("Обновление", Icons.Default.SystemUpdate) { navController.navigate(Screen.AppUpdate.route) }
            SettingsItem("О приложении", Icons.Default.Info, value = BuildConfig.VERSION_NAME) { navController.navigate(Screen.About.route) }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (user?.isAnonymous == true) "Выйти из гостевого режима" else "Выйти из аккаунта", 
                color = Color.Red, 
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Выход") },
            text = { Text("Вы действительно хотите выйти?") },
            confirmButton = {
                TextButton(onClick = {
                    AppLogger.info(SCREEN_NAME, "AUTH_LOGOUT", "User requested logout")
                    FirebaseAuth.getInstance().signOut()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    GoogleSignIn.getClient(context, gso).signOut()
                    showLogoutDialog = false
                }) {
                    Text("Выйти", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, value: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (value != null) {
                Text(text = value, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.LightGray)
        }
    }
}
