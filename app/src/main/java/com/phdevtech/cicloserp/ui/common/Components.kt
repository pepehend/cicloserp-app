package com.phdevtech.cicloserp.ui.common
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.data.model.OrderSummary
import com.phdevtech.cicloserp.ui.theme.StatusDone
import com.phdevtech.cicloserp.ui.theme.StatusOpen
import com.phdevtech.cicloserp.ui.theme.StatusPaused
import com.phdevtech.cicloserp.ui.theme.StatusRunning
import com.phdevtech.cicloserp.ui.theme.StatusScheduled
import com.phdevtech.cicloserp.util.Fmt

/** Estado de uma tela que carrega dados da API. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<T>(val data: T) : Load<T>
    data class Error(val message: String) : Load<Nothing>
}

fun statusColor(status: String): Color = when (status) {
    OrderStatus.CONCLUIDA -> StatusDone
    OrderStatus.PAUSADA -> StatusPaused
    OrderStatus.EM_EXECUCAO, OrderStatus.A_CAMINHO -> StatusRunning
    OrderStatus.AGENDADA -> StatusScheduled
    else -> StatusOpen
}

@Composable
fun StatusChip(status: String, label: String, modifier: Modifier = Modifier) {
    val c = statusColor(status)
    Row(
        modifier
            .background(c.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(c, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.padding(6.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (onRetry != null) {
            Spacer(Modifier.padding(8.dp))
            Button(onClick = onRetry) { Text("Tentar de novo") }
        }
    }
}

@Composable
fun EmptyBox(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * Cartão de uma OS nas listas (.tk-task): horário e dia à esquerda com faixa, cliente e situação, tipo/descrição e as etiquetas
 * "ID: n", "N pendência(s)" (amarela, só se a OS não está concluída) e o técnico quando é de outra pessoa.
 */
@Composable
fun OrderCard(o: OrderSummary, onClick: () -> Unit, showTechnician: Boolean = false, showDate: Boolean = false) {
    val shape = RoundedCornerShape(14.dp)
    val dt = Fmt.parse(o.scheduledAt)
    val day = when {
        dt == null -> ""
        dt.toLocalDate() == java.time.LocalDate.now() -> "HOJE"
        else -> "%02d/%02d".format(dt.dayOfMonth, dt.monthValue)
    }
    val (stBg, stFg) = when (o.status) {
        OrderStatus.CONCLUIDA -> com.phdevtech.cicloserp.ui.theme.Web.okSoft to com.phdevtech.cicloserp.ui.theme.Web.ok
        OrderStatus.EM_EXECUCAO, OrderStatus.A_CAMINHO -> Color(0xFF2563EB).copy(alpha = 0.14f) to Color(0xFF2563EB)
        OrderStatus.PAUSADA -> com.phdevtech.cicloserp.ui.theme.Web.warnSoft to com.phdevtech.cicloserp.ui.theme.Web.warn
        OrderStatus.AGENDADA -> com.phdevtech.cicloserp.ui.theme.Web.accentSoft to com.phdevtech.cicloserp.ui.theme.Web.accent
        else -> com.phdevtech.cicloserp.ui.theme.Web.surface2 to com.phdevtech.cicloserp.ui.theme.Web.muted
    }
    val web = com.phdevtech.cicloserp.ui.theme.Web
    Row(
        Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).clip(shape).background(web.surface).border(1.dp, web.line, shape).clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.width(52.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text(if (dt != null) Fmt.time(o.scheduledAt) else "--:--", color = web.text, fontSize = 15.75.sp, fontWeight = FontWeight.Bold)
            Text(if (showDate && dt != null && day != "HOJE") Fmt.date(o.scheduledAt).take(5) else day, color = web.muted, fontSize = 9.9.sp, letterSpacing = 0.4.sp)
        }
        Box(Modifier.width(3.dp).fillMaxHeight().background(web.line))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Text(o.customer, Modifier.weight(1f), color = web.text, fontSize = 14.7.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(o.statusLabel.lowercase(), Modifier.clip(RoundedCornerShape(50)).background(stBg).padding(horizontal = 9.dp, vertical = 2.dp), color = stFg, fontSize = 11.2.sp, maxLines = 1)
            }
            val sub = o.taskTypeName ?: o.description
            if (sub.isNotBlank()) Text(sub, color = web.muted, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Text("ID: ${o.id}", Modifier.clip(RoundedCornerShape(50)).background(web.surface2).border(1.dp, web.line, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 2.dp), color = web.muted, fontSize = 11.2.sp)
                if (o.pending > 0 && o.status != OrderStatus.CONCLUIDA) {
                    Text("${o.pending} pendência${if (o.pending > 1) "s" else ""}", Modifier.clip(RoundedCornerShape(50)).background(web.warnSoft).padding(horizontal = 9.dp, vertical = 2.dp), color = web.warn, fontSize = 11.2.sp)
                }
                if (showTechnician && o.technicianName != null) {
                    Text(o.technicianName, Modifier.clip(RoundedCornerShape(50)).background(web.surface2).border(1.dp, web.line, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 2.dp), color = web.muted, fontSize = 11.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = com.phdevtech.cicloserp.ui.theme.Web.accentText)
}

/** Campo de busca das listas. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(hint) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = { if (value.isNotEmpty()) androidx.compose.material3.IconButton(onClick = { onChange("") }) { Icon(Icons.Outlined.Close, "Limpar busca") } },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Linha "rótulo: valor" das fichas. */
@Composable
fun InfoRow(label: String, value: String?, modifier: Modifier = Modifier) {
    if (value.isNullOrBlank()) return
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Cartão numérico (totais do cliente, indicadores de orçamento). */
@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    AppCard(m) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Cartão com título, usado nas seções das fichas. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.padding(3.dp))
            content()
        }
    }
}

/** Barra de topo das telas internas (voltar + título). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, onBack: () -> Unit) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { androidx.compose.material3.IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
    )
}

/** Carrega uma vez ao abrir e de novo quando a [key] muda; devolve o estado e o recarregamento. */
class Loader<T>(private val scope: kotlinx.coroutines.CoroutineScope, private val fetch: suspend () -> T) {
    var state by androidx.compose.runtime.mutableStateOf<Load<T>>(Load.Loading)
    var refreshing by androidx.compose.runtime.mutableStateOf(false)
    private var job: kotlinx.coroutines.Job? = null
    fun load(pull: Boolean = false, debounceMs: Long = 0) {
        job?.cancel()
        if (pull) refreshing = true
        job = scope.launch {
            if (debounceMs > 0) kotlinx.coroutines.delay(debounceMs)
            state = try { Load.Ok(fetch()) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) {
                Load.Error(e.message ?: "Erro ao carregar")
            }
            refreshing = false
        }
    }
}

/** Cartão do app do técnico (.tk-card/.tk-kpi): superfície, borda --line, raio 14. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier,
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = com.phdevtech.cicloserp.ui.theme.Web.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor ?: com.phdevtech.cicloserp.ui.theme.Web.line),
        content = content,
    )
}

/** Título da página (h1 1,45rem) com o texto de apoio em cinza. */
@Composable
fun PageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight(650))
        if (subtitle != null) Text(subtitle, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyLarge, color = com.phdevtech.cicloserp.ui.theme.Web.muted)
    }
}

/** Quadradinho com ícone e fundo na cor a 14% (.tk-si, .tk-dli). */
@Composable
fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 38.dp, radius: androidx.compose.ui.unit.Dp = 10.dp) {
    Box(modifier.size(size).background(color.copy(alpha = 0.14f), RoundedCornerShape(radius)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

/** Caixa de seleção do sistema (accent-color vermelho) com texto ao lado. */
@Composable
fun LabeledCheck(checked: Boolean, onChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.clickable { onChange(!checked) }.padding(end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(
            checked, null,
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = com.phdevtech.cicloserp.ui.theme.Web.accent, checkmarkColor = com.phdevtech.cicloserp.ui.theme.Web.accentInk,
                uncheckedColor = com.phdevtech.cicloserp.ui.theme.Web.muted,
            ),
        )
        Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Cartão "Buscar": rótulo, campo e filtros (conteúdo extra) — o .card com o campo de busca das listas. */
@Composable
fun SearchCard(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    WebCard(modifier.padding(horizontal = 16.dp)) {
        WebField(value, onChange, Modifier.fillMaxWidth(), label = "Buscar", placeholder = hint)
        extra()
    }
}

/** Botão vermelho "+ Novo ..." do topo das listas. */
@Composable
fun AddButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    WebButton(label, onClick, modifier.padding(horizontal = 16.dp, vertical = 6.dp), primary = true, icon = Icons.Outlined.Add)
}