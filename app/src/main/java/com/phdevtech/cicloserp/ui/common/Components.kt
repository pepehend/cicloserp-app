package com.phdevtech.cicloserp.ui.common

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

/** Cartão de uma OS nas listas. */
@Composable
fun OrderCard(o: OrderSummary, onClick: () -> Unit, showTechnician: Boolean = false, showDate: Boolean = false) {
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OS #${o.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                StatusChip(o.status, o.statusLabel)
            }
            Spacer(Modifier.padding(2.dp))
            Text(o.customer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(o.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (o.scheduledAt != null) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(if (showDate) Fmt.dateTime(o.scheduledAt) else Fmt.time(o.scheduledAt), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Sem agendamento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                o.taskTypeName?.let { Text("  ·  $it", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.weight(1f))
                if (o.pending > 0 && o.status != OrderStatus.CONCLUIDA) {
                    Icon(Icons.Outlined.WarningAmber, null, Modifier.size(16.dp), tint = StatusPaused)
                    Spacer(Modifier.width(2.dp))
                    Text("${o.pending}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (showTechnician && o.technicianName != null) {
                Text("Técnico: ${o.technicianName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(top = 8.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
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

/** Cartão branco com borda fina e cantos arredondados, como no sistema web. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor ?: MaterialTheme.colorScheme.outlineVariant),
        content = content,
    )
}

/** Título grande da página com texto de apoio, como "Clientes / Cadastre pelo CNPJ…". */
@Composable
fun PageHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Quadradinho colorido com ícone (atalhos do início, menu lateral, indicadores). */
@Composable
fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(modifier.size(size).background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

/** Caixa de seleção com texto ao lado. */
@Composable
fun LabeledCheck(checked: Boolean, onChange: (Boolean) -> Unit, label: String, modifier: Modifier = Modifier) {
    Row(modifier.clickable { onChange(!checked) }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(checked, onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Cartão de busca das listas: rótulo "Buscar", campo e filtros (conteúdo extra). */
@Composable
fun SearchCard(value: String, onChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier, extra: @Composable () -> Unit = {}) {
    AppCard(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Buscar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            androidx.compose.material3.OutlinedTextField(
                value = value, onValueChange = onChange, singleLine = true,
                placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
            extra()
        }
    }
}
