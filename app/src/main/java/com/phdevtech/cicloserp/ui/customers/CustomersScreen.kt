package com.phdevtech.cicloserp.ui.customers
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.SearchCard
import com.phdevtech.cicloserp.ui.common.PageHeader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.Customer
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
import com.phdevtech.cicloserp.ui.common.StatCard
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.StatusDone
import com.phdevtech.cicloserp.util.Fmt

/** Lista de clientes (GET customers): busca por nome, CNPJ/CPF, cidade, telefone ou e-mail. */
@Composable
fun CustomersScreen(onOpen: (Int) -> Unit, pad: PaddingValues) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var inactive by rememberSaveable { mutableStateOf(false) }
    val loader = remember { Loader(scope) { repo.customers(query, inactive) } }
    LaunchedEffect(query, inactive) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    Column(Modifier.fillMaxSize().padding(pad)) {
        PageHeader("Clientes", "Cadastre pelo CNPJ e os dados vêm da Receita Federal.")
        SearchCard(query, { query = it }, "Nome, CNPJ/CPF, cidade, telefone ou e-mail") {
            LabeledCheck(inactive, { inactive = it }, "Mostrar inativos")
        }
        Spacer(Modifier.height(8.dp))
        PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
            when (val s = loader.state) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(s.message, { loader.load() })
                is Load.Ok -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.data.isEmpty()) item { EmptyBox("Nenhum cliente encontrado.") }
                    items(s.data, key = { it.id }) { c -> CustomerCard(c) { onOpen(c.id) } }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(c: Customer, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp)) {
            Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = listOfNotNull(c.document?.let(Fmt::doc), c.cityState, c.phone).joinToString(" · ")
            if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                Text("${c.salesCount} compra(s)", style = MaterialTheme.typography.bodySmall)
                c.lastSale?.let { Text("  ·  Última: ${Fmt.date(it)}", style = MaterialTheme.typography.bodySmall) }
                if (!c.active) Text("  ·  Inativo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Ficha do cliente (GET customer + equipments): totais, dados, o que mais compra, portal e equipamentos. */
@Composable
fun CustomerScreen(id: Int, onBack: () -> Unit) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    val loader = remember(id) { Loader(scope) { repo.customer(id) } }
    LaunchedEffect(id) { loader.load() }
    Scaffold(topBar = { BackBar("Cliente", onBack) }) { pad ->
        Box(Modifier.padding(pad)) {
            PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
                when (val s = loader.state) {
                    Load.Loading -> LoadingBox()
                    is Load.Error -> ErrorBox(s.message, { loader.load() })
                    is Load.Ok -> {
                        val d = s.data
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                Text(d.customer.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                                Text(d.personType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatCard("Total comprado", Fmt.money(d.stats.total), Modifier.weight(1f))
                                    StatCard("Lucro gerado", Fmt.money(d.stats.profit), Modifier.weight(1f), StatusDone)
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatCard("Ticket médio", Fmt.money(d.stats.ticket), Modifier.weight(1f))
                                    StatCard("Última compra", d.stats.lastSale?.let(Fmt::date) ?: "—", Modifier.weight(1f))
                                }
                            }
                            item {
                                SectionCard("Dados") {
                                    InfoRow("CNPJ/CPF", d.customer.document?.let(Fmt::doc))
                                    InfoRow("Telefone", d.customer.phone)
                                    InfoRow("E-mail", d.customer.email)
                                    InfoRow("Endereço", d.address)
                                    InfoRow("CEP", d.cep)
                                    InfoRow("Observações", d.notes)
                                }
                            }
                            item {
                                SectionCard("O que mais compra") {
                                    if (d.topProducts.isEmpty()) Text("Nenhuma compra ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    d.topProducts.forEach {
                                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                            Text(it.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(Fmt.money(it.total), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            d.portalStatus?.let { p -> item { SectionCard("Portal do cliente") { Text(p) } } }
                            item {
                                SectionCard("Equipamentos (${d.equipments.size})") {
                                    if (d.equipments.isEmpty()) Text("Nenhum equipamento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    d.equipments.forEach { e ->
                                        Column(Modifier.padding(vertical = 4.dp)) {
                                            Text(e.label, fontWeight = FontWeight.Medium)
                                            e.warrantyUntil?.let { Text("Garantia até ${Fmt.date(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
