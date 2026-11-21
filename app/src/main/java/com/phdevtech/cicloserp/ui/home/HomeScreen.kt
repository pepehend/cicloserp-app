package com.phdevtech.cicloserp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.TechHome
import com.phdevtech.cicloserp.ui.common.AppCard
import com.phdevtech.cicloserp.ui.common.EmptyBox
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.IconTile
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.OrderCard
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.theme.StatusDone
import com.phdevtech.cicloserp.ui.theme.StatusRunning
import com.phdevtech.cicloserp.ui.theme.StatusScheduled
import com.phdevtech.cicloserp.util.Fmt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

private val Purple = Color(0xFF6F42C1)
private val Blue = Color(0xFF2563EB)
private val Orange = Color(0xFFD97706)
private val Green = Color(0xFF16A34A)
private val Red = Color(0xFFDC2626)

/** Início do app do técnico: saudação, atalhos, indicadores e agenda de hoje (GET tech_home). */
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
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val hour = LocalTime.now().hour
                        val hello = if (hour < 12) "Bom dia" else if (hour < 18) "Boa tarde" else "Boa noite"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("$hello,", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(me.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                            SyncButton { vm.load(pull = true) }
                        }
                    }
                    item {
                        // Atalhos: Nova Tarefa (agenda), Clientes, Orçamento, Produto
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(
                                Shortcut("Nova Tarefa", Icons.Outlined.Add, Purple, "os/nova/0", "ordens.add"),
                                Shortcut("Agenda", Icons.Outlined.CalendarMonth, Blue, "agenda", "ordens"),
                                Shortcut("Clientes", Icons.Outlined.Group, Blue, "clientes", "clientes"),
                                Shortcut("Orçamento", Icons.Outlined.Description, Orange, "orcamentos", "orcamentos"),
                                Shortcut("Produto", Icons.Outlined.Inventory2, Green, "produtos", "produtos"),
                            ).filter { can(it.perm) }.forEach { sc ->
                                AppCard(Modifier.width(84.dp).clickable { onNavigate(sc.route) }) {
                                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        IconTile(sc.icon, sc.color, size = 48.dp)
                                        Text(sc.label, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    if (me.can("ordens.all")) item {
                        LabeledCheck(team, vm::setTeam, "Mostrar as tarefas de toda a equipe")
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Kpi(Icons.Outlined.CalendarMonth, "${h.today.size}", "OS para hoje", StatusScheduled, Modifier.weight(1f))
                            Kpi(Icons.Outlined.WarningAmber, "${h.late}", "OS atrasadas", Red, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Kpi(Icons.Outlined.Description, Fmt.money(h.quotesTotal), "${h.quotesCount} orçamentos lançados", Orange, Modifier.weight(1f))
                            Kpi(Icons.Outlined.PlayArrow, "${h.running}", "em execução", Green, Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Agenda de hoje", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Row(Modifier.clickable { onNavigate("agenda") }, verticalAlignment = Alignment.CenterVertically) {
                                Text("Ver tudo", color = com.phdevtech.cicloserp.ui.theme.Web.accentText, style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Outlined.ChevronRight, null, tint = com.phdevtech.cicloserp.ui.theme.Web.accentText)
                            }
                        }
                    }
                    item {
                        val total = h.today.size + h.done
                        val pct = if (total > 0) h.done.toFloat() / total else 0f
                        Column {
                            Row {
                                Text("${h.done}/$total OS finalizadas", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text("${(pct * 100).toInt()}%", color = com.phdevtech.cicloserp.ui.theme.Web.accentText, fontWeight = FontWeight.SemiBold)
                            }
                            LinearProgressIndicator(
                                progress = { pct }, Modifier.fillMaxWidth().padding(top = 6.dp),
                                color = StatusDone, trackColor = MaterialTheme.colorScheme.surfaceVariant, gapSize = 0.dp,
                            )
                        }
                    }
                    if (h.today.isEmpty()) item { EmptyBox("Nenhuma tarefa pendente para hoje") }
                    items(h.today, key = { it.id }) { o -> OrderCard(o, onClick = { onOpenOrder(o.id) }, showTechnician = team) }
                }
            }
        }
    }
}

private data class Shortcut(val label: String, val icon: ImageVector, val color: Color, val route: String, val perm: String)

/** Botão "Sincronizar": contorno verde, como no sistema web. */
@Composable
fun SyncButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = modifier,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusDone),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusDone),
    ) {
        Icon(Icons.Outlined.Sync, null, Modifier.padding(end = 6.dp))
        Text("Sincronizar", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Kpi(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color)
            Text(value, Modifier.padding(top = 10.dp), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
