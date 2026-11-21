package com.phdevtech.cicloserp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores do app do técnico do sistema web: fundo cinza-claro, cartões brancos com borda fina.
private val LightColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7D7DA),
    onPrimaryContainer = Brand,
    secondary = Color(0xFF198754),
    background = Color(0xFFEEF0F2),
    onBackground = Color(0xFF212529),
    surface = Color.White,
    onSurface = Color(0xFF212529),
    onSurfaceVariant = Color(0xFF6C757D),
    surfaceVariant = Color(0xFFE9ECEF),
    outline = Color(0xFFADB5BD),
    outlineVariant = Color(0xFFDEE2E6),
    surfaceContainerHighest = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5424F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1A20),
    onPrimaryContainer = Color(0xFFFFB3B5),
    secondary = Color(0xFF3FB27F),
    background = Color(0xFF0F0F11),
    onBackground = Color(0xFFE9E9EC),
    surface = Color(0xFF1A1A1D),
    onSurface = Color(0xFFE9E9EC),
    onSurfaceVariant = Color(0xFF9A9AA3),
    surfaceVariant = Color(0xFF26262A),
    outline = Color(0xFF6B6B73),
    outlineVariant = Color(0xFF2E2E33),
    surfaceContainerHighest = Color(0xFF1A1A1D),
)

@Composable
fun CiclosERPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
