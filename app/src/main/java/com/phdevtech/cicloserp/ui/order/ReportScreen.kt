package com.phdevtech.cicloserp.ui.order
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.outlined.DeleteOutline
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
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, "https://wa.me/$phone?text=${android.net.Uri.encode("Ordem de serviço #${o.number}: ${o.publicUrl.orEmpty()}")}".toUri()))
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

/** Controle de horas: totais de deslocamento, pausa e trabalho, lista de atividades com ícone e, em andamento, os botões Pausar/Retomar atendimento. */
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
            TkPage("Controle de horas", onBack, null, snack, locked = o.isDone) {
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
/** Alternador Peça | Serviço (.tk-toggle): pílula cinza com o escolhido em destaque. */
@Composable
private fun PillToggle(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Web.surface2).border(1.dp, Web.line, RoundedCornerShape(50)).padding(2.dp)) {
        options.forEach { (v, label) ->
            val on = v == selected
            Text(
                label, Modifier.clip(RoundedCornerShape(50)).background(if (on) Web.surface else Color.Transparent).clickable { onSelect(v) }.padding(horizontal = 18.dp, vertical = 7.dp),
                color = if (on) Web.accent else Web.muted, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Peças e Serviços da OS (tkParts): itens lançados com remover e total, aviso de revisão e, se a OS pode ser alterada, o formulário
 * Peça|Serviço do cadastro (ou item novo que vai para revisão), quantidade, valor unitário e "+ Adicionar" (POST so_item_save / so_item_delete).
 */
@Composable
fun OrderItemsScreen(me: Me, id: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "itm$id") { OrderViewModel(it, id) }
    val repo = com.phdevtech.cicloserp.ui.common.repository()
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }
    var kind by rememberSaveable { mutableStateOf("produto") }
    var isNew by rememberSaveable { mutableStateOf(false) }
    var picked by remember { mutableStateOf<com.phdevtech.cicloserp.data.model.Product?>(null) }
    var desc by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable { mutableStateOf("1") }
    var price by rememberSaveable { mutableStateOf("") }
    var removing by remember { mutableStateOf<Int?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    val noun = if (kind == "servico") "serviço" else "peça"

    when (val s = state) {
        Load.Loading -> LoadingBox()
        is Load.Error -> ErrorBox(s.message, onRetry = vm::load)
        is Load.Ok -> {
            val o = s.data
            val editable = canChange(me, o)
            TkPage("Peças e Serviços", onBack, null, snack, locked = !editable) {
                if (o.itemsReview > 0) Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Web.warnSoft, RoundedCornerShape(14.dp)).border(1.dp, Web.warn.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WarningAmber, null, Modifier.size(20.dp), tint = Web.warn)
                    Text("${o.itemsReview} ${if (o.itemsReview == 1) "item novo aguarda" else "itens novos aguardam"} revisão do cadastro", color = Web.warn, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                WebCard(padding = 4.dp) {
                    if (o.items.isEmpty()) Text("Nenhuma peça ou serviço lançado.", Modifier.fillMaxWidth().padding(vertical = 44.dp), color = Web.muted, fontSize = 16.sp, textAlign = TextAlign.Center)
                    o.items.forEach { i ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(i.description, color = Web.text, fontWeight = FontWeight.Bold, fontSize = 14.8.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text((o.itemKinds[i.kind] ?: i.kind) + " · ${i.quantity.toString().removeSuffix(".0")} × ${Fmt.money(i.unitPrice)}", color = Web.muted, fontSize = 12.5.sp)
                                    if (i.review) Text("aguardando revisão", Modifier.clip(RoundedCornerShape(50)).background(Web.warnSoft).padding(horizontal = 8.dp, vertical = 1.dp), color = Web.warn, fontSize = 11.sp)
                                }
                            }
                            Text(Fmt.money(i.total), color = Web.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (editable) IconBtn(Icons.Outlined.DeleteOutline, "Remover", size = 34.dp, bordered = false) { removing = i.id }
                        }
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(Web.line))
                    }
                    if (o.items.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Total", Modifier.weight(1f), color = Web.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(Fmt.money(o.itemsTotal), color = Web.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                if (editable) {
                    Spacer(Modifier.height(10.dp))
                    WebCard(padding = 14.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PillToggle(listOf("produto" to "Peça", "servico" to "Serviço"), kind) { kind = it; picked = null }
                            val checkRow: @Composable () -> Unit = {
                                val shape = RoundedCornerShape(10.dp)
                                Row(
                                    Modifier.fillMaxWidth().clip(shape).background(Web.surface).border(1.dp, if (isNew) Web.accent else Web.line, shape).clickable { isNew = !isNew }.padding(horizontal = 12.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Checkbox(isNew, null, Modifier.size(18.dp), colors = CheckboxDefaults.colors(checkedColor = Web.accent, checkmarkColor = Web.accentInk, uncheckedColor = Web.muted))
                                    Text(if (kind == "servico") "O serviço não está cadastrado (vai para revisão)" else "A peça não está cadastrada (vai para revisão)", color = Web.text, fontSize = 14.4.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            if (isNew) {
                                checkRow()
                                WebField(desc, { desc = it.take(200) }, Modifier.fillMaxWidth(), label = if (kind == "servico") "Nome do serviço novo" else "Nome da peça novo", placeholder = "Ex.: Rolamento 6205")
                            } else {
                                com.phdevtech.cicloserp.ui.common.WebPickBox(
                                    picked?.let { p -> p.name + (p.mfrCode ?: p.sku)?.let { " · $it" }.orEmpty() }.orEmpty(), { pickerOpen = true }, Modifier.fillMaxWidth(),
                                    label = if (kind == "servico") "Serviço do cadastro" else "Peça do cadastro", placeholder = "Digite para buscar",
                                )
                                checkRow()
                            }
                            WebField(qty, { qty = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(10) }, Modifier.fillMaxWidth(), label = "Quantidade", keyboardType = KeyboardType.Decimal)
                            WebField(price, { price = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(12) }, Modifier.fillMaxWidth(), label = "Valor unitário (R$)", placeholder = "0,00", keyboardType = KeyboardType.Decimal)
                            PillButton("+ Adicionar", {
                                when {
                                    !isNew && picked == null -> vm.message(if (kind == "servico") "Escolha o serviço do cadastro (ou marque que não está cadastrado)" else "Escolha a peça do cadastro (ou marque que não está cadastrada)")
                                    isNew && desc.isBlank() -> vm.message("Informe o nome do item novo")
                                    else -> vm.run(if (isNew) "Item novo incluído: aguarda revisão do cadastro" else "Item incluído") {
                                        saveItem(id, kind, picked?.id.takeIf { !isNew }, if (isNew) desc.trim() else "", isNew, qty, price)
                                    }.also { picked = null; desc = ""; price = ""; qty = "1"; isNew = false }
                                }
                            }, Modifier.fillMaxWidth(), filled = true, enabled = !busy)
                        }
                    }
                }
                removing?.let { itemId ->
                    AlertDialog(
                        onDismissRequest = { removing = null }, containerColor = Web.surface, title = { Text("Remover este item da tarefa?") },
                        confirmButton = { TextButton({ removing = null; vm.run("Item removido") { deleteItem(itemId) } }) { Text("Remover", color = Web.danger) } },
                        dismissButton = { TextButton({ removing = null }) { Text("Cancelar", color = Web.accent) } },
                    )
                }
                if (pickerOpen) {
                    var q by remember { mutableStateOf("") }
                    var rows by remember { mutableStateOf<List<com.phdevtech.cicloserp.data.model.Product>?>(null) }
                    LaunchedEffect(q, kind) {
                        kotlinx.coroutines.delay(if (q.isEmpty()) 0 else 300)
                        rows = runCatching { repo.products(kind, q, false, false) }.getOrDefault(emptyList())
                    }
                    AlertDialog(
                        onDismissRequest = { pickerOpen = false }, containerColor = Web.surface, title = { Text(if (kind == "servico") "Escolher serviço" else "Escolher peça") },
                        text = {
                            Column {
                                WebField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = "Digite para buscar")
                                Box(Modifier.height(300.dp).padding(top = 8.dp)) {
                                    if (rows == null) LoadingBox() else if (rows!!.isEmpty()) Text("Nada encontrado.", color = Web.muted) else androidx.compose.foundation.lazy.LazyColumn {
                                        items(rows!!.size) { idx ->
                                            val p = rows!![idx]
                                            Column(Modifier.fillMaxWidth().clickable {
                                                picked = p; pickerOpen = false
                                                if (p.salePrice > 0) price = "%.2f".format(p.salePrice).replace('.', ',')
                                            }.padding(vertical = 10.dp)) {
                                                Text(p.name, color = Web.text, fontSize = 15.sp)
                                                Text(listOfNotNull(p.mfrCode ?: p.sku, Fmt.money(p.salePrice)).joinToString(" · "), color = Web.muted, fontSize = 12.5.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {}, dismissButton = { TextButton({ pickerOpen = false }) { Text("Cancelar", color = Web.accent) } },
                    )
                }
            }
        }
    }
}

/** Orçamentos ligados à OS (D.quotes): "Orçamento #n", relação e situação, total à direita; toca para abrir o orçamento. */
@Composable
fun OrderQuotesScreen(id: Int, onBack: () -> Unit, onOpen: ((Int) -> Unit)?) {
    val vm = appViewModel(key = "oq$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    when (val s = state) {
        Load.Loading -> LoadingBox()
        is Load.Error -> ErrorBox(s.message, onRetry = vm::load)
        is Load.Ok -> TkPage("Orçamentos", onBack, null, snack) {
            WebCard(padding = 4.dp) {
                if (s.data.quotes.isEmpty()) Text("Nenhum orçamento ligado a esta tarefa.", Modifier.fillMaxWidth().padding(vertical = 28.dp), color = Web.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
                s.data.quotes.forEach { q ->
                    Row(
                        Modifier.fillMaxWidth().let { if (onOpen != null) it.clickable { onOpen(q.id) } else it }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Orçamento #${q.id}", color = Web.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(listOfNotNull(q.relation, q.statusLabel).joinToString(" · "), color = Web.muted, fontSize = 13.sp)
                        }
                        Text(Fmt.money(q.total), color = Web.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}