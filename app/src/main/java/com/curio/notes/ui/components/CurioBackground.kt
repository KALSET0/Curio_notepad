package com.curio.notes.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Curio's atmospheric background: the theme base with a very subtle
// sky-blue radiance from the top. Static brush (no animation) so it costs
// nothing at rest; screens opt in by wrapping their content, since Scaffold
// paints an opaque background by default.
//
// Dark: near-black base + soft blue glow. Light: warm paper + faint blue
// breath at the top. Same product, both intentional.
@Composable
fun CurioBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val glow = remember(scheme) {
        scheme.primary.copy(alpha = if (dark) 0.16f else 0.12f)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = scheme.background)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(glow, Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.1f
                    )
                )
            },
        content = { content() }
    )
}
