package com.phdevtech.cicloserp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdevtech.cicloserp.ui.theme.Web

/* Componentes com o visual do style.css do sistema web (cartão .card, .btn, campos, .seg, tabela, .badge, fieldset). */

private val R8 = RoundedCornerShape(8.dp)
private val R10 = RoundedCornerShape(10.dp)

/** .card: fundo da superfície, borda --line, raio 10, respiro de 18. */
@Composable
fun WebCard(modifier: Modifier = Modifier, padding: Dp = 18.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(Web.surface, R10).border(1.dp, Web.line, R10).padding(padding), content = content)
}

/** .btn: raio 8, borda --line; `primary` = vermelho cheio; `danger` = texto vermelho. */
@Composable
fun WebButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, danger: Boolean = false,
    icon: ImageVector? = null, enabled: Boolean = true, trailing: ImageVector? = null,
) {
    val bg = if (primary) Web.accent else Web.surface
    val fg = if (primary) Web.accentInk else if (danger) Web.danger else Web.text
    val border = if (primary) Web.accent else Web.line
    Row(
        modifier.alpha(if (enabled) 1f else .55f).clip(R8).background(bg).border(1.dp, border, R8)
            .clickable(enabled = enabled, onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 15.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(16.dp), tint = fg); Spacer(Modifier.width(6.dp)) }
        Text(text, color = fg, fontSize = 14.4.sp, fontWeight = FontWeight(580), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailing != null) { Spacer(Modifier.width(6.dp)); Icon(trailing, null, Modifier.size(16.dp), tint = fg) }
    }
}

/** .tk-btn: botão em pílula do app do técnico (contorno vermelho; `filled` = vermelho cheio). */
@Composable
fun PillButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = false, icon: ImageVector? = null,
    enabled: Boolean = true, color: Color? = null,
) {
    val accent = color ?: Web.accent
    val shape = RoundedCornerShape(50)
    Row(
        modifier.alpha(if (enabled) 1f else .5f).clip(shape).background(if (filled) accent else Web.surface).border(1.5.dp, accent, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (filled) Web.accentInk else accent
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = fg); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, fontSize = 13.8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Rótulo de campo (.86rem, peso 550) com o asterisco de obrigatório. */
@Composable
fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Text(if (required) "$text *" else text, modifier.padding(bottom = 5.dp), color = Web.text, fontSize = 13.8.sp, fontWeight = FontWeight(550))
}

@Composable
fun FieldHelp(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 5.dp), color = Web.muted, fontSize = 12.sp, lineHeight = 16.sp)
}

/** Caixa de campo do sistema: raio 8, borda --line (2dp vermelha com foco, ou vermelha se `error`). */
@Composable
private fun FieldBox(modifier: Modifier, focused: Boolean, error: Boolean, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val color = if (focused || error) Web.accent else Web.line
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp).background(Web.surface, R8).border(if (focused || error) 2.dp else 1.dp, color, R8)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** Campo de texto: rótulo em cima, caixa, texto de ajuda embaixo. */
@Composable
fun WebField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "",
    help: String? = null, required: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text, minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences, error: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        if (label != null) FieldLabel(label, required)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = minLines == 1, minLines = minLines,
            textStyle = TextStyle(color = Web.text, fontSize = 15.sp, lineHeight = 21.sp),
            cursorBrush = SolidColor(Web.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                FieldBox(Modifier, focused, error) {
                    Box(Modifier.weight(1f).let { if (minLines > 1) it.heightIn(min = (21 * minLines).dp) else it }) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, color = Web.muted.copy(alpha = .75f), fontSize = 15.sp)
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
        if (help != null) FieldHelp(help)
    }
}

/** Caixa de toque (lista suspensa, data, hora, escolha de cliente): mostra o texto e um ícone à direita. */
@Composable
fun WebPickBox(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null, placeholder: String = "Selecione...",
    required: Boolean = false, icon: ImageVector = Icons.Outlined.KeyboardArrowDown, help: String? = null, content: @Composable () -> Unit = {},
) {
    Column(modifier) {
        if (label != null) FieldLabel(label, required)
        Box {
            FieldBox(Modifier.clickable(onClick = onClick), false, false) {
                Text(
                    text.ifEmpty { placeholder }, Modifier.weight(1f), color = if (text.isEmpty()) Web.muted.copy(alpha = .75f) else Web.text,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Icon(icon, null, Modifier.size(if (icon == Icons.Outlined.KeyboardArrowDown) 18.dp else 20.dp), tint = Web.muted)
            }
            content()
        }
        if (help != null) FieldHelp(help)
    }
}

/** <select>: caixa com seta e menu de opções (valor, texto). */
@Composable
fun WebSelect(
    options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String = "Selecione...", required: Boolean = false, help: String? = null,
) {
    var open by remember { mutableStateOf(false) }
    WebPickBox(options.firstOrNull { it.first == value }?.second.orEmpty(), { open = true }, modifier, label, placeholder, required, help = help) {
        DropdownMenu(open, { open = false }, containerColor = Web.surface) {
            options.forEach { (v, t) ->
                DropdownMenuItem(text = { Text(t, color = if (v == value) Web.accent else Web.text, fontWeight = if (v == value) FontWeight.SemiBold else FontWeight.Normal) },
                    onClick = { onChange(v); open = false })
            }
        }
    }
}

/** .seg: grupo de botões; o escolhido fica com fundo --accent-soft e texto vermelho. */
@Composable
fun Seg(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(R8).background(Web.surface).border(1.dp, Web.line, R8).height(IntrinsicSize.Min)) {
        options.forEachIndexed { i, t ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).background(if (on) Web.accentSoft else Color.Transparent).clickable { onSelect(i) }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t, color = if (on) Web.accent else Web.muted, fontSize = 13.8.sp, fontWeight = FontWeight(550), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (i < options.lastIndex) Box(Modifier.width(1.dp).height(40.dp).background(Web.line))
        }
    }
}

/** fieldset com legenda sobre a borda (ex.: "Endereço"). */
@Composable
fun Fieldset(title: String, modifier: Modifier = Modifier, background: Color = Web.surface, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth().padding(top = 11.dp)) {
        Column(
            Modifier.fillMaxWidth().border(1.dp, Web.line, R10).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
        )
        Text(title, Modifier.offset(x = 10.dp, y = (-11).dp).background(background).padding(horizontal = 6.dp), color = Web.text, fontWeight = FontWeight(620), fontSize = 15.sp)
    }
}

/** .badge: pílula pequena; kind = ok | warn | err | accent | neutro. */
@Composable
fun Badge(text: String, kind: String = "", modifier: Modifier = Modifier) {
    val (bg, fg) = when (kind) {
        "ok" -> Web.okSoft to Web.ok
        "warn" -> Web.warnSoft to Web.warn
        "err" -> Web.dangerSoft to Web.danger
        "accent" -> Web.accentSoft to Web.accent
        else -> Web.surface2 to Web.muted
    }
    val border = if (kind.isEmpty()) Modifier.border(1.dp, Web.line, RoundedCornerShape(50)) else Modifier
    Text(text, modifier.clip(RoundedCornerShape(50)).background(bg).then(border).padding(horizontal = 8.dp, vertical = 2.dp), color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

/** Coluna da tabela: título, largura e se é numérica (alinhada à direita). */
class Col(val title: String, val width: Dp, val num: Boolean = false)

/**
 * Linha de tabela: [cells] são os textos; [subs] = segunda linha pequena e cinza por coluna; [badges] = selo no lugar do texto
 * (texto e cor de fundo, ex.: status do orçamento); [badgeAfterFirst] = selo ao lado do primeiro texto (ex.: "inativo").
 */
class TRow(
    val key: Any, val cells: List<String>, val onClick: (() -> Unit)? = null, val subs: Map<Int, String> = emptyMap(),
    val badges: Map<Int, Pair<String, Color>> = emptyMap(), val badgeAfterFirst: String? = null, val warnCols: Set<Int> = emptySet(),
    val leading: (@Composable () -> Unit)? = null,
)

/** <table> do sistema: cabeçalho em caixa-alta cinza, linhas separadas por --line, rolagem horizontal quando passa da tela. */
@Composable
fun WebTable(columns: List<Col>, rows: List<TRow>, modifier: Modifier = Modifier, empty: String = "Nada por aqui.", leadingWidth: Dp = 0.dp) {
    val total = (columns.sumOf { it.width.value.toDouble() } + leadingWidth.value).dp
    Column(modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Row {
            if (leadingWidth > 0.dp) Box(Modifier.width(leadingWidth))
            columns.forEach { c ->
                Text(
                    c.title.uppercase(), Modifier.width(c.width).padding(horizontal = 10.dp, vertical = 8.dp), color = Web.muted, fontSize = 11.7.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp, maxLines = 1, textAlign = if (c.num) TextAlign.End else TextAlign.Start,
                )
            }
        }
        Box(Modifier.width(total).height(1.dp).background(Web.line))
        if (rows.isEmpty()) Text(empty, Modifier.width(total).padding(vertical = 28.dp), color = Web.muted, textAlign = TextAlign.Center)
        rows.forEachIndexed { r, row ->
            Row(Modifier.let { if (row.onClick != null) it.clickable(onClick = row.onClick) else it }.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                if (leadingWidth > 0.dp) Box(Modifier.width(leadingWidth), contentAlignment = Alignment.Center) { row.leading?.invoke() }
                columns.forEachIndexed { i, c ->
                    val text = row.cells.getOrElse(i) { "" }
                    Column(Modifier.width(c.width).padding(horizontal = 10.dp, vertical = 10.dp), horizontalAlignment = if (c.num) Alignment.End else Alignment.Start) {
                        val badge = row.badges[i]
                        if (badge != null) {
                            Text(badge.first, Modifier.clip(RoundedCornerShape(50)).background(badge.second).padding(horizontal = 9.dp, vertical = 2.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text.ifBlank { "—" }, Modifier.weight(1f, fill = false), color = if (text.isBlank()) Web.muted else if (i in row.warnCols) Web.danger else Web.text,
                                    fontSize = 13.8.sp, fontWeight = if (i == 0 && columns[0].title != "#") FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = if (c.num) TextAlign.End else TextAlign.Start,
                                )
                                if (i == 0 && row.badgeAfterFirst != null) { Spacer(Modifier.width(6.dp)); Badge(row.badgeAfterFirst) }
                            }
                        }
                        row.subs[i]?.let { Text(it, color = Web.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
            if (r < rows.lastIndex) Box(Modifier.width(total).height(1.dp).background(Web.line))
        }
    }
}