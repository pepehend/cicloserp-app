package com.phdevtech.cicloserp.ui.order

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.OrderDetail
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SectionTitle
import com.phdevtech.cicloserp.ui.common.StatusChip
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.theme.StatusDone
import com.phdevtech.cicloserp.ui.theme.StatusPaused
import com.phdevtech.cicloserp.util.Fmt

/** Motivos sugeridos para a pausa (os mesmos do sistema web). */
private val PAUSE_REASONS = listOf("Almoço", "Fim do expediente", "Aguardando peça", "Aguardando cliente", "Deslocamento para outra tarefa")

/**
 * O que o usuário pode alterar na OS. No celular, depois do check-out tudo fica só para consulta
 * (como no app do técnico web); alterar só pela versão completa.
 */
fun canChange(me: Me, o: OrderDetail) = !o.isDone && (me.can("ordens.execute") || me.can("ordens.edit"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    me: Me,
    id: Int,
    onBack: () -> Unit,
    onQuestionnaire: (Int) -> Unit,
    onReport: () -> Unit,
    onPhotos: () -> Unit,
) {
    val vm = appViewModel(key = "os$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LifecycleResumeEffect(id) { vm.load(); onPauseOrDispose { } }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OS #$id") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
                actions = { IconButton(onClick = vm::load) { Icon(Icons.Outlined.Refresh, "Atualizar") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            (state as? Load.Ok)?.data?.let { o ->
                if (me.can("ordens.execute")) ActionBar(o, busy, onStatus = { to, reason -> vm.setStatus(to, reason) }, onShowPending = { tab = 2 })
            }
        },
    ) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                Column(Modifier.fillMaxSize().padding(pad)) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Header(o)
                    PrimaryTabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Detalhes") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Relatório") })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(if (o.pending.isEmpty()) "Pendências" else "Pendências (${o.pending.size})") })
                    }
                    when (tab) {
                        0 -> DetailsTab(o)
                        1 -> ReportTab(me, o, onQuestionnaire, onReport, onPhotos)
                        else -> PendingTab(o)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(o: OrderDetail) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(o.customerName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(o.status, o.statusLabel)
        }
        Text(
            listOfNotNull(o.taskTypeName, o.scheduledAt?.let { "Agendada para ${Fmt.dateTime(it)}" } ?: "Sem agendamento").joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DetailsTab(o: OrderDetail) {
    val ctx = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item { SectionTitle("Serviço solicitado") }
        item { Text(o.description, style = MaterialTheme.typography.bodyLarge) }
        o.notes?.let { item { Info("Orientação", it) } }
        item { SectionTitle("Cliente") }
        item { Text(o.customerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) }
        o.customerPhone?.let { phone ->
            item {
                LinkRow(Icons.Outlined.Phone, phone) { ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri())) }
            }
        }
        o.customerAddress?.let { addr ->
            item {
                LinkRow(Icons.Outlined.Place, addr) {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, "geo:0,0?q=${android.net.Uri.encode(addr)}".toUri()))
                }
            }
        }
        o.customerEmail?.let { item { Info("E-mail", it) } }
        item { SectionTitle("Atendimento") }
        o.equipment?.let { item { Info("Equipamento", it) } }
        item { Info("Técnico", o.technicianName ?: "Sem técnico") }
        o.dueDate?.let { item { Info("Prazo", Fmt.date(it)) } }
        o.startedAt?.let { item { Info("Iniciada em", Fmt.dateTime(it)) } }
        o.finishedAt?.let { item { Info("Concluída em", Fmt.dateTime(it)) } }
        if (o.hours.isNotEmpty()) {
            item { SectionTitle("Controle de horas") }
            item {
                Text(
                    "Deslocamento ${Fmt.duration(o.hoursTravel)} · Trabalho ${Fmt.duration(o.hoursWork)} · Pausa ${Fmt.duration(o.hoursPause)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(o.hours) { h ->
                Text(
                    "${Fmt.dateTime(h.at)} — ${h.label}" + (h.reason?.let { " ($it)" } ?: "") + (h.userName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (o.items.isNotEmpty()) {
            item { SectionTitle("Peças e serviços") }
            items(o.items, key = { it.id }) { i ->
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(i.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            (o.itemKinds[i.kind] ?: i.kind) + " · ${i.quantity.toString().removeSuffix(".0")} × ${Fmt.money(i.unitPrice)}" + if (i.review) " · aguardando revisão" else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Fmt.money(i.total), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("Total", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(Fmt.money(o.itemsTotal), fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Info(label: String, value: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LinkRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(text, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Relatório: o checklist do app do técnico (fotos, questionários, relato/km e assinatura). */
@Composable
private fun ReportTab(me: Me, o: OrderDetail, onQuestionnaire: (Int) -> Unit, onReport: () -> Unit, onPhotos: () -> Unit) {
    val locked = !canChange(me, o)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (locked) item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (o.isDone) "OS concluída: no celular o relatório fica só para consulta. Para alterar, use a versão completa (navegador)."
                        else "Você não tem permissão para preencher o relatório desta OS.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        item {
            CheckRow(Icons.Outlined.PhotoCamera, "Fotos", if (o.photos.isEmpty()) "Nenhuma foto" else "${o.photos.size} foto(s)", o.photos.isNotEmpty(), onPhotos)
        }
        items(o.questionnaires, key = { it.id }) { q ->
            val sub = when {
                q.missing.isNotEmpty() && q.answered -> "${q.filled} de ${q.total} · faltam obrigatórias"
                q.answered -> "${q.filled} de ${q.total} respondidas"
                else -> "Não respondido"
            }
            CheckRow(Icons.Outlined.Quiz, q.name, sub, q.answered && q.missing.isEmpty()) { onQuestionnaire(q.id) }
        }
        item {
            val parts = listOfNotNull(o.report?.let { "Relato preenchido" }, o.km?.let { "Km ${it.removeSuffix(".00")}" })
            CheckRow(Icons.Outlined.Description, "Relato e quilometragem", parts.joinToString(" · ").ifEmpty { "Não preenchido" }, o.report != null, onReport)
        }
        item {
            CheckRow(Icons.Outlined.Draw, "Assinatura", o.signerName?.let { "Assinado por $it" } ?: if (o.signature != null) "Assinado" else "Sem assinatura", o.signature != null, onReport)
        }
    }
}

@Composable
private fun CheckRow(icon: ImageVector, title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (done) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null,
                tint = if (done) StatusDone else MaterialTheme.colorScheme.outline,
            )
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PendingTab(o: OrderDetail) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (o.pending.isEmpty()) item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, null, tint = StatusDone)
                Spacer(Modifier.width(8.dp))
                Text(if (o.isDone) "OS concluída." else "Nenhuma pendência: a OS já pode ser concluída (check-out).")
            }
        }
        items(o.pending) { p ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.WarningAmber, null, tint = StatusPaused, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(p, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (o.itemsReview > 0) item {
            Text("${o.itemsReview} item(ns) lançados na OS aguardam revisão do cadastro.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Barra com A caminho / Check-in / Pausar / Retomar / Check-out, conforme as transições permitidas da OS. */
@Composable
private fun ActionBar(o: OrderDetail, busy: Boolean, onStatus: (String, String?) -> Unit, onShowPending: () -> Unit) {
    val actions = o.transitions.filter { it in listOf(OrderStatus.A_CAMINHO, OrderStatus.EM_EXECUCAO, OrderStatus.PAUSADA, OrderStatus.CONCLUIDA) }
    if (actions.isEmpty()) return
    var pauseDialog by remember { mutableStateOf(false) }
    var checkoutDialog by remember { mutableStateOf(false) }
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.forEach { to ->
                val (label, icon) = when (to) {
                    OrderStatus.A_CAMINHO -> "A caminho" to Icons.Outlined.DirectionsCar
                    OrderStatus.EM_EXECUCAO -> (if (o.status == OrderStatus.PAUSADA) "Retomar" else "Check-in") to
                        (if (o.status == OrderStatus.PAUSADA) Icons.Outlined.PlayArrow else Icons.AutoMirrored.Outlined.Login)
                    OrderStatus.PAUSADA -> "Pausar" to Icons.Outlined.Pause
                    else -> "Check-out" to Icons.AutoMirrored.Outlined.Logout
                }
                val primary = to == OrderStatus.EM_EXECUCAO || to == OrderStatus.CONCLUIDA
                val click = {
                    when (to) {
                        OrderStatus.PAUSADA -> pauseDialog = true
                        OrderStatus.CONCLUIDA -> if (o.pending.isNotEmpty()) onShowPending() else checkoutDialog = true
                        else -> onStatus(to, null)
                    }
                }
                if (primary) Button(onClick = click, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) { BarContent(icon, label) }
                else OutlinedButton(onClick = click, enabled = !busy, modifier = Modifier.weight(1f).height(52.dp)) { BarContent(icon, label) }
            }
        }
    }
    if (pauseDialog) PauseDialog(onDismiss = { pauseDialog = false }, onConfirm = { pauseDialog = false; onStatus(OrderStatus.PAUSADA, it) })
    if (checkoutDialog) AlertDialog(
        onDismissRequest = { checkoutDialog = false },
        title = { Text("Fazer check-out?") },
        text = { Text("A OS será concluída. Depois disso, no celular ela fica só para consulta.") },
        confirmButton = { TextButton(onClick = { checkoutDialog = false; onStatus(OrderStatus.CONCLUIDA, null) }) { Text("Concluir") } },
        dismissButton = { TextButton(onClick = { checkoutDialog = false }) { Text("Voltar") } },
    )
}

@Composable
private fun BarContent(icon: ImageVector, label: String) {
    Icon(icon, null, Modifier.size(20.dp))
    Spacer(Modifier.width(6.dp))
    Text(label)
}

@Composable
private fun PauseDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pausar atendimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = reason, onValueChange = { reason = it.take(120) }, label = { Text("Motivo da pausa") }, singleLine = true)
                PAUSE_REASONS.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { r -> SuggestionChip(onClick = { reason = r }, label = { Text(r) }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }) { Text("Pausar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
