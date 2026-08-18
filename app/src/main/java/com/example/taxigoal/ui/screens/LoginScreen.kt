package com.example.taxigoal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taxigoal.ui.theme.BrandPurple
import com.example.taxigoal.ui.theme.TextPrimary
import com.example.taxigoal.ui.theme.TextSecondary
import com.example.taxigoal.utils.AppLogger

private const val SCREEN = "LoginScreen"

@Composable
fun LoginScreen(
    onGoogleLoginClick: () -> Unit,
    onGuestLoginClick: () -> Unit
) {
    AppLogger.screenOpen(SCREEN)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✦",
            fontSize = 64.sp,
            color = BrandPurple,
            fontWeight = FontWeight.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "МОЙ ДОХОД",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = "Ваш финансовый помощник",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Google Login Button
        Button(
            onClick = {
                AppLogger.buttonClick(SCREEN, "BTN_GOOGLE_LOGIN")
                onGoogleLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G ", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Войти через Google", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("или", color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Guest Login Button
        OutlinedButton(
            onClick = {
                AppLogger.buttonClick(SCREEN, "BTN_GUEST_LOGIN")
                onGuestLoginClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Продолжить как гость", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "Продолжая, вы соглашаетесь с использованием приложения.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
