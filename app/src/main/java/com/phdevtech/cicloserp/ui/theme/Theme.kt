package com.phdevtech.cicloserp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandContainer,
    onPrimaryContainer = OnBrandContainer,
    secondary = Color(0xFF775656),
    tertiary = Color(0xFF755A2F),
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFF680014),
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = BrandContainer,
    secondary = Color(0xFFE7BDBC),
    tertiary = Color(0xFFE5C18D),
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
