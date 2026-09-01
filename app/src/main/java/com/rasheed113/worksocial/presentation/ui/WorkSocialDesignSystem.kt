package com.rasheed113.worksocial.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object WorkSocialColors {
    val Text = Color(0xFF17202A)
    val Background = Color(0xFFF5F7FA)
    val Surface = Color.White
    val Border = Color(0xFFE3E8EF)
    val Primary = Color(0xFF3155D9)
    val WorkHouseStart = Color(0xFF182A67)
    val WorkHouseEnd = Color(0xFF4F46E5)
}

object WorkSocialTypography {
    private val Sans = FontFamily.SansSerif
    val display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, color = WorkSocialColors.Text)
    val title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, color = WorkSocialColors.Text)
    val body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, color = WorkSocialColors.Text)
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, color = WorkSocialColors.Text)
}

object WorkSocialShapes {
    val Card = RoundedCornerShape(16.dp)
    val Button = RoundedCornerShape(12.dp)
}

object WorkSocialSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}

@Composable
fun WorkSocialCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.shadow(2.dp, WorkSocialShapes.Card), shape = WorkSocialShapes.Card, colors = CardDefaults.cardColors(containerColor = WorkSocialColors.Surface), border = androidx.compose.foundation.BorderStroke(1.dp, WorkSocialColors.Border), content = { content() })
}

@Composable
fun WorkSocialButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = WorkSocialShapes.Button, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = WorkSocialColors.Primary)) { Text(text, style = WorkSocialTypography.label.copy(color = Color.White)) }
}

@Composable
fun WorkSocialHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(WorkSocialSpacing.xs)) { Text(title, style = WorkSocialTypography.display); subtitle?.let { Text(it, style = WorkSocialTypography.body.copy(color = WorkSocialColors.Text.copy(alpha = .72f))) } }
}

@Composable
fun WorkHouseHeader(title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(WorkSocialColors.WorkHouseStart, WorkSocialColors.WorkHouseEnd))), color = Color.Transparent, shape = WorkSocialShapes.Card) { Row(modifier = Modifier.padding(WorkSocialSpacing.xl)) { Text(title, style = WorkSocialTypography.title.copy(color = Color.White)) } }
}
