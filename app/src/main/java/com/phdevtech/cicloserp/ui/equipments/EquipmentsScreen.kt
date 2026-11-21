package com.phdevtech.cicloserp.ui.equipments
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import com.phdevtech.cicloserp.ui.common.SearchCard
import com.phdevtech.cicloserp.ui.common.PageHeader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
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
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.model.Equipment
import com.phdevtech.cicloserp.ui.common.AppCard
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.InfoRow
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SearchField
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.util.Fmt

/** Equipamentos dos clientes (GET equipments, tabela equipments). */
@Composable
fun EquipmentsScreen(pad: PaddingValues) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    val loader = remember { Loader(scope) { repo.equipments(query) } }
    LaunchedEffect(query) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    Column(Modifier.fillMaxSize().padding(pad)) {
        PageHeader("Equipamentos", "Equipamentos dos seus clientes, com série e garantia.")
        SearchCard(query, { query = it }, "Nome, marca, modelo, série ou cliente")
        Spacer(Modifier.height(8.dp))
        PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
            when (val s = loader.state) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(s.message, { loader.load() })
                is Load.Ok -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.data.isEmpty()) item { EmptyBox("Nenhum equipamento encontrado.") }
                    items(s.data, key = { it.id }) { EquipmentCard(it) }
                }
            }
        }
    }
}

@Composable
private fun EquipmentCard(e: Equipment) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(e.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InfoRow("Cliente", e.customerName)
            InfoRow("Marca/modelo", listOfNotNull(e.brand, e.model).joinToString(" ").ifBlank { null })
            InfoRow("Nº de série", e.serial)
            InfoRow("Categoria", e.category)
            InfoRow("Garantia até", e.warrantyUntil?.let(Fmt::date))
            InfoRow("Observações", e.notes)
        }
    }
}
