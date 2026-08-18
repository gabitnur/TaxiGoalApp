package com.example.taxigoal.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.taxigoal.R

@Composable
fun GeminiIcon(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_gemini),
        contentDescription = "Gemini AI",
        modifier = modifier.size(size),
        tint = Color.Unspecified // To keep the gradient from XML
    )
}
