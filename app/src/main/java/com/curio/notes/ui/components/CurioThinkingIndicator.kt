package com.curio.notes.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.curio.notes.R
import com.curio.notes.ui.theme.Spacing

// Curio's signature processing visual: the logo breathing inside a soft
// sky-blue glow. Calm and intelligent, never a generic spinner. A single
// InfiniteTransition drives both motions, and it only exists while
// composed (processing states), so it costs nothing at rest.
@Composable
fun CurioThinkingIndicator(
    modifier: Modifier = Modifier,
    logoSize: Dp = 72.dp
) {
    val transition = rememberInfiniteTransition(label = "curio-thinking")
    val breathe by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    Box(
        modifier = modifier.size(logoSize * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(logoSize * 2.2f)
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                    alpha = glowAlpha
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
        Image(
            painter = painterResource(R.drawable.curio_logo),
            contentDescription = null,
            modifier = Modifier
                .size(logoSize)
                .graphicsLayer {
                    scaleX = breathe
                    scaleY = breathe
                }
        )
    }
}

// Intentional processing state: the note is saved, Curio is thinking,
// and it is safe to leave. Never a bare full-screen spinner.
@Composable
fun ProcessingIndicator(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxWidth()
    ) {
        CurioThinkingIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
