package com.rasheed113.worksocial.presentation.ui

import androidx.compose.foundation.BorderStroke
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

/** Web-derived Work Social product tokens. See web src/app/styles.css and current premium profile/header styles. */
object WorkSocialColors {
    val Text = Color(0xFF17202A)
    val StrongText = Color(0xFF111827)
    val MutedText = Color(0xFF64748B)
    val Background = Color(0xFFF5F7FA)
    val Surface = Color.White
    val SurfaceSoft = Color(0xFFF8FAFC)
    val Border = Color(0xFFDFE5EB)
    val BorderPremium = Color(0x266366F1)
    val Primary = Color(0xFF6D5DFC)
    val PrimaryDeep = Color(0xFF5146E5)
    val Cyan = Color(0xFF22B8D4)
    val Error = Color(0xFFB4232D)
    val WorkHouseStart = Color(0xFF0A1220)
    val WorkHouseMid = Color(0xFF192337)
    val WorkHouseEnd = Color(0xFF2D2352)
    val WorkPrimaryStart = Color(0xFF6B63E4)
    val WorkPrimaryMid = Color(0xFF5148DF)
    val WorkPrimaryEnd = Color(0xFF2563EB)
}

object WorkSocialGradients {
    val Brand = Brush.linearGradient(listOf(Color(0xFF6D5DFC), Color(0xFF22B8D4), Color(0xFFFF5CA8)))
    val Header = Brush.linearGradient(listOf(WorkSocialColors.WorkHouseStart, WorkSocialColors.WorkHouseMid, WorkSocialColors.WorkHouseEnd))
    val PrimaryButton = Brush.linearGradient(listOf(WorkSocialColors.WorkPrimaryStart, WorkSocialColors.WorkPrimaryMid, WorkSocialColors.WorkPrimaryEnd))
}

object WorkSocialTypography {
    private val Sans = FontFamily.SansSerif
    val display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp, color = WorkSocialColors.StrongText, letterSpacing = (-1.2).sp)
    val title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 20.sp, lineHeight = 24.sp, color = WorkSocialColors.StrongText, letterSpacing = (-0.4).sp)
    val body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, color = WorkSocialColors.Text)
    val label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, color = WorkSocialColors.Text)
    val eyebrow = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 10.sp, lineHeight = 12.sp, color = WorkSocialColors.PrimaryDeep, letterSpacing = 1.3.sp)
}

object WorkSocialShapes {
    val Card = RoundedCornerShape(16.dp)
    val PremiumCard = RoundedCornerShape(18.dp)
    val ProfileCard = RoundedCornerShape(22.dp)
    val Button = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(999.dp)
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
fun WorkSocialCard(modifier: Modifier = Modifier, premium: Boolean = false, content: @Composable () -> Unit) {
    val shape = if (premium) WorkSocialShapes.PremiumCard else WorkSocialShapes.Card
    Card(modifier = modifier.shadow(if (premium) 8.dp else 2.dp, shape), shape = shape, colors = CardDefaults.cardColors(containerColor = if (premium) WorkSocialColors.SurfaceSoft else WorkSocialColors.Surface), border = BorderStroke(1.dp, if (premium) WorkSocialColors.BorderPremium else WorkSocialColors.Border), content = { content() })
}

@Composable
fun WorkSocialButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled, shape = WorkSocialShapes.Button, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = WorkSocialColors.PrimaryDeep)) { Text(text, style = WorkSocialTypography.label.copy(color = Color.White)) }
}

@Composable
fun WorkSocialHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(WorkSocialSpacing.xs)) { Text(title, style = WorkSocialTypography.display); subtitle?.let { Text(it, style = WorkSocialTypography.body.copy(color = WorkSocialColors.MutedText)) } }
}

@Composable
fun WorkHouseHeader(title: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = Color.Transparent, shape = WorkSocialShapes.PremiumCard) {
        Row(modifier = Modifier.background(WorkSocialGradients.Header, WorkSocialShapes.PremiumCard).padding(WorkSocialSpacing.xl)) { Text(title, style = WorkSocialTypography.title.copy(color = Color.White)) }
    }
}
