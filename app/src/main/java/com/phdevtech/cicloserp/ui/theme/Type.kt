package com.phdevtech.cicloserp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun t(size: Double, weight: FontWeight = FontWeight.Normal, line: Double = size * 1.4) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

/**
 * Escala do sistema web (corpo 15px = 15sp, h1 1,45rem, h2 1,1rem, h3 .95rem, pequeno .85rem), na fonte do sistema
 * (o CSS usa Inter com queda para system-ui, que no Android é a Roboto).
 */
val Typography = Typography(
    displaySmall = t(24.0, FontWeight.Bold),
    headlineLarge = t(23.0, FontWeight.Bold),
    headlineMedium = t(21.75, FontWeight.Bold, 27.0),   // h1 da página: 1,45rem
    headlineSmall = t(20.25, FontWeight.Bold, 25.0),    // h1 do app do técnico: 1,35rem
    titleLarge = t(17.6, FontWeight.Bold, 24.0),        // títulos de seção do app: 1,1rem+
    titleMedium = t(16.5, FontWeight.SemiBold, 22.0),   // h2: 1,1rem
    titleSmall = t(14.25, FontWeight.SemiBold, 20.0),   // h3: .95rem
    bodyLarge = t(15.0, line = 22.5),
    bodyMedium = t(13.8, line = 20.0),                  // .92rem (tabelas)
    bodySmall = t(12.75, line = 17.0),                  // .85rem
    labelLarge = t(14.0, FontWeight.Medium, 20.0),      // rótulo de campo .86rem w550
    labelMedium = t(12.0, FontWeight.Medium, 16.0),
    labelSmall = t(10.8, FontWeight.Medium, 14.0),      // .72rem (atalhos)
)
