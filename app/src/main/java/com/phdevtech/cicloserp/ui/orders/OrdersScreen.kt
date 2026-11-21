package com.phdevtech.cicloserp.ui.orders

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.data.model.OrderSummary
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.OrderCard
import com.phdevtech.cicloserp.ui.common.appViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrdersViewModel(private val repo: CiclosRepository) : ViewModel() {
    val status = MutableStateFlow("abertas")
    val query = MutableStateFlow("")
    private val _rows = MutableStateFlow<Load<List<OrderSummary>>>(Load.Loading)
    val rows = _rows.asStateFlow()
    val refreshing = MutableStateFlow(false)
    private var job: Job? = null

    fun load(pull: Boolean = false, debounce: Boolean = false) {
        job?.cancel()
        if (pull) refreshing.value = true
        job = viewModelScope.launch {
            if (debounce) delay(400)
            _rows.value = try { Load.Ok(repo.orders(status.value, query.value)) } catch (e: Exception) { Load.Error(e.message ?: "Erro ao carregar") }
            refreshing.value = false
        }
    }

    fun setStatus(s: String) { status.value = s; _rows.value = Load.Loading; load() }
    fun setQuery(q: String) { query.value = q; load(debounce = true) }
}

/** Lista de ordens de serviço (GET service_orders) com busca e filtro de situação. */
@Composable
fun OrdersScreen(onOpenOrder: (Int) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { OrdersViewModel(it) }
    val rows by vm.rows.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { vm.load(); onPauseOrDispose { } }
    val filters = listOf("abertas" to "Em aberto", "todas" to "Todas") + OrderStatus.LABELS.toList()

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery, singleLine = true,
            placeholder = { Text("Buscar por cliente, descrição ou número") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, "Limpar busca") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.load() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (code, label) -> FilterChip(selected = status == code, onClick = { vm.setStatus(code) }, label = { Text(label) }) }
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            when (val r = rows) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(r.message, onRetry = { vm.load() })
                is Load.Ok -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (r.data.isEmpty()) item { EmptyBox("Nenhuma ordem de serviço encontrada.") }
                    items(r.data, key = { it.id }) { o -> OrderCard(o, onClick = { onOpenOrder(o.id) }, showTechnician = true, showDate = true) }
                }
            }
        }
    }
}
