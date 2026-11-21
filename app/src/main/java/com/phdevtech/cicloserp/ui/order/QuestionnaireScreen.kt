package com.phdevtech.cicloserp.ui.order
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.dashed
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.PillButton
import com.phdevtech.cicloserp.ui.common.IconTile
import com.phdevtech.cicloserp.ui.common.TimeF
import com.phdevtech.cicloserp.ui.common.DateF
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.asStr
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.data.model.Question
import com.phdevtech.cicloserp.data.model.QuestionLogic
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SignaturePad
import com.phdevtech.cicloserp.ui.common.appViewModel
import com.phdevtech.cicloserp.ui.common.rememberSignatureState
import com.phdevtech.cicloserp.util.Fmt
import com.phdevtech.cicloserp.util.Images
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Selo de situação do questionário (.tk-qs): NÃO INICIADO, EM ANDAMENTO ou FINALIZADO. */
@Composable
private fun StateTag(state: String) {
    val (bg, fg, label) = when (state) {
        "fin" -> Triple(Web.okSoft, Web.ok, "FINALIZADO")
        "and" -> Triple(Web.warnSoft, Web.warn, "EM ANDAMENTO")
        else -> Triple(Web.surface2, Web.muted, "NÃO INICIADO")
    }
    Text("• $label", Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp), color = fg, fontSize = 10.6.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
}

/**
 * Página Questionários (tkQuestionnaires): título, aviso de só consulta, filtros Todos/Pendentes/Em andamento/Finalizados com contagem
 * e um cartão por questionário (nome, "N de 1 finalizado", equipamento, progresso e situação).
 */
@Composable
fun QuestionnairesListScreen(me: Me, id: Int, onBack: () -> Unit, onOpen: (Int) -> Unit) {
    val vm = appViewModel(key = "ql$id") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }
    var filter by rememberSaveable { mutableStateOf("todos") }
    Scaffold(containerColor = Web.bg) { pad ->
        when (val s = state) {
            Load.Loading -> LoadingBox(Modifier.padding(pad))
            is Load.Error -> ErrorBox(s.message, onRetry = vm::load, modifier = Modifier.padding(pad))
            is Load.Ok -> {
                val o = s.data
                val info = o.questionnaires.map { q -> q to (if (q.filled == 0) "nao" else if (q.missing.isNotEmpty()) "and" else "fin") }
                val counts = mapOf(
                    "todos" to info.size, "pendentes" to info.count { it.second != "fin" },
                    "andamento" to info.count { it.second == "and" }, "finalizados" to info.count { it.second == "fin" },
                )
                val shown = info.filter { (_, st) -> filter == "todos" || (if (filter == "pendentes") st != "fin" else if (filter == "andamento") st == "and" else st == "fin") }
                Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Web.text) }
                    }
                    Text("Questionários", color = Web.text, fontSize = 20.25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    if (!canChange(me, o)) LockNote("Ordem concluída: só consulta. Para alterar, use o computador.")
                    // filtros (.tk-chips.dark): o escolhido fica escuro
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("todos" to "Todos", "pendentes" to "Pendentes", "andamento" to "Em andamento", "finalizados" to "Finalizados").forEach { (k, l) ->
                            val on = filter == k
                            Row(
                                Modifier.clip(RoundedCornerShape(50)).background(if (on) Web.text else Web.surface).border(1.dp, if (on) Web.text else Web.line, RoundedCornerShape(50))
                                    .clickable { filter = k }.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(l, color = if (on) Web.surface else Web.text, fontSize = 12.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                                Text("${counts[k]}", Modifier.clip(RoundedCornerShape(50)).background(if (on) Web.surface else Web.surface2).padding(horizontal = 6.dp), color = Web.text, fontSize = 11.sp)
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (shown.isEmpty()) Text(
                            if (o.questionnaires.isEmpty()) "Esta tarefa não tem questionários." else "Nenhum questionário neste filtro.",
                            Modifier.fillMaxWidth().padding(vertical = 28.dp), color = Web.muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        shown.forEach { (q, st) ->
                            val pct = if (q.total > 0) q.filled * 100 / q.total else 0
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Web.surface).border(1.dp, Web.line, RoundedCornerShape(14.dp)).clickable { onOpen(q.id) }) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconTile(Icons.Outlined.Description, Color(0xFF7C3AED), size = 34.dp)
                                    Column(Modifier.weight(1f)) {
                                        Text(q.name, color = Web.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("${if (st == "fin") 1 else 0} de 1 finalizado", color = Web.muted, fontSize = 12.5.sp)
                                    }
                                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(16.dp), tint = Web.muted)
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line))
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Outlined.Settings, null, Modifier.size(16.dp), tint = Web.accent)
                                        Text(o.equipmentName ?: "Sem equipamento na OS", color = Web.text, fontSize = 14.sp)
                                    }
                                    if (q.answered && q.answeredAt != null) Text(whenText(q.answeredAt), color = Web.text, fontSize = 12.8.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(Web.line)) {
                                            Box(Modifier.fillMaxWidth(pct / 100f).height(5.dp).clip(RoundedCornerShape(50)).background(Web.ok))
                                        }
                                        Text("$pct%", color = Web.text, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        StateTag(st)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Responder um questionário da OS (POST so_answers_save): progresso, equipamento, abas Todas / Foco (uma pergunta por vez) / Sem resposta,
 * e cartões com faixa lateral (vermelha obrigatória sem resposta, verde respondida). Perguntas condicionais aparecem conforme as respostas.
 */
@Composable
fun QuestionnaireScreen(me: Me, id: Int, questionnaireId: Int, onBack: () -> Unit) {
    val vm = appViewModel(key = "q$id-$questionnaireId") { OrderViewModel(it, id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm) { vm.messages.collect { snack.showSnackbar(it) } }

    val o = (state as? Load.Ok)?.data
    val qn = o?.questionnaires?.firstOrNull { it.id == questionnaireId }
    // Respostas em edição (começam com as salvas no servidor)
    val answers = remember(qn?.id, o != null) { mutableStateMapOf<String, JsonElement>().apply { qn?.answers?.let { putAll(it) } } }
    var mode by rememberSaveable { mutableIntStateOf(0) }   // 0 Todas, 1 Foco, 2 Sem resposta
    var idx by rememberSaveable { mutableIntStateOf(0) }

    val save = {
        if (qn != null) {
            val vis = QuestionLogic.visibleMap(qn.questions, answers)
            val toSend = answers.filterKeys { vis[it] == true }
            vm.run(null, then = onBack) {
                val missing = saveAnswers(id, qn.id, toSend)
                vm.message(if (missing.isEmpty()) "Questionário salvo" else "Salvo. Falta responder ${missing.size} obrigatória(s)")
            }
        }
    }

    Scaffold(
        containerColor = Web.bg, snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (o != null && qn != null && mode == 1) {
                val vq = qn.questions.filter { QuestionLogic.visibleMap(qn.questions, answers)[it.id] == true }
                Row(Modifier.fillMaxWidth().background(Web.bg).navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("‹ Anterior", { if (idx > 0) idx-- }, Modifier.weight(1f), enabled = idx > 0)
                    PillButton(if (idx >= vq.size - 1) "Concluir" else "Próxima ›", { if (idx >= vq.size - 1) { if (canChange(me, o)) save() else onBack() } else idx++ }, Modifier.weight(1f), filled = true)
                }
            }
        },
    ) { pad ->
        when {
            state is Load.Loading -> LoadingBox(Modifier.padding(pad))
            state is Load.Error -> ErrorBox((state as Load.Error).message, onRetry = vm::load, modifier = Modifier.padding(pad))
            qn == null -> ErrorBox("Este questionário não faz parte da OS.", onRetry = null, modifier = Modifier.padding(pad))
            else -> {
                val editable = canChange(me, o)
                val vis = QuestionLogic.visibleMap(qn.questions, answers)
                val vq = qn.questions.filter { vis[it.id] == true }
                val (filled, total) = QuestionLogic.progress(qn.questions, answers)
                Column(Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
                    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Web.text) }
                        Spacer(Modifier.weight(1f))
                        if (editable) TextButton(onClick = { save() }, enabled = !busy) { Text("Salvar", color = Web.accent, fontSize = 16.sp, fontWeight = FontWeight(650)) }
                    }
                    Text(qn.name, color = Web.text, fontSize = 20.25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
                    if (!editable) LockNote("Ordem concluída: só consulta. Para alterar, use o computador.")
                    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)).background(Web.line)) {
                            Box(Modifier.fillMaxWidth(if (total > 0) filled / total.toFloat() else 0f).height(5.dp).clip(RoundedCornerShape(50)).background(Web.accent))
                        }
                        Text("$filled de $total", color = Web.muted, fontSize = 12.8.sp)
                    }
                    o.equipment?.let { eq ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Settings, null, Modifier.size(20.dp), tint = Web.text)
                            Text(eq, color = Web.text, fontSize = 15.sp)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Web.line))
                    }
                    Tabs(listOf("Todas", "Foco", "Sem resposta"), mode) { mode = it; if (it == 1) idx = 0 }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        fun num(q: Question) = qn.questions.indexOf(q) + 1
                        val set: (Question) -> (JsonElement?) -> Unit = { q -> { v -> if (v == null) answers.remove(q.id) else answers[q.id] = v } }
                        when (mode) {
                            0 -> vq.forEach { q -> QuestionCard(num(q), q, answers[q.id], editable, set(q)) }
                            2 -> {
                                val list = vq.filter { !QuestionLogic.filled(it, answers[it.id]) }
                                if (list.isEmpty()) Text("Todas as perguntas foram respondidas.", Modifier.fillMaxWidth().padding(vertical = 28.dp), color = Web.muted, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                list.forEach { q -> QuestionCard(num(q), q, answers[q.id], editable, set(q)) }
                            }
                            else -> {
                                val i = idx.coerceIn(0, (vq.size - 1).coerceAtLeast(0))
                                val q = vq.getOrNull(i)
                                if (q == null) Text("Nenhuma pergunta.", Modifier.fillMaxWidth().padding(vertical = 28.dp), color = Web.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                else {
                                    Text("PERGUNTA ${i + 1} DE ${vq.size}", color = Web.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(Web.line)) {
                                        Box(Modifier.fillMaxWidth((i + 1) / vq.size.toFloat()).height(4.dp).clip(RoundedCornerShape(50)).background(Web.accent))
                                    }
                                    QuestionCard(num(q), q, answers[q.id], editable, set(q))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** Opção de resposta (.tk-opt): linha com borda, destaque vermelho claro quando escolhida; redonda (uma) ou quadrada (várias). */
@Composable
private fun OptionRow(text: String, selected: Boolean, square: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(if (selected) Web.accentSoft else Web.surface).border(1.dp, if (selected) Web.accent else Web.line, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp).alpha(if (enabled) 1f else .7f),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (square) Checkbox(selected, null, enabled = enabled, modifier = Modifier.size(18.dp), colors = CheckboxDefaults.colors(checkedColor = Web.accent, checkmarkColor = Web.accentInk, uncheckedColor = Web.muted))
        else RadioButton(selected, null, enabled = enabled, modifier = Modifier.size(18.dp), colors = RadioButtonDefaults.colors(selectedColor = Web.accent, unselectedColor = Web.muted))
        Text(text, color = Web.text, fontSize = 14.4.sp, fontWeight = FontWeight.Medium)
    }
}

/** Cartão de pergunta (.tk-qq): faixa lateral de 4dp (vermelha: obrigatória sem resposta; verde: respondida), número e texto em negrito. */
@Composable
private fun QuestionCard(n: Int, q: Question, value: JsonElement?, editable: Boolean, onChange: (JsonElement?) -> Unit) {
    val text = value.asStr().orEmpty()
    val setText: (String) -> Unit = { onChange(if (it.isEmpty()) null else JsonPrimitive(it)) }
    val filledNow = QuestionLogic.filled(q, value)
    val stripe = if (q.required && !filledNow) Color(0xFFDC2626) else if (filledNow) Web.ok else Web.line
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(Web.surface).border(1.dp, Web.line, shape).height(IntrinsicSize.Min)) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(stripe))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$n. ${q.label}" + if (q.required) "*" else "", color = Web.text, fontSize = 14.2.sp, fontWeight = FontWeight.Bold)
            q.description?.let { Text(it, color = Web.muted, fontSize = 12.8.sp, modifier = Modifier.padding(top = (-4).dp)) }
            when (q.type) {
                "check" -> OptionRow("Sim", text == "Sim", true, editable) { setText(if (text == "Sim") "" else "Sim") }
                "itens" -> q.options.forEach { opt -> OptionRow(opt, text == opt, false, editable) { setText(if (text == opt) "" else opt) } }
                "multipla" -> {
                    val sel = QuestionLogic.asList(value)
                    q.options.forEach { opt ->
                        val on = opt in sel
                        OptionRow(opt, on, true, editable) {
                            val next = if (on) sel - opt else q.options.filter { it in sel || it == opt }
                            onChange(if (next.isEmpty()) null else JsonArray(next.map(::JsonPrimitive)))
                        }
                    }
                }
                "data" -> DateF(null, text, setText, enabled = editable)
                "hora" -> TimeF(null, text, setText, enabled = editable)
                "assinatura" -> SignatureAnswer(text, editable, setText)
                "foto" -> PhotoAnswer(value, editable, onChange)
                else -> {
                    val (kb, lines) = when (q.type) {
                        "numerica", "monetaria" -> KeyboardType.Decimal to 1
                        "documento" -> KeyboardType.Number to 1
                        "area" -> KeyboardType.Text to 4
                        else -> KeyboardType.Text to 1
                    }
                    WebField(
                        if (q.type == "documento") Fmt.doc(text) else text,
                        { v ->
                            setText(when (q.type) {
                                "documento" -> v.filter(Char::isDigit).take(14)
                                "numerica", "monetaria" -> v.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(20)
                                else -> v.take(if (q.type == "area") 5000 else 1000)
                            })
                        },
                        Modifier.fillMaxWidth(), placeholder = if (q.type == "monetaria") "R$ 0,00" else "Resposta", keyboardType = kb, minLines = lines, enabled = editable,
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(value: String, editable: Boolean, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }, enabled = editable) { Text(if (value.isEmpty()) "Escolher data" else Fmt.date(value)) }
        if (value.isNotEmpty() && editable) TextButton(onClick = { onChange("") }) { Text("Limpar") }
    }
    if (open) {
        val initial = runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        val st = rememberDatePickerState(initialSelectedDateMillis = initial ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        ) { DatePicker(st) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(value: String, editable: Boolean, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }, enabled = editable) { Text(value.ifEmpty { "Escolher hora" }) }
        if (value.isNotEmpty() && editable) TextButton(onClick = { onChange("") }) { Text("Limpar") }
    }
    if (open) {
        val parts = value.split(":")
        val now = LocalDateTime.now()
        val st = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: now.hour, parts.getOrNull(1)?.toIntOrNull() ?: now.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Hora") },
            text = { TimeInput(st) },
            confirmButton = { TextButton(onClick = { onChange("%02d:%02d".format(st.hour, st.minute)); open = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SignatureAnswer(value: String, editable: Boolean, onChange: (String) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    val pad = rememberSignatureState()
    // quadro pequeno e tracejado (.tk-sigbox.sm): toque para assinar
    Box(
        Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp)).background(Web.surface2).dashed(Web.line, 12.dp).clickable(enabled = editable) { pad.clear(); dialog = true },
        contentAlignment = Alignment.Center,
    ) {
        val img = remember(value.length) { Images.decodeDataUrl(value.ifEmpty { null }) }
        if (img != null) Image(img, "Assinatura", Modifier.fillMaxSize().padding(4.dp).background(Color.White, RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
        else Text("Toque para assinar", color = Web.muted, fontSize = 14.sp)
    }
    if (value.isNotEmpty() && editable) Text("Remover assinatura", Modifier.clickable { onChange("") }, color = Web.accent, fontSize = 13.6.sp)
    if (dialog) AlertDialog(
        onDismissRequest = { dialog = false }, containerColor = Web.surface, title = { Text("Assinatura") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SignaturePad(pad); WebButton("Limpar", pad::clear) } },
        confirmButton = { TextButton({ if (!pad.isEmpty) pad.toDataUrl()?.let(onChange); dialog = false }) { Text("Salvar", color = Web.accent) } },
        dismissButton = { TextButton({ dialog = false }) { Text("Cancelar", color = Web.accent) } },
    )
}
/** Foto como resposta: lista de {src, at} (até 10 por pergunta). */
@Composable
private fun PhotoAnswer(value: JsonElement?, editable: Boolean, onChange: (JsonElement?) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val list = (value as? JsonArray).orEmpty().mapNotNull { it.asObj() }
    var camUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val add: (Uri) -> Unit = { uri ->
        scope.launch {
            runCatching { Images.photoDataUrl(ctx, uri, maxSide = 1280) }
                .onSuccess { src ->
                    val at = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    onChange(JsonArray(list + buildJsonObject { put("src", src); put("at", at) }))
                }
                .onFailure { error = "Não consegui ler a foto" }
        }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) camUri?.let(add) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(add) }

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEachIndexed { i, ph ->
            val img = remember(ph.str("src")?.length, i) { Images.decodeDataUrl(ph.str("src"), 300) }
            Box {
                if (img != null) Image(img, null, Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                if (editable) IconButton(onClick = {
                    val next = list.filterIndexed { j, _ -> j != i }
                    onChange(if (next.isEmpty()) null else JsonArray(next))
                }, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
                    Icon(Icons.Outlined.Close, "Remover foto", tint = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)))
                }
            }
        }
    }
    if (editable && list.size < 10) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { Images.newCameraUri(ctx).also { camUri = it; camera.launch(it) } }) { Icon(Icons.Outlined.PhotoCamera, null); Text(" Câmera") }
        OutlinedButton(onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Outlined.PhotoLibrary, null); Text(" Galeria") }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
