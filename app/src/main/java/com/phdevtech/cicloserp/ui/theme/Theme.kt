package com.phdevtech.cicloserp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Variáveis de cor do style.css do sistema web (:root e html[data-theme="dark"]) que o Material não tem:
 * --surface-2, --line, --muted, --ok/--ok-soft, --warn/--warn-soft, --danger/--danger-soft, --accent-soft.
 */
@Immutable
class Ciclos(
    val bg: Color, val surface: Color, val surface2: Color, val line: Color, val text: Color, val muted: Color,
    val accent: Color, val accentHover: Color, val accentInk: Color, val accentSoft: Color,
    val danger: Color, val dangerSoft: Color, val warn: Color, val warnSoft: Color, val ok: Color, val okSoft: Color,
    val dark: Boolean,
)

private val LightCiclos = Ciclos(
    bg = Color(0xFFEEF0F2), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF8F9FA), line = Color(0xFFDEE2E6),
    text = Color(0xFF212529), muted = Color(0xFF6C757D),
    accent = Color(0xFFB02A37), accentHover = Color(0xFF842029), accentInk = Color.White, accentSoft = Color(0xFFF8D7DA),
    danger = Color(0xFFC0392B), dangerSoft = Color(0xFFFDECEA), warn = Color(0xFF9A6100), warnSoft = Color(0xFFFDF3DC),
    ok = Color(0xFF1F7A3D), okSoft = Color(0xFFE5F4EA), dark = false,
)

private val DarkCiclos = Ciclos(
    bg = Color(0xFF131315), surface = Color(0xFF1C1C1F), surface2 = Color(0xFF232327), line = Color(0xFF34343A),
    text = Color(0xFFE9ECEF), muted = Color(0xFFA3A8AE),
    accent = Color(0xFFDC3545), accentHover = Color(0xFFE4606D), accentInk = Color.White, accentSoft = Color(0xFF3A1418),
    danger = Color(0xFFFF8A80), dangerSoft = Color(0xFF3D1C1C), warn = Color(0xFFE8B04B), warnSoft = Color(0xFF362A12),
    ok = Color(0xFF5FC983), okSoft = Color(0xFF173322), dark = true,
)

val LocalCiclos = staticCompositionLocalOf { LightCiclos }

/** Acesso curto aos tokens: `Web.line`, `Web.muted`… */
object Web {
    val c: Ciclos @Composable @ReadOnlyComposable get() = LocalCiclos.current
    val bg @Composable @ReadOnlyComposable get() = c.bg
    val surface @Composable @ReadOnlyComposable get() = c.surface
    val surface2 @Composable @ReadOnlyComposable get() = c.surface2
    val line @Composable @ReadOnlyComposable get() = c.line
    val text @Composable @ReadOnlyComposable get() = c.text
    val muted @Composable @ReadOnlyComposable get() = c.muted
    val accent @Composable @ReadOnlyComposable get() = c.accent
    val accentSoft @Composable @ReadOnlyComposable get() = c.accentSoft
    val accentInk @Composable @ReadOnlyComposable get() = c.accentInk
    val danger @Composable @ReadOnlyComposable get() = c.danger
    val dangerSoft @Composable @ReadOnlyComposable get() = c.dangerSoft
    val warn @Composable @ReadOnlyComposable get() = c.warn
    val warnSoft @Composable @ReadOnlyComposable get() = c.warnSoft
    val ok @Composable @ReadOnlyComposable get() = c.ok
    val okSoft @Composable @ReadOnlyComposable get() = c.okSoft
}

private fun scheme(c: Ciclos) = if (c.dark) darkColorScheme(
    primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.accentSoft, onPrimaryContainer = c.accent,
    secondary = c.ok, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
    onSurfaceVariant = c.muted, surfaceVariant = c.surface2, outline = c.muted, outlineVariant = c.line,
    error = c.danger, errorContainer = c.dangerSoft, surfaceContainerHighest = c.surface, surfaceContainer = c.surface,
    surfaceContainerLow = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLowest = c.surface, surfaceBright = c.surface,
) else lightColorScheme(
    primary = c.accent, onPrimary = c.accentInk, primaryContainer = c.accentSoft, onPrimaryContainer = c.accent,
    secondary = c.ok, background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
    onSurfaceVariant = c.muted, surfaceVariant = c.surface2, outline = Color(0xFFADB5BD), outlineVariant = c.line,
    error = c.danger, errorContainer = c.dangerSoft, surfaceContainerHighest = c.surface, surfaceContainer = c.surface,
    surfaceContainerLow = c.surface, surfaceContainerHigh = c.surface, surfaceContainerLowest = c.surface, surfaceBright = c.surface,
)

@Composable
fun CiclosERPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val c = if (darkTheme) DarkCiclos else LightCiclos
    CompositionLocalProvider(LocalCiclos provides c) {
        MaterialTheme(colorScheme = scheme(c), typography = Typography, content = content)
    }
}
