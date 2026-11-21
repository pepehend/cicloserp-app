package com.phdevtech.cicloserp.ui.order

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.OrderDetail
import com.phdevtech.cicloserp.data.model.OrderStatus
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.IconTile
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.PillButton
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
import java.time.LocalDate

/** Motivos sugeridos para a pausa (os mesmos do sistema web). */
private val PAUSE_REASONS = listOf("Almoço", "Fim do expediente", "Aguardando peça", "Aguardando cliente", "Deslocamento para outra tarefa")

private val Purple = Color(0xFF7C3AED)
private val Blue = Color(0xFF2563EB)
private val Orange = Color(0xFFD97706)
private val Green = Color(0xFF16A34A)
private val Red = Color(0xFFDC2626)

/**
 * O que o usuário pode alterar na OS. No celular, depois do check-out tudo fica só para consulta
 * (como no app do técnico web); alterar só pela versão completa.
 */
fun canChange(me: Me, o: OrderDetail) = !o.isDone && (me.can("ordens.execute") || me.can("ordens.edit"))

/** Páginas que a ficha da OS abre (iguais às do app do técnico web). */
class OrderNav(
    val onBack: () -> Unit,
    val onQuestionnaire: (Int) -> Unit,
    val onQuestionnaires: () -> Unit,
    val onPhotos: () -> Unit,
    val onPage: (String) -> Unit,          // km | relato | assinar | pausas | pecas
    val onCustomer: ((Int) -> Unit)?,
    val onEditWeb: (() -> Unit)? = null,
    val onQuote: ((Int) -> Unit)? = null,
)

@Composable
fun OrderScreen(me: Me, id: Int, nav: OrderNav) {
    val vm = appViewModel(key = "os$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LifecycleResumeEffect(id) { vm.load(); onPauseOrDispose { } }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val ctx = LocalContext.current

    Scaffold(
        containerColor = Web.bg,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            (state as? Load.Ok)?.data?.let { o ->
                if (me.can("ordens.execute")) ActionBar(o, busy, onStatus = { to, reason -> vm.setStatus(to, reason) }, onShowPending = { tab = 2; vm.message("Resolva as pendências antes do check-out") })
            }
        },
    ) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                    // barra do topo: voltar
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", bordered = false, onClick = nav.onBack)
                    }
                    // cabeçalho: cliente, Tarefa #, situação e os botões de ligar e compartilhar
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(o.customerName, color = Web.text, fontSize = 20.25.sp, fontWeight = FontWeight.Bold)
                            Text("Tarefa #${o.id}", color = Web.muted, fontSize = 13.sp)
                            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusTag(o.status, o.statusLabel)
                                if (o.pending.isNotEmpty() && !o.isDone) PendingTag(o.pending.size)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            o.customerPhone?.let { phone ->
                                IconBtn(Icons.Outlined.Phone, "Ligar", size = 38.dp) { ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri())) }
                            }
                            if (nav.onEditWeb != null && !o.isDone && me.can("app.switch")) IconBtn(Icons.Outlined.Edit, "Editar na versão completa", size = 38.dp, onClick = nav.onEditWeb)
                            IconBtn(Icons.Outlined.Share, "Compartilhar", size = 38.dp) {
                                val text = listOfNotNull("Tarefa #${o.id} · ${o.customerName}", o.description, o.publicUrl).joinToString("\n")
                                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, null))
                            }
                        }
                    }
                    if (!canChange(me, o)) LockNote(
                        if (o.isDone) "Check-out feito: a ordem está concluída. Pelo celular dá só para consultar; para alterar, use o computador."
                        else "Você não tem permissão para preencher o relatório desta OS.",
                    )
                    Tabs(listOf("Detalhes", "Relatório", if (o.pending.isNotEmpty() && !o.isDone) "Pendências (${o.pending.size})" else "Pendências"), tab) { tab = it }
                    Spacer(Modifier.height(12.dp))
                    when (tab) {
                        0 -> DetailsTab(o, nav, onReportTab = { tab = 1 })
                        1 -> ReportTab(o, nav)
                        else -> PendingTab(o, nav, onReportTab = { tab = 1 })
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- peças comuns (.tk-*)

@Composable
internal fun IconBtn(icon: ImageVector, desc: String, size: Dp = 40.dp, bordered: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(size).clip(shape).then(if (bordered) Modifier.background(Web.surface).border(1.dp, Web.line, shape) else Modifier).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(20.dp), tint = Web.text) }
}

/** .tk-st: situação em pílula pequena. */
@Composable
private fun StatusTag(status: String, label: String, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.CONCLUIDA -> Web.okSoft to Web.ok
        OrderStatus.EM_EXECUCAO, OrderStatus.A_CAMINHO -> Blue.copy(alpha = 0.14f) to Blue
        OrderStatus.PAUSADA -> Web.warnSoft to Web.warn
        OrderStatus.AGENDADA -> Web.accentSoft to Web.accent
        else -> Web.surface2 to Web.muted
    }
    Text(label, modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 9.dp, vertical = 2.dp), color = fg, fontSize = 11.2.sp)
}

/** .tk-tag.warn: "N pendência(s)". */
@Composable
fun PendingTag(count: Int, modifier: Modifier = Modifier) {
    Text("$count pendência${if (count > 1) "s" else ""}", modifier.clip(RoundedCornerShape(50)).background(Web.warnSoft).padding(horizontal = 9.dp, vertical = 2.dp), color = Web.warn, fontSize = 11.2.sp)
}

/** .tk-lock: aviso cinza com cadeado. */
@Composable
internal fun LockNote(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Web.surface2, RoundedCornerShape(12.dp)).border(1.dp, Web.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp), tint = Web.muted)
        Text(text, color = Web.muted, fontSize = 13.4.sp, lineHeight = 18.sp)
    }
}

/** .tk-tabs: abas com sublinhado vermelho na escolhida. */
@Composable
internal fun Tabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, t ->
                val on = i == selected
                Column(Modifier.weight(1f).clickable { onSelect(i) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(t, Modifier.padding(vertical = 10.dp), color = if (on) Web.accent else Web.muted, fontSize = 13.8.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    Box(Modifier.fillMaxWidth().height(2.5.dp).background(if (on) Web.accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line))
    }
}

/** .tk-card: superfície com borda e raio 14. */
@Composable
private fun TkCard(modifier: Modifier = Modifier, padding: Dp = 12.dp, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().background(Web.surface, RoundedCornerShape(14.dp)).border(1.dp, Web.line, RoundedCornerShape(14.dp)).padding(padding)) { content() }
}

/** .tk-line: quadradinho de ícone + texto. */
@Composable
private fun Line(icon: ImageVector, color: Color, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(vertical = 8.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, color, size = 34.dp)
        Box(Modifier.weight(1f)) { content() }
        trailing?.invoke()
    }
}

internal fun whenText(iso: String?): String {
    val dt = Fmt.parse(iso) ?: return "Sem agendamento"
    val day = if (dt.toLocalDate() == LocalDate.now()) "Hoje" else Fmt.date(iso)
    return "$day às ${Fmt.time(iso)}"
}

private fun hhmm(seconds: Long): String = "%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60)

@Composable
private fun DetailsTab(o: OrderDetail, nav: OrderNav, onReportTab: () -> Unit) {
    val ctx = LocalContext.current
    var orientation by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TkCard {
            Line(Icons.Outlined.Place, Blue) { Text(o.customerAddress ?: "Cliente sem endereço", color = Web.text, fontSize = 14.4.sp) }
            o.customerAddress?.let { addr ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(50)).background(Blue)
                        .clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, "geo:0,0?q=${android.net.Uri.encode(addr)}".toUri())) }.padding(12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Navigation, null, Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Navegar até o local", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
            o.customerPhone?.let { phone ->
                Line(Icons.Outlined.Phone, Green, onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, "tel:${phone.filter { it.isDigit() || it == '+' }}".toUri())) }) { Text(phone, color = Web.text, fontSize = 14.4.sp) }
            }
            Line(Icons.Outlined.CalendarMonth, Purple) { Text(whenText(o.scheduledAt), color = Web.text, fontSize = 14.4.sp) }
            Line(Icons.Outlined.Info, Web.muted, onClick = { orientation = !orientation }, trailing = {
                Icon(if (orientation) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = Web.muted)
            }) { Text("Orientação", color = Web.text, fontSize = 14.4.sp) }
            if (orientation) Text(o.description.ifBlank { "—" }, Modifier.padding(start = 48.dp, bottom = 6.dp), color = Web.muted, fontSize = 14.4.sp)
            o.taskTypeName?.let { t ->
                Line(Icons.Outlined.Sell, Orange) { Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Tipo: ") }; append(t) }, color = Web.text, fontSize = 14.4.sp) }
            }
            o.equipment?.let { e ->
                Line(Icons.Outlined.Settings, Web.muted) { Text(buildAnnotatedString { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Equipamento: ") }; append(e) }, color = Web.text, fontSize = 14.4.sp) }
            }
        }
        o.taskTypeDuration?.takeIf { it > 0 }?.let { min ->
            TkCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp), tint = Web.text)
                    Text("%02d:%02dh".format(min / 60, min % 60), color = Web.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Tempo para execução", color = Web.muted, fontSize = 14.sp)
                }
            }
        }
        TkCard(padding = 4.dp) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                NavRow(Icons.Outlined.Description, Blue, "Relatório de tarefa", onClick = onReportTab)
                Divider()
                NavRow(Icons.Outlined.Person, Purple, "Detalhes do cliente") { o.customerId?.let { nav.onCustomer?.invoke(it) } }
                Divider()
                NavRow(Icons.Outlined.Inventory2, Orange, "Peças e Serviços", sub = if (o.items.isEmpty()) null else "${o.items.size} item(ns)" + if (o.itemsReview > 0) " · ${o.itemsReview} aguardando revisão" else "") { nav.onPage("pecas") }
                Divider()
                NavRow(Icons.Outlined.Sell, Red, "Orçamentos", sub = if (o.quotes.isEmpty()) null else "${o.quotes.size} orçamento(s)") { nav.onPage("orcamentos") }
            }
        }
    }
}

@Composable
private fun Divider() { Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line)) }

@Composable
private fun NavRow(icon: ImageVector, color: Color, label: String, sub: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, color, size = 34.dp)
        Column(Modifier.weight(1f)) {
            Text(label, color = Web.text, fontSize = 15.sp)
            if (sub != null) Text(sub, color = Web.muted, fontSize = 12.5.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = Web.muted)
    }
}

/** Relatório: checklist (fotos, questionário, quilometragem, relato, assinar e enviar, pausas) com o progresso e os avisos de pendência. */
@Composable
private fun ReportTab(o: OrderDetail, nav: OrderNav) {
    val photosDone = o.photos.isNotEmpty()
    val qPend = o.questionnaires.count { !it.answered || it.missing.isNotEmpty() }
    val qDone = o.questionnaires.isNotEmpty() && qPend == 0
    val kmDone = o.km != null
    val reportDone = !o.report.isNullOrBlank()
    val signDone = o.signature != null
    val flags = listOf(photosDone, qDone, kmDone, reportDone, signDone)
    val done = flags.count { it }
    val pausedNow = o.status == OrderStatus.PAUSADA
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TkCard {
            Row {
                Text("Checklist do relatório", Modifier.weight(1f), color = Web.text, fontSize = 13.4.sp, fontWeight = FontWeight.Bold)
                Text("$done de ${flags.size} concluídos", color = Web.accent, fontSize = 13.4.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Web.line)) {
                Box(Modifier.fillMaxWidth(done / flags.size.toFloat()).height(4.dp).clip(RoundedCornerShape(50)).background(Web.accent))
            }
            Text("Complete os itens abaixo antes de assinar e enviar.", color = Web.muted, fontSize = 12.8.sp)
        }
        TkCard(padding = 4.dp) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                CheckRow(Icons.Outlined.Image, "Fotos", if (photosDone) "${o.photos.size} foto(s)" else null, photosDone, onClick = nav.onPhotos)
                Divider()
                CheckRow(
                    Icons.Outlined.Description, "Questionário",
                    if (o.questionnaires.isEmpty()) "Nenhum questionário" else if (qPend > 0) "$qPend questionário${if (qPend > 1) "s" else ""} pendente${if (qPend > 1) "s" else ""}" else "Respondido",
                    qDone, warn = qPend > 0, onClick = nav.onQuestionnaires,
                )
                Divider()
                CheckRow(Icons.Outlined.Speed, "Quilometragem", o.km?.removeSuffix(".00")?.replace('.', ',')?.let { "$it km" }, kmDone) { nav.onPage("km") }
                Divider()
                CheckRow(Icons.Outlined.Edit, "Relato", null, reportDone) { nav.onPage("relato") }
                Divider()
                CheckRow(Icons.Outlined.Draw, "Assinar e enviar", o.signerName, signDone) { nav.onPage("assinar") }
                Divider()
                CheckRow(Icons.Outlined.Pause, "Controle de Horas", if (o.hoursPause > 0) "${Fmt.duration(o.hoursPause)} em pausa" else null, null, tint = Red, warn = pausedNow) { nav.onPage("pausas") }
            }
        }
        TkCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.Schedule, null, Modifier.size(20.dp), tint = Web.text)
                Text(
                    "Check-in: ${o.startedAt?.let { Fmt.time(it) + "h" } ?: "00:00h"}  ·  Check-out: ${o.finishedAt?.let { Fmt.time(it) + "h" } ?: "00:00h"}",
                    color = Web.muted, fontSize = 13.8.sp,
                )
            }
        }
    }
}

/** Bolinha de situação: cheia e verde (feito), tracejada amarela (pendente/atenção) ou vazia. */
@Composable
private fun StatusDot(done: Boolean?, warn: Boolean) {
    if (done == true) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Web.ok), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color.White) }
    } else if (warn) {
        val c = Web.warn
        Box(Modifier.size(22.dp).drawBehind {
            drawCircle(c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
        })
    } else {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Web.surface2).border(1.5.dp, Web.line, CircleShape))
    }
}

/** Linha do checklist: bolinha de situação, ícone, rótulo, subtítulo (amarelo quando há pendência) e seta. */
@Composable
private fun CheckRow(icon: ImageVector, label: String, sub: String?, done: Boolean?, tint: Color? = null, warn: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(done, warn)
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(if (tint != null) tint.copy(alpha = 0.14f) else Web.surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = tint ?: Web.muted)
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = Web.text, fontSize = 15.sp)
            if (sub != null) Text(sub, color = if (warn) Web.warn else Web.muted, fontSize = 12.5.sp)
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = Web.muted)
    }
}

/** Página para onde cada pendência leva (igual ao web): questionário, relato, assinatura, foto, km ou o relatório. */
private fun pendingTarget(text: String): String = when {
    Regex("questionário", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "questionarios"
    Regex("relato", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "relato"
    Regex("assin|cpf|nome de quem", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "assinar"
    Regex("foto", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "fotos"
    Regex("km", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "km"
    else -> "relatorio"
}

/** Aba Pendências: cartão amarelo "N pendência(s)" com cada item clicável (leva à página que resolve), ou "Nenhuma pendência." */
@Composable
private fun PendingTab(o: OrderDetail, nav: OrderNav, onReportTab: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (o.pending.isEmpty() || o.isDone) Text("Nenhuma pendência.", Modifier.fillMaxWidth().padding(vertical = 28.dp), color = Web.muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        else Column(
            Modifier.fillMaxWidth().background(Web.warnSoft, RoundedCornerShape(14.dp)).border(1.dp, Web.warn.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.WarningAmber, null, Modifier.size(20.dp), tint = Web.warn)
                Text("${o.pending.size} pendência${if (o.pending.size > 1) "s" else ""}", color = Web.warn, fontWeight = FontWeight.Bold)
            }
            o.pending.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        when (val target = pendingTarget(p)) {
                            "questionarios" -> nav.onQuestionnaires()
                            "fotos" -> nav.onPhotos()
                            "relatorio" -> onReportTab()
                            else -> nav.onPage(target)
                        }
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(p, Modifier.weight(1f), color = Web.text, fontSize = 14.sp)
                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = Web.warn)
                }
            }
        }
        if (o.itemsReview > 0) Text("${o.itemsReview} item(ns) lançados na OS aguardam revisão do cadastro.", color = Web.muted, fontSize = 13.sp)
    }
}
/** .tk-actbar: barra fixa com A caminho / Check-in / Pausar / Retomar / Check-out, conforme as transições da OS. */
@Composable
private fun ActionBar(o: OrderDetail, busy: Boolean, onStatus: (String, String?) -> Unit, onShowPending: () -> Unit) {
    val actions = o.transitions.filter { it in listOf(OrderStatus.A_CAMINHO, OrderStatus.EM_EXECUCAO, OrderStatus.PAUSADA, OrderStatus.CONCLUIDA) }
    if (actions.isEmpty()) return
    var pauseDialog by remember { mutableStateOf(false) }
    var checkoutDialog by remember { mutableStateOf(false) }
    Surface(color = Web.surface) {
        Column {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { to ->
                    val (label, icon) = when (to) {
                        OrderStatus.A_CAMINHO -> "A caminho" to Icons.Outlined.DirectionsCar
                        OrderStatus.EM_EXECUCAO -> (if (o.status == OrderStatus.PAUSADA) "Retomar" else "Check-in") to
                            (if (o.status == OrderStatus.PAUSADA) Icons.Outlined.PlayArrow else Icons.AutoMirrored.Outlined.Login)
                        OrderStatus.PAUSADA -> "Pausar" to Icons.Outlined.Pause
                        else -> "Check-out" to Icons.AutoMirrored.Outlined.Logout
                    }
                    PillButton(
                        label, {
                            when (to) {
                                OrderStatus.PAUSADA -> pauseDialog = true
                                OrderStatus.CONCLUIDA -> if (o.pending.isNotEmpty()) onShowPending() else checkoutDialog = true
                                else -> onStatus(to, null)
                            }
                        },
                        Modifier.weight(1f), filled = to == OrderStatus.EM_EXECUCAO || to == OrderStatus.CONCLUIDA, icon = icon, enabled = !busy,
                    )
                }
            }
        }
    }
    if (pauseDialog) PauseDialog(onDismiss = { pauseDialog = false }, onConfirm = { pauseDialog = false; onStatus(OrderStatus.PAUSADA, it) })
    if (checkoutDialog) AlertDialog(
        onDismissRequest = { checkoutDialog = false }, containerColor = Web.surface,
        title = { Text("Fazer check-out?") },
        text = { Text("A OS será concluída. Depois disso, no celular ela fica só para consulta.") },
        confirmButton = { TextButton(onClick = { checkoutDialog = false; onStatus(OrderStatus.CONCLUIDA, null) }) { Text("Concluir", color = Web.accent) } },
        dismissButton = { TextButton(onClick = { checkoutDialog = false }) { Text("Voltar", color = Web.accent) } },
    )
}

/** Motivo da pausa: lista de motivos (.tk-reasons) e campo livre. */
@Composable
internal fun PauseDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Web.surface,
        title = { Text("Pausar atendimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PAUSE_REASONS.forEach { r ->
                    Text(
                        r, Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, if (reason == r) Web.accent else Web.line, RoundedCornerShape(12.dp))
                            .background(if (reason == r) Web.accentSoft else Web.surface).clickable { reason = r }.padding(12.dp),
                        color = Web.text, fontSize = 14.4.sp,
                    )
                }
                WebField(reason, { reason = it.take(120) }, Modifier.fillMaxWidth(), label = "Outro motivo")
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(reason.trim()) }) { Text("Pausar", color = Web.accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Web.accent) } },
    )
}
