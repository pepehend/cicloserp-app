package com.phdevtech.cicloserp.ui.products
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.SearchCard
import com.phdevtech.cicloserp.ui.common.PageHeader

import androidx.compose.foundation.clickable
import com.phdevtech.cicloserp.ui.common.AddButton
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.Product
import com.phdevtech.cicloserp.ui.common.AppCard
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SearchField
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.util.Fmt

/** Produtos e serviços (GET products, tabela products). [service] = lista de serviços. */
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

    Column(Modifier.fillMaxSize().padding(pad)) {
        PageHeader(
            if (service) "Serviços" else "Produtos",
            if (service) "Serviços que você presta, sem controle de estoque." else "Produtos que você vende e controla no estoque.",
        )
        if (canAdd) AddButton(if (service) "Novo serviço" else "Novo produto", onNew)
        SearchCard(query, { query = it }, if (service) "Nome ou código do serviço" else "Nome, marca, código (SKU, fabricante ou referência)") {
            Row {
                if (!service) LabeledCheck(low, { low = it }, "Abaixo do mínimo")
                LabeledCheck(inactive, { inactive = it }, "Mostrar inativos")
            }
        }
        Spacer(Modifier.height(8.dp))
        PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
            when (val s = loader.state) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(s.message, { loader.load() })
                is Load.Ok -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.data.isEmpty()) item { EmptyBox(if (service) "Nenhum serviço encontrado." else "Nenhum produto encontrado.") }
                    items(s.data, key = { it.id }) { p -> ProductCard(p, if (onOpen != null) ({ onOpen(p) }) else null) }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(p: Product, onClick: (() -> Unit)?) {
    AppCard(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(Fmt.money(p.salePrice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            val line = listOfNotNull(p.sku?.let { "SKU $it" }, p.brand, p.category, p.location?.let { "Local: $it" }).joinToString(" · ")
            if (line.isNotBlank()) Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!p.isService) {
                val qty = if (p.stock % 1.0 == 0.0) p.stock.toLong().toString() else "%.3f".format(p.stock)
                Text("Estoque: $qty ${p.unit}" + if (p.lowStock) " · abaixo do mínimo" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
            if (!p.active) Text("Inativo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
