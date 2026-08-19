package com.example.taxigoal.ui.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.TaxiAlert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.taxigoal.R
import com.example.taxigoal.ui.screens.*
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.utils.AppLogger
import com.example.taxigoal.viewmodel.AuthState
import com.example.taxigoal.viewmodel.AuthViewModel
import com.example.taxigoal.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val SCREEN = "Navigation"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiGoalNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    
    var showAddMenu by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Google Sign-In Setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                AppLogger.info("Auth", "AUTH_GOOGLE_START", "Received Google Token")
                authViewModel.signInWithGoogle(account.idToken!!) { success ->
                    if (success) AppLogger.info("Auth", "AUTH_GOOGLE_SUCCESS", "Firebase signed in")
                    else AppLogger.error("Auth", "AUTH_GOOGLE_FAILED", "Firebase sign in failed")
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val statusMessage = com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString(statusCode)
                AppLogger.error("Auth", "AUTH_GOOGLE_FAILED", "Code: $statusCode ($statusMessage)")
                
                val hint = when (statusCode) {
                    10 -> "DEVELOPER_ERROR: Проверьте SHA-1 в консоли Firebase"
                    12500 -> "SIGN_IN_FAILED: Проверьте интернет или Google Play Services"
                    12501 -> "SIGN_IN_CANCELLED: Вы отменили вход"
                    else -> "Ошибка #$statusCode: $statusMessage"
                }
                Toast.makeText(context, hint, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                AppLogger.error("Auth", "AUTH_GOOGLE_FAILED", e.message ?: "Unknown Exception")
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Log navigation events
    LaunchedEffect(currentDestination) {
        currentDestination?.route?.let { route ->
            AppLogger.info(SCREEN, "NAVIGATION_SUCCESS", "User reached $route")
        }
    }

    when (authState) {
        is AuthState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPurple)
            }
        }
        is AuthState.Unauthenticated -> {
            LoginScreen(
                onGoogleLoginClick = { 
                    AppLogger.buttonClick("Login", "BTN_GOOGLE_LOGIN")
                    
                    val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                    val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
                    
                    if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                        try {
                            googleLauncher.launch(googleSignInClient.signInIntent)
                        } catch (e: Exception) {
                            AppLogger.error("Auth", "LAUNCH_FAILED", e.message ?: "Unknown")
                            Toast.makeText(context, "Не удалось запустить вход", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        AppLogger.warn("Auth", "PLAY_SERVICES_MISSING", "Code: $resultCode")
                        if (googleApiAvailability.isUserResolvableError(resultCode)) {
                            val activity = context as? Activity
                            if (activity != null) {
                                googleApiAvailability.getErrorDialog(activity, resultCode, 9000)?.show()
                            } else {
                                Toast.makeText(context, "Обновите Google Play Services", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Google Play Services не поддерживаются", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onGuestLoginClick = { 
                    AppLogger.info("Auth", "AUTH_ANONYMOUS_START", "Starting guest login")
                    authViewModel.signInAnonymously { success ->
                        if (success) AppLogger.info("Auth", "AUTH_ANONYMOUS_SUCCESS", "Guest login ok")
                        else AppLogger.error("Auth", "AUTH_ANONYMOUS_FAILED", "Guest login fail")
                    }
                }
            )
        }
        is AuthState.Authenticated -> {
            val mainScreens = listOf(Screen.Home, Screen.Statistics, Screen.Shift, Screen.Goals, Screen.Settings)

            Scaffold(
                bottomBar = {
                    if (currentDestination?.route in mainScreens.map { it.route } || currentDestination?.route == Screen.Home.route) {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            mainScreens.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                NavigationBarItem(
                                    icon = {
                                        if (screen == Screen.Shift) {
                                            Surface(
                                                color = BrandPurple,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                            }
                                        } else {
                                            Icon(screen.icon, contentDescription = null)
                                        }
                                    },
                                    label = { if (screen != Screen.Shift) Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        if (screen == Screen.Shift) {
                                            showAddMenu = true
                                        } else {
                                            AppLogger.buttonClick("BottomNav", "BTN_NAV_${screen.route.uppercase()}")
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BrandPurple,
                                        unselectedIconColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                if (showAddMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        AddMenuContent(
                            onAction = { route ->
                                showAddMenu = false
                                navController.navigate(route)
                            }
                        )
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Home.route) { HomeScreen(navController, viewModel) }
                    composable(Screen.Statistics.route) { StatisticsScreen(viewModel) }
                    composable(Screen.Shift.route) { ShiftScreen(navController, viewModel) }
                    composable(Screen.Goals.route) { GoalsScreen(navController, viewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(navController, viewModel) }
                    
                    // Sub-screens
                    composable(Screen.Profile.route) { ProfileScreen(navController, viewModel) }
                    composable(Screen.History.route) { HistoryScreen(navController, viewModel) }
                    composable(Screen.YandexImport.route) { YandexImportScreen(navController) }
                    composable(Screen.BankImport.route) { BankImportScreen(navController) }
                    composable(Screen.AIChat.route) { AIChatScreen(navController, viewModel) }
                    composable(Screen.GeminiApi.route) { GeminiApiScreen(navController, viewModel) }
                    composable(Screen.Backup.route) { BackupScreen(navController, viewModel) }
                    composable(Screen.AppUpdate.route) { AppUpdateScreen(navController, viewModel) }
                    composable(Screen.ErrorLog.route) { ErrorLogScreen(navController, viewModel) }
                    composable(Screen.Diagnostics.route) { DiagnosticsScreen(navController, viewModel) }
                    composable(Screen.GeminiDiagnostic.route) { GeminiDiagnosticScreen(navController) }
                    composable(Screen.About.route) { AboutScreen(navController) }
                    composable("report_bug") { ReportBugScreen(navController, viewModel) }
                    composable(Screen.GoalDetails.route) { backStackEntry ->
                        val goalId = backStackEntry.arguments?.getString("goalId")?.toLongOrNull()
                        GoalDetailsScreen(navController, viewModel, goalId)
                    }
                }
            }
        }
    }
}

@Composable
fun AddMenuContent(onAction: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Text("Добавить", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        AddMenuItem("Добавить смену", Icons.AutoMirrored.Filled.Assignment) { onAction(Screen.Shift.route) }
        AddMenuItem("Импорт Яндекс Про", Icons.Default.TaxiAlert) { onAction(Screen.YandexImport.route) }
        AddMenuItem("Импорт банка", Icons.Default.AccountBalance) { onAction(Screen.BankImport.route) }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AddMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = BrandPurple)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
