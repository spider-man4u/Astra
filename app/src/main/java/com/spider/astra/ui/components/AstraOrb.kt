package com.spider.astra.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.updateTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.linearInterpolator
import androidx.compose.animation.core.repeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.DrawScope
import androidx.compose.ui.graphics.drawscope.drawArc
import androidx.compose.ui.graphics.drawscope.drawCircle
import androidx.compose.ui.graphics.paint.DefaultPaintCap
import androidx.compose.ui.graphics.paint.Style
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LocalDensity
import com.spider.astra.assistant.core.AssistantState
import com.spider.astra.ui.theme.AstraTheme

/**
 * Central animated ASTRA orb.
 *
 * The orb pulses at rest and changes colour / behaviour with [AssistantState].
 * It is drawn entirely with Compose Canvas so there are no image assets to ship.
 */
@Composable
fun AstraOrb(
    state: AssistantState = AssistantState.IDLE,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    glowEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }

    // Current colour for the given state (recomputed whenever state changes).
    val stateColor = rememberUpdatedState(stateColorFor(state))

    // Continuous pulse value (0f .. 1f .. 0f) that drives scale and glow.
    val pulseTarget = pulseTargetFor(state)
    val pulseValue by animateFloatAsState(
        targetValue = pulseTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, delayMillis = 200),
            repeatMode = repeatMode.reverse,
            interpolator = linearInterpolator
        )
    )

    val transition = updateTransition(state, label = "OrbState")
    val glowAlpha by transition.animateFloat(
        label = "glowAlpha",
        transitionSpec = { tween(600) }
    ) { s ->
        when (s) {
            AssistantState.IDLE -> 0.35f
            AssistantState.LISTENING -> 0.7f
            AssistantState.THINKING -> 0.85f
            AssistantState.PLANNING -> 0.85f
            AssistantState.EXECUTING -> 0.9f
            AssistantState.WAITING_FOR_PERMISSION -> 0.65f
            AssistantState.COMPLETED -> 0.55f
            AssistantState.ERROR -> 0.9f
        }
    }
    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = { tween(500) }
    ) { s ->
        when (s) {
            AssistantState.IDLE -> 1.0f
            AssistantState.LISTENING -> 1.04f
            AssistantState.THINKING -> 1.08f
            AssistantState.PLANNING -> 1.08f
            AssistantState.EXECUTING -> 1.1f
            AssistantState.WAITING_FOR_PERMISSION -> 1.02f
            AssistantState.COMPLETED -> 1.05f
            AssistantState.ERROR -> 1.06f
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                drawOrb(
                    center = Offset(sizePx / 2f, sizePx / 2f),
                    radius = sizePx / 2f * scale * (1f + 0.12f * pulseValue),
                    color = stateColor.value,
                    glowAlpha = glowAlpha,
                    glowEnabled = glowEnabled,
                    pulse = pulseValue,
                    state = state
                )
            }
        )
    }
}

/** Draws the orb with glow rings, core, and state-specific sweep indicator. */
private fun DrawScope.drawOrb(
    center: Offset,
    radius: Float,
    color: Color,
    glowAlpha: Float,
    glowEnabled: Boolean,
    pulse: Float,
    state: AssistantState
) {
    val outer = radius * 1.9f
    val glow = radius * 1.45f

    if (glowEnabled && glowAlpha > 0) {
        androidx.compose.ui.graphics.drawscope.saveLayer(
            alpha = glowAlpha,
            blendMode = androidx.compose.ui.graphics.BlendMode.SrcOver
        ) {
            drawCircle(
                color = color.copy(alpha = 0.18f),
                center = center,
                radius = outer
            )
            drawCircle(
                color = color.copy(alpha = 0.10f),
                center = center,
                radius = glow
            )
            // Inner halo.
            drawCircle(
                color = color.copy(alpha = 0.30f),
                center = center,
                radius = radius * 1.12f
            )
        }
        androidx.compose.ui.graphics.drawscope.restoreLayer()
    }

    // State-specific sweep ring (indicates activity progress).
    val sweepDegrees = when (state) {
        AssistantState.IDLE -> 0f
        AssistantState.LISTENING -> 180f + (pulse * 120f)
        AssistantState.THINKING -> 360f
        AssistantState.PLANNING -> 270f + (pulse * 60f)
        AssistantState.EXECUTING -> 360f
        AssistantState.WAITING_FOR_PERMISSION -> 120f
        AssistantState.COMPLETED -> 90f
        AssistantState.ERROR -> 0f
    }

    if (sweepDegrees > 0f) {
        val strokeWidth = 6f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepDegrees,
            useCenter = false,
            style = Style.Stroke,
            topLeft = Offset(center.x - glow, center.y - glow),
            size = Size(glow * 2f, glow * 2f),
            strokeWidth = strokeWidth,
            cap = DefaultPaintCap.Round
        )
    }

    // Core orb.
    drawCircle(
        color = color,
        center = center,
        radius = radius
    )
    // Core inner bright spot.
    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f),
        radius = radius * 0.18f
    )
}

private fun stateColorFor(state: AssistantState): Color = when (state) {
    AssistantState.IDLE -> Color(0xFF22D3EE) // cyan
    AssistantState.LISTENING -> Color(0xFF3B82F6) // blue
    AssistantState.THINKING -> Color(0xFF8B5CF6) // violet
    AssistantState.PLANNING -> Color(0xFFF59E0B) // amber
    AssistantState.EXECUTING -> Color(0xFFEF4444) // red
    AssistantState.WAITING_FOR_PERMISSION -> Color(0xFFF59E0B) // amber
    AssistantState.COMPLETED -> Color(0xFF10B981) // green
    AssistantState.ERROR -> Color(0xFFF87171) // rose
}

private fun pulseTargetFor(state: AssistantState): Float = when (state) {
    AssistantState.IDLE, AssistantState.COMPLETED -> 0.3f
    AssistantState.LISTENING, AssistantState.WAITING_FOR_PERMISSION -> 0.7f
    AssistantState.THINKING, AssistantState.PLANNING, AssistantState.EXECUTING -> 1.0f
    AssistantState.ERROR -> 1.0f
}
