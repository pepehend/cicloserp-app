package com.phdevtech.cicloserp.ui.order

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

/** Responder um questionário da OS (POST so_answers_save). Perguntas condicionais aparecem conforme as respostas. */
@OptIn(ExperimentalMaterial3Api::class)
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
    var onlyEmpty by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(qn?.name ?: "Questionário") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (o != null && qn != null && canChange(me, o)) {
                Button(
                    enabled = !busy,
                    onClick = {
                        val vis = QuestionLogic.visibleMap(qn.questions, answers)
                        val toSend = answers.filterKeys { vis[it] == true }
                        vm.run(null, then = onBack) {
                            val missing = saveAnswers(id, qn.id, toSend)
                            vm.message(if (missing.isEmpty()) "Questionário salvo" else "Salvo. Ainda faltam: ${missing.joinToString(", ")}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).height(52.dp),
                ) { Text("Salvar respostas") }
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
                val (filled, total) = QuestionLogic.progress(qn.questions, answers)
                Column(Modifier.fillMaxSize().padding(pad).imePadding()) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$filled de $total respondidas", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        FilterChip(selected = onlyEmpty, onClick = { onlyEmpty = !onlyEmpty }, label = { Text("Sem resposta") })
                    }
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        var n = 0
                        qn.questions.forEach { q ->
                            if (vis[q.id] != true) return@forEach
                            n++
                            if (onlyEmpty && QuestionLogic.filled(q, answers[q.id])) return@forEach
                            QuestionCard(n, q, answers[q.id], editable) { v -> if (v == null) answers.remove(q.id) else answers[q.id] = v }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionCard(n: Int, q: Question, value: JsonElement?, editable: Boolean, onChange: (JsonElement?) -> Unit) {
    val text = value.asStr().orEmpty()
    val setText: (String) -> Unit = { onChange(if (it.isEmpty()) null else JsonPrimitive(it)) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "$n. ${q.label}" + if (q.required) " *" else "",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            )
            q.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            when (q.type) {
                "check" -> Row(Modifier.clickable(enabled = editable) { setText(if (text == "Sim") "" else "Sim") }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = text == "Sim", onCheckedChange = { setText(if (it) "Sim" else "") }, enabled = editable)
                    Text(if (text == "Sim") "Sim" else "Não marcado")
                }
                "itens" -> q.options.forEach { opt ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = editable) { setText(if (text == opt) "" else opt) }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = text == opt, onClick = { setText(if (text == opt) "" else opt) }, enabled = editable)
                        Text(opt)
                    }
                }
                "multipla" -> {
                    val sel = QuestionLogic.asList(value)
                    q.options.forEach { opt ->
                        val on = opt in sel
                        val toggle = {
                            val next = if (on) sel - opt else q.options.filter { it in sel || it == opt }
                            onChange(if (next.isEmpty()) null else JsonArray(next.map(::JsonPrimitive)))
                        }
                        Row(Modifier.fillMaxWidth().clickable(enabled = editable, onClick = toggle), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = on, onCheckedChange = { toggle() }, enabled = editable)
                            Text(opt)
                        }
                    }
                }
                "data" -> DateField(text, editable, setText)
                "hora" -> TimeField(text, editable, setText)
                "assinatura" -> SignatureAnswer(text, editable, setText)
                "foto" -> PhotoAnswer(value, editable, onChange)
                else -> {
                    val (kb, multi) = when (q.type) {
                        "numerica", "monetaria" -> KeyboardType.Decimal to false
                        "documento" -> KeyboardType.Number to false
                        "area" -> KeyboardType.Text to true
                        else -> KeyboardType.Text to false
                    }
                    OutlinedTextField(
                        value = if (q.type == "documento") Fmt.doc(text) else text,
                        onValueChange = { v ->
                            setText(when (q.type) {
                                "documento" -> v.filter(Char::isDigit).take(14)
                                "numerica", "monetaria" -> v.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(20)
                                else -> v.take(if (q.type == "area") 5000 else 1000)
                            })
                        },
                        enabled = editable, singleLine = !multi,
                        prefix = if (q.type == "monetaria") ({ Text("R$ ") }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = kb),
                        modifier = Modifier.fillMaxWidth().then(if (multi) Modifier.heightIn(min = 110.dp) else Modifier),
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
    var drawing by remember { mutableStateOf(value.isEmpty()) }
    val pad = rememberSignatureState()
    if (value.isNotEmpty() && !drawing) {
        Images.decodeDataUrl(value)?.let { Image(it, "Assinatura", Modifier.fillMaxWidth().height(150.dp).background(Color.White), contentScale = ContentScale.Fit) }
        if (editable) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { drawing = true }) { Text("Assinar de novo") }
            TextButton(onClick = { onChange(""); drawing = true }) { Text("Remover") }
        }
    } else if (editable) {
        SignaturePad(pad)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = pad::clear) { Text("Limpar") }
            Button(onClick = { pad.toDataUrl()?.let { onChange(it); drawing = false } }) { Text("Usar assinatura") }
        }
    } else Text("Sem assinatura", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
