package com.veigar.questtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography

private val LightColorScheme = lightColorScheme(
    primary = CoralBlue,
    onPrimary = Color.White,
    primaryContainer = CoralBlueLight,
    onPrimaryContainer = CoralBlueDark,
    secondary = CoralBlueDark,
    background = Background,
    surface = Surface,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
fun QuestTrackerTheme(
    content: @Composable () -> Unit
) {
    val appTypography = Typography()
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = appTypography,
        content = content
    )
}