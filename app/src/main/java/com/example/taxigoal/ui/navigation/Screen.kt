package com.example.taxigoal.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Главная", Icons.Default.Home)
    object Statistics : Screen("statistics", "Статистика", Icons.Default.BarChart)
    object Shift : Screen("shift", "Добавить", Icons.Default.Add)
    object Goals : Screen("goals", "Цели", Icons.Default.EmojiEvents)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    
    // Sub-screens
    object Profile : Screen("profile", "Профиль", Icons.Default.Person)
    object History : Screen("history", "История", Icons.Default.History)
    object YandexImport : Screen("yandex_import", "Яндекс Про", Icons.Default.TaxiAlert)
    object BankImport : Screen("bank_import", "Банк", Icons.Default.AccountBalance)
    object AIChat : Screen("ai_chat", "AI Помощник", Icons.Default.SmartToy)
    object GeminiApi : Screen("gemini_api", "Gemini API", Icons.Default.VpnKey)
    object Backup : Screen("backup", "Google Backup", Icons.Default.CloudUpload)
    object AppUpdate : Screen("app_update", "Обновление", Icons.Default.SystemUpdate)
    object ErrorLog : Screen("error_log", "Лог ошибок", Icons.Default.BugReport)
    object Diagnostics : Screen("diagnostics", "Диагностика", Icons.Default.Terminal)
    object GeminiDiagnostic : Screen("gemini_diagnostic", "Gemini Diag", Icons.Default.Terminal)
    object About : Screen("about", "О приложении", Icons.Default.Info)
    object GoalDetails : Screen("goal_details/{goalId}", "Детали цели", Icons.Default.Info)
}
