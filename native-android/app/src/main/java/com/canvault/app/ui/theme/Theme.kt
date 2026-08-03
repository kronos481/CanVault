package com.canvault.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

object CanVaultColors {
    val Background = Color(0xFF090B0E)
    val RaisedBackground = Color(0xFF0D1014)
    val Surface = Color(0xFF14181E)
    val RaisedSurface = Color(0xFF1A2028)
    val Mint = Color(0xFF58E4C2)
    val MintDark = Color(0xFF0B6B5A)
    val Text = Color(0xFFF5F7FA)
    val Muted = Color(0xFFAEB8C4)
    val Danger = Color(0xFFFF7B7B)
    val Warning = Color(0xFFF5BF60)
}

private val DarkColors = darkColorScheme(
    primary = CanVaultColors.Mint,
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF174E43),
    onPrimaryContainer = Color(0xFFB8F4E5),
    secondary = Color(0xFFB8C9C4),
    background = CanVaultColors.Background,
    onBackground = CanVaultColors.Text,
    surface = CanVaultColors.Surface,
    onSurface = CanVaultColors.Text,
    surfaceVariant = CanVaultColors.RaisedSurface,
    onSurfaceVariant = CanVaultColors.Muted,
    error = CanVaultColors.Danger,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B59),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF2D9),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4C635C),
    background = Color(0xFFF7FAF9),
    onBackground = Color(0xFF181C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C1B),
    surfaceVariant = Color(0xFFE2EAE7),
    onSurfaceVariant = Color(0xFF414A47),
    error = Color(0xFFBA1A1A),
)

@Composable
fun CanVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
