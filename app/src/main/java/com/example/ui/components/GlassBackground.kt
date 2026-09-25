package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.NeuDarkBg
import com.example.ui.theme.NeuLightBg

/**
 * Neumorphic Canvas Background.
 * Provides the soft matte tactile backdrop with subtle top-left directional illumination
 * that physically grounds all extruded and debossed elements.
 */
@Composable
fun GlassBackground(
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseBg = if (isDarkMode) NeuDarkBg else NeuLightBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isDarkMode) {
                // Top-left diffuse ambient key light (giving physics to the highlights)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x18FFFFFF),
                            Color(0x0AFFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 1.1f
                    )
                )

                // Subtle warm crimson ambient bloom in the bottom corner for brand depth
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x16FF2D55),
                            Color(0x08FF2D55),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.85f),
                        radius = width * 0.9f
                    )
                )

                // Soft bottom edge shadow gradient for depth
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x220A0C0F)
                        ),
                        startY = height * 0.7f,
                        endY = height
                    )
                )
            } else {
                // Light Mode: Clean porcelain clay canvas with soft top-left key light
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFFFFF),
                            Color(0x20FFFFFF),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 1.2f
                    )
                )

                // Soft subtle ambient rose-crimson accent tint at bottom right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x0CFF2D55),
                            Color(0x04FF2D55),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.85f, height * 0.85f),
                        radius = width * 0.95f
                    )
                )
            }
        }

        content()
    }
}
