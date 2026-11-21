package com.phdevtech.cicloserp.ui.quotes
import com.phdevtech.cicloserp.ui.common.SearchCard
import com.phdevtech.cicloserp.ui.common.PageHeader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.QuoteRow
import com.phdevtech.cicloserp.data.model.QuotesPage
import com.phdevtech.cicloserp.ui.common.AppCard
import com.phdevtech.cicloserp.ui.common.BackBar
import com.phdevtech.cicloserp.ui.common.AppCard
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.InfoRow
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SearchField
import com.phdevtech.cicloserp.ui.common.SectionCard
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.util.Fmt

/** Cores dos status de sistema (QuoteStatuses::SYSTEM no servidor). */
private fun quoteColor(code: String): Color = when (code) {
    "rascunho" -> Color(0xFF65A30D)
    "em_elaboracao" -> Color(0xFF0284C7)
    "aguardando_envio" -> Color(0xFF7C3AED)
    "enviado" -> Color(0xFFD97706)
    "aprovado" -> Color(0xFF15803D)
    "recusado" -> Color(0xFFDC2626)
    "vendido" -> Color(0xFF475569)
    else -> Color(0xFF6C757D)
}

/** Orçamentos (GET quotes): indicadores do mês, busca, filtro por status e visão em Lista ou Kanban. */
@Composable
fun QuotesScreen(onOpen: (Int) -> Unit, pad: PaddingValues) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var kanban by rememberSaveable { mutableStateOf(false) }
    val loader = remember { Loader(scope) { repo.quotes(query, status) } }
    LaunchedEffect(query, status) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize().padding(pad)) {
        when (val s = loader.state) {
            Load.Loading -> LoadingBox()
            is Load.Error -> ErrorBox(s.message, { loader.load() })
            is Load.Ok -> {
                val page = s.data
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { PageHeader("Orçamentos") }
                    item { Indicators(page, status) { status = if (status == it) "" else it } }
                    item { SearchCard(query, { query = it }, "Nº, cliente, lead ou código externo") }
                    item {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            SegmentedButton(!kanban, { kanban = false }, SegmentedButtonDefaults.itemShape(0, 2), colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)) { Text("Lista") }
                            SegmentedButton(kanban, { kanban = true }, SegmentedButtonDefaults.itemShape(1, 2), colors = SegmentedButtonDefaults.colors(activeContainerColor = MaterialTheme.colorScheme.primaryContainer, activeContentColor = MaterialTheme.colorScheme.primary)) { Text("Kanban") }
                        }
                    }
                    item {
                        Text("Total: ${Fmt.money(page.sum)} · Orçamentos: ${page.total}", Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    if (page.rows.isEmpty()) item { EmptyBox("Nenhum orçamento encontrado.") }
                    else if (!kanban) items(page.rows, key = { it.id }) { q -> QuoteCard(q, page, Modifier.padding(horizontal = 16.dp)) { onOpen(q.id) } }
                    else item { Kanban(page, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun Indicators(page: QuotesPage, selected: String, onFilter: (String) -> Unit) {
    val m = page.summary
    // Cada indicador filtra a lista pelo status correspondente (os "do mês" mostram o total do mês)
    val items = listOf(
        Triple("Pendentes de envio", m.pendentesEnvio, "aguardando_envio") to Color(0xFF7C3AED),
        Triple("Aguardando aprovação", m.aguardandoAprovacao, "enviado") to Color(0xFF2563EB),
        Triple("Expiram este mês", m.expiramMes, "") to Color(0xFFD97706),
        Triple("Recusados no mês", m.recusadosMes, "recusado") to Color(0xFFDC2626),
        Triple("Aprovados no mês", m.aprovadosMes, "aprovado") to Color(0xFF15803D),
        Triple("Aprovados aguardando venda", m.aguardandoVenda, "aprovado") to Color(0xFFD97706),
        Triple("Viraram venda no mês", m.vendidosMes, "vendido") to Color(0xFF475569),
    )
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (t, c) ->
                    val (label, n, code) = t
                    AppCard(Modifier.weight(1f).let { if (code.isNotEmpty()) it.clickable { onFilter(code) } else it }, borderColor = c) {
                        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text("$n", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c)
                            Text(label, style = MaterialTheme.typography.bodySmall, color = if (code.isNotEmpty() && code == selected) c else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuoteCard(q: QuoteRow, page: QuotesPage, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text("#${q.id}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Box(Modifier.weight(1f))
                val c = quoteColor(q.status)
                Text(page.statuses[q.status] ?: q.status, Modifier.background(c.copy(alpha = 0.15f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
            Text(q.customer, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row {
                Text(Fmt.money(q.total), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                q.expiresAt?.let { Text("Validade ${Fmt.date(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

/** Quadro por status (somente leitura no app; a mudança de status segue pelo sistema web). */
@Composable
private fun Kanban(page: QuotesPage, onOpen: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        page.statuses.forEach { (code, name) ->
            val rows = page.rows.filter { it.status == code }
            val c = quoteColor(code)
            Column(Modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(Modifier.fillMaxWidth().background(c, RoundedCornerShape(8.dp)).padding(10.dp)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("${Fmt.money(rows.sumOf { it.total })} · ${rows.size}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                if (rows.isEmpty()) Text("Nenhum orçamento", Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                rows.forEach { QuoteCard(it, page) { onOpen(it.id) } }
            }
        }
    }
}

/** Orçamento completo (GET quote): cliente, itens, custos adicionais e total. */
@Composable
fun QuoteScreen(id: Int, onBack: () -> Unit) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    val loader = remember(id) { Loader(scope) { repo.quote(id) } }
    LaunchedEffect(id) { loader.load() }
    Scaffold(topBar = { BackBar("Orçamento #$id", onBack) }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (val s = loader.state) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(s.message, { loader.load() })
                is Load.Ok -> {
                    val q = s.data
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text(q.customer, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(q.statusLabel, color = quoteColor(q.status))
                        }
                        item {
                            SectionCard("Dados") {
                                InfoRow("Telefone", q.customerPhone)
                                InfoRow("E-mail", q.customerEmail)
                                InfoRow("Data", q.requestDate?.let(Fmt::date))
                                InfoRow("Validade", q.expiresAt?.let(Fmt::date))
                                InfoRow("Responsável", q.userName)
                                InfoRow("Observações", q.notes)
                            }
                        }
                        item {
                            SectionCard("Itens (${q.items.size})") {
                                q.items.forEach {
                                    Column(Modifier.padding(vertical = 4.dp)) {
                                        Text(it.description)
                                        Text("${it.quantity.let { n -> if (n % 1.0 == 0.0) n.toLong().toString() else n.toString() }} ${it.unit.orEmpty()} × ${Fmt.money(it.unitPrice)} = ${Fmt.money(it.total)}",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                q.extras.forEach { (d, v) -> InfoRow(d, Fmt.money(v)) }
                            }
                        }
                        item { Text("Total: ${Fmt.money(q.total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
