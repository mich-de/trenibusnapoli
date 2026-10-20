package com.eavorari.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eavorari.ui.theme.Amber

@Composable
fun SplitFlapDigit(
    char: Char,
    modifier: Modifier = Modifier,
    color: Color = Amber,
    flapHeight: Dp = 56.dp,
    flapWidth: Dp = 40.dp,
) {
    val flapBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E2130),
            Color(0xFF0F111A)
        )
    )

    Box(
        modifier = modifier
            .width(flapWidth)
            .height(flapHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(flapBackground)
            .border(1.2.dp, Color(0xFF2D324D), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = char.toString(),
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 0.sp,
            ),
            color = color,
        )

        // Split line simulating physical flap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(Color(0xFF07080D))
                .align(Alignment.Center)
        )
    }
}

@Composable
fun SplitFlapText(
    text: String,
    color: Color = Amber,
    flapHeight: Dp = 56.dp,
    flapWidth: Dp = 40.dp,
    spacing: Dp = 4.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEach { char ->
            SplitFlapDigit(
                char = char,
                color = color,
                flapHeight = flapHeight,
                flapWidth = flapWidth,
            )
        }
    }
}

@Composable
fun RetroIndicator(
    isActive: Boolean = true,
    color: Color = Amber,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.8f else 0.2f,
        targetValue = if (isActive) 0.3f else 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "indicatorAlpha",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(16.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color.copy(alpha = alpha * 0.4f), RoundedCornerShape(2.dp))
            )
        }
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color.copy(alpha = if (isActive) 1f else 0.2f), RoundedCornerShape(1.5.dp))
                .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(1.5.dp)),
        )
    }
}

@Composable
fun RetroDivider(
    color: Color = Color(0xFF2A3344),
    thickness: Dp = 1.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}
