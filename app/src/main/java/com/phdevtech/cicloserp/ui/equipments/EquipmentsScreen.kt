package com.phdevtech.cicloserp.ui.equipments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.phdevtech.cicloserp.data.model.Equipment
import com.phdevtech.cicloserp.ui.common.AddButton
import com.phdevtech.cicloserp.ui.common.Col
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.PageHeader
import com.phdevtech.cicloserp.ui.common.TRow
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.WebTable
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt

/** Equipamentos (GET equipments): Equipamento (marca · modelo · categoria) | Cliente | Identificador | Garantia. */
@Composable
fun EquipmentsScreen(pad: PaddingValues, canAdd: Boolean = false, onNew: () -> Unit = {}, onOpen: ((Equipment) -> Unit)? = null) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    val loader = remember { Loader(scope) { repo.equipments(query) } }
    LaunchedEffect(query) { loader.load(debounceMs = if (query.isEmpty()) 0 else 350) }

    PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize().padding(pad)) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { PageHeader("Equipamentos", "Equipamentos dos seus clientes, com identificador e garantia.") }
            if (canAdd) item { AddButton("Novo equipamento", onNew) }
            item {
                WebCard(Modifier.padding(horizontal = 16.dp)) {
                    WebField(query, { query = it }, Modifier.fillMaxWidth(), label = "Buscar", placeholder = "Nome, marca, modelo, série ou cliente")
                    Spacer(Modifier.height(8.dp))
                    when (val s = loader.state) {
                        Load.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Web.accent) }
                        is Load.Error -> Text(s.message, color = Web.danger)
                        is Load.Ok -> WebTable(
                            listOf(Col("Equipamento", 200.dp), Col("Cliente", 180.dp), Col("Identificador", 140.dp), Col("Garantia", 110.dp)),
                            s.data.map { e ->
                                TRow(e.id, listOf(e.name, e.customerName.orEmpty(), e.serial.orEmpty(), e.warrantyUntil?.let(Fmt::date).orEmpty()),
                                    onOpen?.let { { it(e) } },
                                    subs = listOfNotNull(e.brand, e.model, e.category).joinToString(" · ").let { if (it.isBlank()) emptyMap() else mapOf(0 to it) },
                                    badgeAfterFirst = if (e.active) null else "inativo")
                            },
                            empty = "Nenhum equipamento encontrado.",
                        )
                    }
                }
            }
        }
    }
}
