package com.spider.astra.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.ui.theme.AstraPalette
import kotlin.math.cos
import kotlin.math.sin

/** Animated holographic system core. */
@Composable
fun AstraOrb(
    state: AssistantState = AssistantState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    glowEnabled: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "astra-core")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "core-pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000)),
        label = "ring-rotation"
    )
    val accent = when (state) {
        AssistantState.IDLE -> AstraPalette.Cyan
        AssistantState.LISTENING -> AstraPalette.Blue
        AssistantState.THINKING, AssistantState.PLANNING -> AstraPalette.Violet
        AssistantState.EXECUTING, AssistantState.WAITING_FOR_PERMISSION -> AstraPalette.Amber
        AssistantState.COMPLETED -> AstraPalette.Green
        AssistantState.ERROR -> AstraPalette.Red
    }

    Box(modifier = modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val unit = this.size.minDimension
            val outer = unit * 0.47f
            val core = unit * 0.255f * pulse

            if (glowEnabled) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.07f), Color.Transparent),
                        center = c,
                        radius = outer * 1.08f
                    ),
                    radius = outer * 1.08f,
                    center = c
                )
            }

            // Orbital guide rings.
            drawCircle(accent.copy(alpha = 0.10f), outer * 0.91f, c, style = Stroke(unit * 0.012f))
            drawCircle(accent.copy(alpha = 0.20f), outer * 0.77f, c, style = Stroke(unit * 0.006f))
            drawCircle(Color.White.copy(alpha = 0.08f), outer * 0.64f, c, style = Stroke(unit * 0.004f))

            // Short luminous orbit segments.
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, accent.copy(alpha = 0.95f), Color.Transparent),
                    center = c
                ),
                startAngle = rotation,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(c.x - outer * 0.88f, c.y - outer * 0.88f),
                size = Size(outer * 1.76f, outer * 1.76f),
                style = Stroke(width = unit * 0.018f, cap = StrokeCap.Round)
            )
            drawArc(
                color = AstraPalette.Blue.copy(alpha = 0.75f),
                startAngle = 180f - rotation * 0.45f,
                sweepAngle = 44f,
                useCenter = false,
                topLeft = Offset(c.x - outer * 0.77f, c.y - outer * 0.77f),
                size = Size(outer * 1.54f, outer * 1.54f),
                style = Stroke(width = unit * 0.009f, cap = StrokeCap.Round)
            )

            // Fine radial ticks around the core.
            for (i in 0 until 48) {
                val angle = Math.toRadians((i * 7.5 + rotation * 0.10).toDouble())
                val innerR = outer * if (i % 4 == 0) 0.92f else 0.95f
                val outerR = outer * 0.99f
                val start = Offset(c.x + cos(angle).toFloat() * innerR, c.y + sin(angle).toFloat() * innerR)
                val end = Offset(c.x + cos(angle).toFloat() * outerR, c.y + sin(angle).toFloat() * outerR)
                drawLine(
                    color = accent.copy(alpha = if (i % 4 == 0) 0.70f else 0.22f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 4 == 0) unit * 0.008f else unit * 0.004f,
                    cap = StrokeCap.Round
                )
            }

            // Glass-like energy sphere.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBDF9FF),
                        accent,
                        Color(0xFF087FA8),
                        Color(0xFF06182C)
                    ),
                    center = Offset(c.x - core * 0.26f, c.y - core * 0.36f),
                    radius = core * 2.3f
                ),
                radius = core,
                center = c
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.40f),
                radius = core * 0.10f,
                center = Offset(c.x - core * 0.34f, c.y - core * 0.40f)
            )
            drawCircle(
                color = accent.copy(alpha = 0.50f),
                radius = core * 1.09f,
                center = c,
                style = Stroke(width = unit * 0.008f)
            )
        }
    }
}
