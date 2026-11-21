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
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
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
