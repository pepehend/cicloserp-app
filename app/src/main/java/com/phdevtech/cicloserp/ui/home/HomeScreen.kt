package com.phdevtech.cicloserp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.TechHome
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.OrderCard
import com.phdevtech.cicloserp.ui.common.SectionTitle
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.theme.StatusDone
import com.phdevtech.cicloserp.ui.theme.StatusPaused
import com.phdevtech.cicloserp.ui.theme.StatusRunning
import com.phdevtech.cicloserp.ui.theme.StatusScheduled
import com.phdevtech.cicloserp.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HomeViewModel(private val repo: CiclosRepository) : ViewModel() {
    private val _state = MutableStateFlow<Load<TechHome>>(Load.Loading)
    val state = _state.asStateFlow()
    val team = MutableStateFlow(false)
    val refreshing = MutableStateFlow(false)

    fun load(pull: Boolean = false) {
        if (pull) refreshing.value = true
        viewModelScope.launch {
            _state.value = try { Load.Ok(repo.techHome(team.value)) } catch (e: Exception) {
                if (_state.value is Load.Ok && !pull) _state.value else Load.Error(e.message ?: "Erro ao carregar")
            }
            refreshing.value = false
        }
    }

    fun setTeam(v: Boolean) { team.value = v; _state.value = Load.Loading; load() }
}

/** Início do app do técnico: OS de hoje, atrasadas e em execução (GET tech_home). */
@Composable
fun HomeScreen(
    me: Me, onOpenOrder: (Int) -> Unit, contentPadding: PaddingValues,
    onNavigate: (String) -> Unit = {}, can: (String) -> Boolean = { true },
) {
    val vm = appViewModel { HomeViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    val team by vm.team.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { vm.load(); onPauseOrDispose { } }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        when (val s = state) {
            Load.Loading -> LoadingBox()
            is Load.Error -> ErrorBox(s.message, onRetry = { vm.load() })
            is Load.Ok -> {
                val h = s.data
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        val hour = LocalTime.now().hour
                        val hello = if (hour < 12) "Bom dia" else if (hour < 18) "Boa tarde" else "Boa noite"
                        Text("$hello, ${me.name.substringBefore(' ')}!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(Fmt.weekday(LocalDate.now()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        // Atalhos do app do técnico: Agenda (nova tarefa), Clientes, Orçamento e Produto
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                Triple("Agenda", "agenda", "ordens"), Triple("Clientes", "clientes", "clientes"),
                                Triple("Orçamento", "orcamentos", "orcamentos"), Triple("Produto", "produtos", "produtos"),
                            ).filter { can(it.third) }.forEach { (label, route, _) ->
                                OutlinedButton(onClick = { onNavigate(route) }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                }
                            }
                        }
                    }
                    if (me.can("ordens.all")) item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !team, onClick = { vm.setTeam(false) }, label = { Text("Minhas OS") })
                            FilterChip(selected = team, onClick = { vm.setTeam(true) }, label = { Text("Equipe") })
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Kpi("Hoje", h.today.size, StatusScheduled, Modifier.weight(1f))
                            Kpi("Concluídas", h.done, StatusDone, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Kpi("Em execução", h.running, StatusRunning, Modifier.weight(1f))
                            Kpi("Atrasadas", h.late, StatusPaused, Modifier.weight(1f))
                        }
                    }
                    if (h.canQuotes && h.quotesCount > 0) item {
                        Text("Orçamentos lançados hoje: ${h.quotesCount} · ${Fmt.money(h.quotesTotal)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    item { SectionTitle("Tarefas de hoje") }
                    if (h.today.isEmpty()) item { EmptyBox("Nenhuma OS agendada para hoje.") }
                    items(h.today, key = { it.id }) { o -> OrderCard(o, onClick = { onOpenOrder(o.id) }, showTechnician = team) }
                }
            }
        }
    }
}

@Composable
private fun Kpi(label: String, value: Int, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(Modifier.padding(14.dp)) {
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
