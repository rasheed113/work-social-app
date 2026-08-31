package com.rasheed113.worksocial.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WorkSocialColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF0EA5A8),
    tertiary = Color(0xFF4F46E5),
    background = Color(0xFFF7FAFC),
    surface = Color.White
)

@Composable
fun WorkSocialTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WorkSocialColors, content = content)
}
