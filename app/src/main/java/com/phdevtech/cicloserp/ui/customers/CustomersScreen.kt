package com.phdevtech.cicloserp.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdevtech.cicloserp.ui.common.AddButton
import com.phdevtech.cicloserp.ui.common.BackBar
import com.phdevtech.cicloserp.ui.common.Badge
import com.phdevtech.cicloserp.ui.common.Col
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.PageHeader
import com.phdevtech.cicloserp.ui.common.TRow
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.WebTable
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt

/** Clientes (GET customers): título, "Novo cliente" e o cartão de busca com a tabela do sistema web. */
@Composable
fun CustomersScreen(onOpen: (Int) -> Unit, pad: PaddingValues, canAdd: Boolean = false, onNew: () -> Unit = {}) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var inactive by rememberSaveable { mutableStateOf(false) }
    val loader = remember { Loader(scope) { repo.customers(query, inactive) } }
    LaunchedEffect(query, inactive) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize().padding(pad)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PageHeader("Clientes", "Cadastre pelo CNPJ e os dados vêm da Receita Federal.") }
            if (canAdd) item { AddButton("Novo cliente", onNew) }
            item {
                WebCard(Modifier.padding(horizontal = 16.dp)) {
                    WebField(query, { query = it }, Modifier.fillMaxWidth(), label = "Buscar", placeholder = "Nome, CNPJ/CPF, cidade, telefone ou e-mail")
                    LabeledCheck(inactive, { inactive = it }, "Mostrar inativos", Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    when (val s = loader.state) {
                        Load.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Web.accent) }
                        is Load.Error -> Text(s.message, color = Web.danger)
                        is Load.Ok -> WebTable(
                            listOf(Col("Cliente", 170.dp), Col("CNPJ / CPF", 150.dp), Col("Cidade", 130.dp), Col("Telefone", 130.dp), Col("Compras", 90.dp, num = true), Col("Última compra", 120.dp)),
                            s.data.map { c ->
                                TRow(c.id, listOf(c.name, c.document?.let(Fmt::doc).orEmpty(), c.cityState.orEmpty(), c.phone.orEmpty(), "${c.salesCount}", c.lastSale?.let(Fmt::date).orEmpty()),
                                    { onOpen(c.id) }, badgeAfterFirst = if (c.active) null else "inativo")
                            },
                            empty = "Nenhum cliente encontrado.",
                        )
                    }
                }
            }
        }
    }
}

/** Ficha do cliente (GET customer + equipments): voltar, nome, ações, totais, dados, o que mais compra, portal e equipamentos. */
@Composable
fun CustomerScreen(id: Int, onBack: () -> Unit, canEdit: Boolean = false, onEdit: () -> Unit = {}, canAddEquipment: Boolean = false, onNewEquipment: () -> Unit = {}) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    val loader = remember(id) { Loader(scope) { repo.customer(id) } }
    androidx.lifecycle.compose.LifecycleResumeEffect(id) { loader.load(); onPauseOrDispose { } }
    Scaffold(containerColor = Web.bg, topBar = { BackBar("Cliente", onBack) }) { pad ->
        Box(Modifier.padding(pad)) {
            PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
                when (val s = loader.state) {
                    Load.Loading -> LoadingBox()
                    is Load.Error -> ErrorBox(s.message, { loader.load() })
                    is Load.Ok -> {
                        val d = s.data
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text("← Clientes", color = Web.accent, fontSize = 14.sp)
                                Text(d.customer.name, color = Web.text, fontSize = 21.75.sp, fontWeight = FontWeight(650))
                                Text(d.personType, color = Web.muted, fontSize = 14.sp)
                            }
                            if (canEdit) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { WebButton("Editar", onEdit) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Kpi("Total comprado", Fmt.money(d.stats.total), "${d.stats.count} compra(s)", Modifier.weight(1f))
                                Kpi("Lucro gerado", Fmt.money(d.stats.profit), null, Modifier.weight(1f), valueColor = Web.ok)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Kpi("Ticket médio", Fmt.money(d.stats.ticket), null, Modifier.weight(1f))
                                Kpi("Última compra", d.stats.lastSale?.let(Fmt::date) ?: "—", null, Modifier.weight(1f))
                            }
                            WebCard {
                                Text("Dados", fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                Spacer(Modifier.height(6.dp))
                                Info("CNPJ/CPF", d.customer.document?.let(Fmt::doc))
                                Info("Telefone", d.customer.phone)
                                Info("E-mail", d.customer.email)
                                Info("Endereço", d.address)
                                Info("CEP", d.cep)
                                Info("Observações", d.notes)
                            }
                            WebCard {
                                Text("O que mais compra", fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                Spacer(Modifier.height(6.dp))
                                if (d.topProducts.isEmpty()) Text("Nenhuma compra ainda.", color = Web.muted)
                                d.topProducts.forEach {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        Text(it.name, Modifier.weight(1f), color = Web.text)
                                        Text(Fmt.money(it.total), color = Web.muted, fontSize = 13.8.sp)
                                    }
                                }
                            }
                            d.portalStatus?.let { p ->
                                WebCard {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Portal do cliente", Modifier.padding(end = 8.dp), fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                        Badge(p, "warn")
                                    }
                                }
                            }
                            WebCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Equipamentos (${d.equipments.size})", Modifier.weight(1f), fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                    if (canAddEquipment) WebButton("Novo equipamento", onNewEquipment)
                                }
                                Spacer(Modifier.height(8.dp))
                                WebTable(
                                    listOf(Col("Equipamento", 170.dp), Col("Identificador", 140.dp), Col("Garantia até", 120.dp)),
                                    d.equipments.map { e ->
                                        TRow(e.id, listOf(e.name, e.serial.orEmpty(), e.warrantyUntil?.let(Fmt::date).orEmpty()),
                                            subs = listOfNotNull(e.brand, e.model, e.category).joinToString(" · ").let { if (it.isBlank()) emptyMap() else mapOf(0 to it) })
                                    },
                                    empty = "Nenhum equipamento.",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: String, sub: String?, modifier: Modifier, valueColor: Color = Web.text) {
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.background(Web.surface, shape).border(1.dp, Web.line, shape).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(label, color = Web.muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight(650), maxLines = 1)
        if (sub != null) Text(sub, color = Web.muted, fontSize = 12.sp)
    }
}

@Composable
private fun Info(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.padding(end = 12.dp).width(96.dp), color = Web.muted, fontSize = 13.8.sp)
        Text(value, Modifier.weight(1f), color = Web.text, fontSize = 14.4.sp, fontWeight = FontWeight.Medium)
    }
}
