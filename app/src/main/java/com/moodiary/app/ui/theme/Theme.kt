package com.moodiary.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MoodiaryColorScheme = lightColorScheme(
    primary = MoodiaryColors.Accent,
    onPrimary = Color.White,
    primaryContainer = MoodiaryColors.AccentSoft,
    onPrimaryContainer = MoodiaryColors.AccentText,
    secondary = MoodiaryColors.TextSecondary,
    background = MoodiaryColors.Background,
    onBackground = MoodiaryColors.TextPrimary,
    surface = MoodiaryColors.Surface,
    onSurface = MoodiaryColors.TextPrimary,
    surfaceVariant = MoodiaryColors.Canvas,
    onSurfaceVariant = MoodiaryColors.TextTertiary,
    outline = MoodiaryColors.BorderStrong,
    outlineVariant = MoodiaryColors.Border,
)

/**
 * Light-only theme. The design canvas is a warm paper palette with no dark variant;
 * forcing a dynamic-color or dark scheme on it would break the whole look, so we
 * pin the scheme rather than following the system.
 */
@Composable
fun MoodiaryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MoodiaryColorScheme,
        typography = MoodiaryTypography,
        content = content,
    )
}
