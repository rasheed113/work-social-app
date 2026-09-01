package com.rasheed113.worksocial.presentation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

private val WorkSocialLightColors = lightColorScheme(
    primary = WorkSocialColors.Primary,
    secondary = Color(0xFF334E8C),
    tertiary = WorkSocialColors.WorkHouseEnd,
    background = WorkSocialColors.Background,
    surface = WorkSocialColors.Surface,
    onBackground = WorkSocialColors.Text,
    onSurface = WorkSocialColors.Text,
    outline = WorkSocialColors.Border,
)

private val WorkSocialMaterialTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = WorkSocialColors.Text),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, color = WorkSocialColors.Text),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = WorkSocialColors.Text),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp, color = WorkSocialColors.Text),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp, color = WorkSocialColors.Text),
)

@Composable
fun WorkSocialTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WorkSocialLightColors, typography = WorkSocialMaterialTypography, shapes = androidx.compose.material3.Shapes(medium = WorkSocialShapes.Card, large = WorkSocialShapes.Card), content = content)
}
