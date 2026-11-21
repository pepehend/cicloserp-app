package com.phdevtech.cicloserp.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.Product
import com.phdevtech.cicloserp.ui.common.AddButton
import com.phdevtech.cicloserp.ui.common.Col
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.PageHeader
import com.phdevtech.cicloserp.ui.common.TRow
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.WebTable
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload

private fun qty(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else "%.3f".format(v).trimEnd('0').trimEnd(',', '.')

/**
 * Produtos e serviços (GET products): título, botões, cartão "Buscar" com as opções e a tabela do sistema web.
 * Produtos: Cód. fabricante | Produto | Localização | Estoque | Custo unit. | Preço | Margem. Serviços: Serviço | Preço.
 */
@Composable
fun ProductsScreen(service: Boolean, pad: PaddingValues, canAdd: Boolean = false, onNew: () -> Unit = {}, onOpen: ((Product) -> Unit)? = null) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var inactive by rememberSaveable { mutableStateOf(false) }
    var low by rememberSaveable { mutableStateOf(false) }
    val type = if (service) "servico" else "produto"
    val loader = remember(type) { Loader(scope) { repo.products(type, query, inactive, low && !service) } }
    LaunchedEffect(type, query, inactive, low) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize().padding(pad)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                PageHeader(
                    if (service) "Serviços" else "Produtos",
                    if (service) "Serviços que você presta, sem controle de estoque." else "Produtos que você vende e controla no estoque.",
                )
            }
            if (canAdd) item {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    if (!service) WebButton("Importar planilha", {}, Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp), icon = Icons.Outlined.FileUpload, enabled = false)
                    AddButton(if (service) "Novo serviço" else "Novo produto", onNew)
                }
            }
            item {
                WebCard(Modifier.padding(horizontal = 16.dp)) {
                    WebField(query, { query = it }, Modifier.fillMaxWidth(), label = "Buscar", placeholder = if (service) "Nome do serviço" else "Nome, marca, código (SKU, fabricante ou referência)")
                    Row(Modifier.padding(top = 4.dp)) {
                        if (!service) LabeledCheck(low, { low = it }, "Abaixo do mínimo")
                        LabeledCheck(inactive, { inactive = it }, "Mostrar inativos")
                    }
                    Spacer(Modifier.height(8.dp))
                    when (val s = loader.state) {
                        Load.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Web.accent) }
                        is Load.Error -> Text(s.message, color = Web.danger)
                        is Load.Ok -> if (service) WebTable(
                            listOf(Col("Serviço", 220.dp), Col("Preço", 110.dp, num = true)),
                            s.data.map { p -> TRow(p.id, listOf(p.name, Fmt.money(p.salePrice)), onOpen?.let { { it(p) } }, badgeAfterFirst = if (p.active) null else "inativo") },
                            empty = "Nenhum serviço encontrado. Cadastre o primeiro em Novo serviço.",
                        ) else WebTable(
                            listOf(Col("Cód. fabricante", 130.dp), Col("Produto", 220.dp), Col("Localização", 130.dp), Col("Estoque", 90.dp, num = true), Col("Custo unit.", 100.dp, num = true), Col("Preço", 100.dp, num = true), Col("Margem", 80.dp, num = true)),
                            s.data.map { p ->
                                val margin = if (p.salePrice > 0) (p.salePrice - p.avgCost) / p.salePrice * 100 else null
                                TRow(
                                    p.id,
                                    listOf(p.mfrCode.orEmpty(), p.name, p.location.orEmpty(), "${qty(p.stock)} ${p.unit}", Fmt.money(p.avgCost),
                                        Fmt.money(p.salePrice), margin?.let { "%.0f%%".format(it) }.orEmpty()),
                                    onOpen?.let { { it(p) } },
                                    subs = p.markup?.let { mapOf(5 to "auto +${qty(it)}%") }.orEmpty(),
                                    warnCols = if (p.lowStock) setOf(3) else emptySet(),
                                )
                            },
                            empty = "Nenhum produto encontrado. Cadastre o primeiro em Novo produto ou importe uma nota fiscal.",
                        )
                    }
                }
            }
        }
    }
}
