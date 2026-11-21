package com.phdevtech.cicloserp.ui.common

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.phdevtech.cicloserp.R

/** Logo do CiclosERP (símbolo + nome): versão escura sobre fundo claro e versão branca sobre fundo escuro. */
@Composable
fun CiclosLogo(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Image(
        painter = painterResource(if (dark) R.drawable.ciclos_logo_dark else R.drawable.ciclos_logo),
        contentDescription = "CiclosERP",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}
