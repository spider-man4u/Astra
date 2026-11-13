package com.spider.astra.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spider.astra.assistant.core.AssistantState

@Composable
fun AstraOrb(
    state: AssistantState = AssistantState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    glowEnabled: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "astra-orb")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1500), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )
    val color = when (state) {
        AssistantState.IDLE -> Color(0xFF22D3EE)
        AssistantState.LISTENING -> Color(0xFF3B82F6)
        AssistantState.THINKING, AssistantState.PLANNING -> Color(0xFF8B5CF6)
        AssistantState.EXECUTING -> Color(0xFFF59E0B)
        AssistantState.WAITING_FOR_PERMISSION -> Color(0xFFF59E0B)
        AssistantState.COMPLETED -> Color(0xFF10B981)
        AssistantState.ERROR -> Color(0xFFF87171)
    }

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension * 0.31f * pulse
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        if (glowEnabled) {
            drawCircle(color.copy(alpha = 0.12f), radius = radius * 1.9f, center = center)
            drawCircle(color.copy(alpha = 0.20f), radius = radius * 1.45f, center = center)
        }
        drawCircle(color, radius = radius, center = center)
        drawCircle(Color.White.copy(alpha = 0.45f), radius = radius * 0.17f,
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f))
        if (state != AssistantState.IDLE) {
            drawCircle(color.copy(alpha = 0.75f), radius = radius * 1.55f, center = center,
                style = Stroke(width = 3.dp.toPx()))
        }
    }
}