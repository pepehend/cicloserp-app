package com.phdevtech.cicloserp.ui.agenda
import com.phdevtech.cicloserp.ui.home.SyncButton
import com.phdevtech.cicloserp.ui.common.AppCard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background

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

    private val _ui = MutableStateFlow(Ui(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)), false, "todas", Load.Loading))
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
    fun today() { _ui.value = _ui.value.copy(weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)), rows = Load.Loading); load() }
    fun setTeam(v: Boolean) { _ui.value = _ui.value.copy(team = v, rows = Load.Loading); load() }
    fun setFilter(f: String) { _ui.value = _ui.value.copy(filter = f) }
}

/** Agenda da semana (GET tech_tasks): cartão com os dias (domingo a sábado), filtros de situação com contagem e as OS do dia. */
@Composable
fun AgendaScreen(me: Me, onOpenOrder: (Int) -> Unit, contentPadding: PaddingValues) {
    val vm = appViewModel { AgendaViewModel(it) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { vm.load(); onPauseOrDispose { } }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    // Trocou de semana: o dia escolhido passa a ser o primeiro dia dela (ou hoje, se ela o contém)
    LaunchedEffect(ui.weekStart) {
        val days = (0L..6L).map { ui.weekStart.plusDays(it) }
        if (selected !in days) selected = if (LocalDate.now() in days) LocalDate.now() else days.first()
    }
    val month = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
    val dow = DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("pt-BR"))

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { vm.load(pull = true) }, modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(if (ui.team) "Agenda da equipe" else "Minha Agenda", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
            if (me.can("ordens.all")) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !ui.team, onClick = { vm.setTeam(false) }, label = { Text("Minha agenda") })
                    FilterChip(selected = ui.team, onClick = { vm.setTeam(true) }, label = { Text("Equipe") })
                }
            }
            val all = (ui.rows as? Load.Ok)?.data.orEmpty()
            item {
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { vm.week(-1) }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Semana anterior") }
                            Text(selected.format(month), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { vm.week(1) }) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Próxima semana") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            (0L..6L).map { ui.weekStart.plusDays(it) }.forEach { day ->
                                val sel = day == selected
                                val has = all.any { Fmt.parse(it.scheduledAt)?.toLocalDate() == day }
                                Column(
                                    Modifier.width(40.dp)
                                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { selected = day }.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    val c = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    Text(day.format(dow).replaceFirstChar { it.uppercase() }.take(3), style = MaterialTheme.typography.labelSmall, color = if (sel) c else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${day.dayOfMonth}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c)
                                    Box(Modifier.padding(top = 2.dp).size(5.dp).background(if (has) c else Color.Transparent, CircleShape))
                                }
                            }
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = { vm.today(); selected = LocalDate.now() }, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) { Text("Hoje") }
                        }
                        // Filtros de situação com a contagem de OS (da semana)
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FILTERS.forEach { (code, label, st) ->
                                val n = all.count { st == null || it.status in st }
                                FilterChip(
                                    selected = ui.filter == code, onClick = { vm.setFilter(code) }, label = { Text("$label ($n)") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                                    border = null, shape = RoundedCornerShape(50),
                                )
                            }
                        }
                    }
                }
            }
            item {
                SyncButton(Modifier.fillMaxWidth()) { vm.load(pull = true) }
            }
            when (val r = ui.rows) {
                Load.Loading -> item { LoadingBox(Modifier.height(160.dp)) }
                is Load.Error -> item { ErrorBox(r.message, onRetry = { vm.load() }, modifier = Modifier.height(240.dp)) }
                is Load.Ok -> {
                    val statuses = FILTERS.first { it.first == ui.filter }.third
                    val rows = r.data.filter { (statuses == null || it.status in statuses) && Fmt.parse(it.scheduledAt)?.toLocalDate() == selected }
                    if (rows.isEmpty()) item { EmptyBox("Nenhuma tarefa encontrada") }
                    items(rows, key = { it.id }) { o -> OrderCard(o, onClick = { onOpenOrder(o.id) }, showTechnician = ui.team) }
                }
            }
        }
    }
}