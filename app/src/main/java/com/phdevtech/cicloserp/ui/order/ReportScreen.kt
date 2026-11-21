package com.phdevtech.cicloserp.ui.order
import com.phdevtech.cicloserp.ui.common.IconTile
import com.phdevtech.cicloserp.data.model.OrderStatus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Check
import com.phdevtech.cicloserp.ui.common.dashed
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Chat

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.OrderDetail
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.PillButton
import com.phdevtech.cicloserp.ui.common.SignaturePad
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.common.rememberSignatureState
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
import com.phdevtech.cicloserp.util.Images

private fun hms(seconds: Long) = "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)

/** Moldura das páginas do relatório: voltar, título grande, aviso de só consulta e o botão Salvar do topo (.tk-save). */
@Composable
private fun TkPage(title: String, onBack: () -> Unit, onSave: (() -> Unit)?, snack: SnackbarHostState, locked: Boolean = false, content: @Composable () -> Unit) {
    Scaffold(containerColor = Web.bg, snackbarHost = { SnackbarHost(snack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Web.text) }
                Spacer(Modifier.weight(1f))
                if (onSave != null && !locked) TextButton(onClick = onSave) { Text("Salvar", color = Web.accent, fontSize = 16.sp, fontWeight = FontWeight(650)) }
            }
            Text(title, color = Web.text, fontSize = 20.25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            if (locked) Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Web.surface2, RoundedCornerShape(12.dp)).border(1.dp, Web.line, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Lock, null, Modifier.size(20.dp), tint = Web.muted)
                Text("Ordem concluída: só consulta. Para alterar, use o computador.", color = Web.muted, fontSize = 13.4.sp)
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Páginas Quilometragem (km), Relato (relato) e Assinar e enviar (assinar) da OS (POST so_report_save):
 * cada uma mostra só o seu campo, como no app do técnico do sistema web; todas gravam o relatório inteiro.
 */
@Composable
fun ReportScreen(me: Me, id: Int, onBack: () -> Unit, page: String = "relato") {
    val vm = appViewModel(key = "rep$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    when (val s = state) {
        Load.Loading -> LoadingBox()
        is Load.Error -> ErrorBox(s.message, onRetry = vm::load)
        is Load.Ok -> {
            val o = s.data
            val editable = canChange(me, o)
            var report by rememberSaveable(o.id) { mutableStateOf(o.report.orEmpty()) }
            var km by rememberSaveable(o.id) { mutableStateOf(o.km?.removeSuffix(".00")?.replace('.', ',').orEmpty()) }
            var signer by rememberSaveable(o.id) { mutableStateOf(o.signerName.orEmpty()) }
            var doc by rememberSaveable(o.id) { mutableStateOf(Fmt.doc(o.signerDoc)) }
            var email by rememberSaveable(o.id) { mutableStateOf(o.customerEmail.orEmpty()) }
            var sigNew by rememberSaveable(o.id) { mutableStateOf<String?>(null) }
            var sigDialog by remember { mutableStateOf(false) }
            var removeSig by rememberSaveable(o.id) { mutableStateOf(false) }
            val pad2 = rememberSignatureState()
            val ctx = LocalContext.current
            val title = when (page) { "km" -> "Quilometragem"; "assinar" -> "Assinar e enviar"; else -> "Relato" }
            // Desenhou: assinatura nova; pediu para remover: apaga ("" ); senão mantém a salva (null)
            val sigToSave = { if (sigNew != null) sigNew else if (removeSig) "" else null }
            val save = { vm.run("Salvo", then = onBack) { saveReport(id, report, km, signer, doc.filter(Char::isDigit), sigToSave()) } }
            TkPage(title, onBack, save, snack, locked = !editable) {
                when (page) {
                    "km" -> WebCard(padding = 14.dp) {
                        WebField(km, { v -> km = v.filter { it.isDigit() || it == ',' || it == '.' }.take(10) }, Modifier.fillMaxWidth(), label = "Km rodado", placeholder = "0", keyboardType = KeyboardType.Decimal)
                    }
                    "relato" -> WebCard(padding = 14.dp) {
                        WebField(report, { report = it.take(10000) }, Modifier.fillMaxWidth(), label = "O que foi feito", placeholder = "Descreva o atendimento", minLines = 6)
                    }
                    else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // pendências que ainda impedem o envio (as da própria assinatura não contam)
                        val pend = o.pending.filter { !Regex("assinatura|nome de quem|CPF/CNPJ de quem", RegexOption.IGNORE_CASE).containsMatchIn(it) }
                        if (pend.isNotEmpty() && !o.isDone) Column(
                            Modifier.fillMaxWidth().background(Web.warnSoft, RoundedCornerShape(14.dp)).border(1.dp, Web.warn.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Outlined.WarningAmber, null, Modifier.size(20.dp), tint = Web.warn)
                                Text("Antes de enviar falta:", color = Web.warn, fontWeight = FontWeight.Bold)
                            }
                            pend.forEach { Text(it, color = Web.text, fontSize = 13.6.sp) }
                        }
                        WebCard(padding = 14.dp) {
                            // quadro da assinatura (.tk-sigbox): tracejado cinza; toque para colher
                            val shownSig = if (removeSig) null else (sigNew ?: o.signature)
                            Box(
                                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Web.surface2).dashed(Web.line, 12.dp)
                                    .clickable(enabled = editable) { pad2.clear(); sigDialog = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                val img = remember(shownSig?.length) { Images.decodeDataUrl(shownSig) }
                                if (img != null) Image(img, "Assinatura", Modifier.fillMaxSize().padding(6.dp).background(Color.White, RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                                else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.Draw, null, Modifier.size(20.dp), tint = Web.muted)
                                    Text("Toque para colher a assinatura", color = Web.muted, fontSize = 14.sp)
                                }
                            }
                            if (shownSig != null && editable) Text(
                                "Remover assinatura", Modifier.padding(vertical = 6.dp).clickable { sigNew = null; removeSig = true }, color = Web.accent, fontSize = 13.6.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            WebField(signer, { signer = it.take(120) }, Modifier.fillMaxWidth(), label = "Nome de quem assinou", capitalization = KeyboardCapitalization.Words, enabled = editable)
                            Spacer(Modifier.height(10.dp))
                            WebField(doc, { v -> doc = Fmt.doc(v.filter(Char::isDigit).take(14)) }, Modifier.fillMaxWidth(), label = "CPF/CNPJ de quem assinou", placeholder = "CPF ou CNPJ", keyboardType = KeyboardType.Number, enabled = editable)
                        }
                        WebCard(padding = 14.dp) {
                            WebField(email, { email = it }, Modifier.fillMaxWidth(), label = "Enviar a OS digital para o e-mail", placeholder = "email@cliente.com", keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
                            Spacer(Modifier.height(10.dp))
                            PillButton("Enviar por e-mail", {
                                // salva o que mudou e manda a OS digital pelo servidor (como no web)
                                vm.run("OS enviada por e-mail") {
                                    if (editable) saveReport(id, report, km, signer, doc.filter(Char::isDigit), sigToSave())
                                    sendOrder(id, email)
                                }
                            }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Email, enabled = email.contains('@') && !busy)
                            Spacer(Modifier.height(8.dp))
                            PillButton("Enviar pelo WhatsApp", {
                                val phone = o.customerPhone.orEmpty().filter { it.isDigit() }.let { if (it.length >= 10) "55$it" else "" }
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$phone?text=${android.net.Uri.encode("Ordem de serviço #${o.id}: ${o.publicUrl.orEmpty()}")}".toUri()))
                            }, Modifier.fillMaxWidth(), icon = Icons.Outlined.Chat, enabled = !o.publicUrl.isNullOrBlank())
                        }
                    }
                }
                // janela "Assinatura do cliente" (.signaturePad do web)
                if (sigDialog) AlertDialog(
                    onDismissRequest = { sigDialog = false }, containerColor = Web.surface,
                    title = { Text("Assinatura do cliente") },
                    text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SignaturePad(pad2); WebButton("Limpar", pad2::clear) } },
                    confirmButton = { TextButton({ if (!pad2.isEmpty) { sigNew = pad2.toDataUrl(); removeSig = false }; sigDialog = false }) { Text("Salvar", color = Web.accent) } },
                    dismissButton = { TextButton({ sigDialog = false }) { Text("Cancelar", color = Web.accent) } },
                )
                if (busy) Text("Salvando…", color = Web.muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Pausas: totais de deslocamento, pausa e trabalho, lista de atividades com ícone e, em andamento, os botões Pausar/Retomar atendimento. */
@Composable
fun PausesScreen(id: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "pau$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var pauseDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }
    when (val s = state) {
        Load.Loading -> LoadingBox()
        is Load.Error -> ErrorBox(s.message, onRetry = vm::load)
        is Load.Ok -> {
            val o = s.data
            TkPage("Pausas", onBack, null, snack, locked = o.isDone) {
                WebCard(padding = 14.dp) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Deslocamento" to o.hoursTravel, "Pausa" to o.hoursPause, "Trabalho" to o.hoursWork).forEach { (l, v) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(l, color = Web.muted, fontSize = 11.5.sp)
                                Text(hms(v), color = Web.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                WebCard(padding = 4.dp) {
                    if (o.hours.isEmpty()) Text("Nenhuma atividade registrada.", Modifier.fillMaxWidth().padding(20.dp), color = Web.muted, textAlign = TextAlign.Center)
                    o.hours.forEachIndexed { i, h ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val l = h.label.lowercase()
                            val isPause = l.contains("pausa")
                            val (icon, tint) = when {
                                isPause && (l.contains("início") || l.contains("inicio") || !l.contains("fim")) -> Icons.Outlined.Pause to Color(0xFFDC2626)
                                isPause -> Icons.Outlined.PlayArrow to Color(0xFFDC2626)
                                l.contains("check-out") -> Icons.Outlined.Check to Color(0xFF2563EB)
                                l.contains("check-in") -> Icons.Outlined.PlayArrow to Color(0xFF2563EB)
                                else -> Icons.Outlined.DirectionsCar to Color(0xFF2563EB)
                            }
                            IconTile(icon, tint, size = 34.dp)
                            Column(Modifier.weight(1f)) {
                                Text(h.label, color = Web.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(Fmt.dateTime(h.at) + (h.reason?.let { " · $it" } ?: ""), color = Web.muted, fontSize = 12.8.sp)
                            }
                        }
                        if (i < o.hours.lastIndex) Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(Web.line))
                    }
                }
                if (o.status == OrderStatus.EM_EXECUCAO) PillButton("Pausar atendimento", { pauseDialog = true }, Modifier.fillMaxWidth().padding(top = 10.dp), icon = Icons.Outlined.Pause, enabled = !busy)
                else if (o.status == OrderStatus.PAUSADA) PillButton("Retomar atendimento", { vm.setStatus(OrderStatus.EM_EXECUCAO, null) }, Modifier.fillMaxWidth().padding(top = 10.dp), filled = true, icon = Icons.Outlined.PlayArrow, enabled = !busy)
                if (pauseDialog) PauseDialog(onDismiss = { pauseDialog = false }, onConfirm = { pauseDialog = false; vm.setStatus(OrderStatus.PAUSADA, it) })
            }
        }
    }
}
/** Peças e Serviços lançados na OS (só consulta; lançar e revisar continua no sistema web). */
@Composable
fun OrderItemsScreen(id: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "itm$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    when (val s = state) {
        Load.Loading -> LoadingBox()
        is Load.Error -> ErrorBox(s.message, onRetry = vm::load)
        is Load.Ok -> {
            val o = s.data
            TkPage("Peças e Serviços", onBack, null, snack, locked = o.isDone) {
                WebCard(padding = 4.dp) {
                    if (o.items.isEmpty()) Text("Nenhuma peça ou serviço lançado.", Modifier.fillMaxWidth().padding(20.dp), color = Web.muted, textAlign = TextAlign.Center)
                    o.items.forEach { i ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(i.description, color = Web.text, fontSize = 14.4.sp)
                                Text(
                                    (o.itemKinds[i.kind] ?: i.kind) + " · ${i.quantity.toString().removeSuffix(".0")} × ${Fmt.money(i.unitPrice)}" + if (i.review) " · aguardando revisão" else "",
                                    color = Web.muted, fontSize = 12.5.sp,
                                )
                            }
                            Text(Fmt.money(i.total), color = Web.text, fontSize = 14.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line))
                    }
                    if (o.items.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Total", Modifier.weight(1f), color = Web.text, fontWeight = FontWeight.Bold)
                        Text(Fmt.money(o.itemsTotal), color = Web.text, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Orçamentos ligados à OS: a lista fica no sistema web (aqui só o aviso). */
@Composable
fun OrderQuotesScreen(onBack: () -> Unit) {
    val snack = remember { SnackbarHostState() }
    TkPage("Orçamentos", onBack, null, snack) {
        Text("Os orçamentos desta OS ficam em Orçamentos, no menu.", color = Web.muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
    }
}
