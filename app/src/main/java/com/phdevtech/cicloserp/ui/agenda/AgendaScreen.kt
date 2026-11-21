package com.phdevtech.cicloserp.ui.agenda

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.data.model.OrderSummary
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.OrderCard
import com.phdevtech.cicloserp.ui.common.SectionTitle
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/** Filtros de situação da agenda, iguais aos do app do técnico web (TK_FILTERS). */
private val FILTERS = listOf(
    Triple("todas", "Todas", null),
    Triple("aberto", "Em aberto", listOf(OrderStatus.ABERTA, OrderStatus.AGENDADA)),
    Triple("execucao", "Em execução", listOf(OrderStatus.A_CAMINHO, OrderStatus.EM_EXECUCAO)),
    Triple("pausa", "Em pausa", listOf(OrderStatus.PAUSADA)),
    Triple("finalizadas", "Finalizadas", listOf(OrderStatus.CONCLUIDA)),
)

class AgendaViewModel(private val repo: CiclosRepository) : ViewModel() {
    data class Ui(val weekStart: LocalDate, val team: Boolean, val filter: String, val rows: Load<List<OrderSummary>>)

    private val _ui = MutableStateFlow(Ui(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), false, "todas", Load.Loading))
    val ui = _ui.asStateFlow()
    val refreshing = MutableStateFlow(false)

    fun load(pull: Boolean = false) {
        val u = _ui.value
        if (pull) refreshing.value = true
        viewModelScope.launch {
            val r = try {
                Load.Ok(repo.techTasks(u.weekStart.toString(), u.weekStart.plusDays(6).toString(), u.team))
            } catch (e: Exception) { Load.Error(e.message ?: "Erro ao carregar") }
            // Só aplica se o usuário não trocou de semana/equipe enquanto carregava
            if (_ui.value.weekStart == u.weekStart && _ui.value.team == u.team) _ui.value = _ui.value.copy(rows = r)
            refreshing.value = false
        }
    }

    fun week(delta: Long) { _ui.value = _ui.value.copy(weekStart = _ui.value.weekStart.plusWeeks(delta), rows = Load.Loading); load() }
    fun today() { _ui.value = _ui.value.copy(weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), rows = Load.Loading); load() }
    fun setTeam(v: Boolean) { _ui.value = _ui.value.copy(team = v, rows = Load.Loading); load() }
    fun setFilter(f: String) { _ui.value = _ui.value.copy(filter = f) }
}

/** Agenda da semana (GET tech_tasks), por dia, com filtros de situação. */
@Composable
fun AgendaScreen(me: Me, onOpenOrder: (Int) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { AgendaViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { vm.load(); onPauseOrDispose { } }
    val dm = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR"))

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.week(-1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Semana anterior") }
            Text("${ui.weekStart.format(dm)} – ${ui.weekStart.plusDays(6).format(dm)}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = vm::today) { Text("Hoje") }
            IconButton(onClick = { vm.week(1) }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Próxima semana") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (me.can("ordens.all")) {
                FilterChip(selected = !ui.team, onClick = { vm.setTeam(false) }, label = { Text("Minha agenda") })
                FilterChip(selected = ui.team, onClick = { vm.setTeam(true) }, label = { Text("Equipe") })
            }
            FILTERS.forEach { (code, label, _) -> FilterChip(selected = ui.filter == code, onClick = { vm.setFilter(code) }, label = { Text(label) }) }
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize()) {
            when (val r = ui.rows) {
                Load.Loading -> LoadingBox()
                is Load.Error -> ErrorBox(r.message, onRetry = { vm.load() })
                is Load.Ok -> {
                    val statuses = FILTERS.first { it.first == ui.filter }.third
                    val rows = r.data.filter { statuses == null || it.status in statuses }
                    val byDay = rows.groupBy { Fmt.parse(it.scheduledAt)?.toLocalDate() }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (rows.isEmpty()) item { EmptyBox("Nenhuma OS nesta semana.") }
                        (0L..6L).map { ui.weekStart.plusDays(it) }.forEach { day ->
                            val list = byDay[day].orEmpty()
                            if (list.isNotEmpty()) {
                                item(key = "d$day") {
                                    SectionTitle(Fmt.weekday(day) + if (day == LocalDate.now()) " · hoje" else "")
                                }
                                items(list, key = { it.id }) { o -> OrderCard(o, onClick = { onOpenOrder(o.id) }, showTechnician = ui.team) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private val bold = FontWeight.SemiBold
