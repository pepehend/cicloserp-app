package com.phdevtech.cicloserp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.api.ApiException
import com.phdevtech.cicloserp.data.model.Customer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Valores dos campos de um formulário (chave = nome do campo da API). */
class FormState(initial: Map<String, String> = emptyMap()) {
    private val values = mutableStateMapOf<String, String>().apply { putAll(initial) }
    operator fun get(key: String): String = values[key].orEmpty()
    operator fun set(key: String, v: String) { values[key] = v }

    /** Todos os campos como texto (o servidor limpa e valida); [numeric] troca vírgula por ponto. */
    fun toJson(numeric: Set<String> = emptySet(), extra: Map<String, JsonElement> = emptyMap()): JsonObject {
        val m = LinkedHashMap<String, JsonElement>()
        values.forEach { (k, v) -> m[k] = JsonPrimitive(if (k in numeric) v.trim().replace(',', '.') else v.trim()) }
        m.putAll(extra)
        return JsonObject(m)
    }
}

/** Estrutura de tela de formulário: barra com voltar, campos roláveis, erro do servidor e botão Salvar fixo embaixo. */
@Composable
fun FormScaffold(
    title: String, onBack: () -> Unit, saving: Boolean, error: String?, onSave: () -> Unit,
    saveLabel: String = "Salvar", content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackBar(title, onBack) },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (error != null) Text(error, Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) {
                        if (saving) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) else Text(saveLabel)
                    }
                }
            }
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun TextF(
    form: FormState, key: String, label: String, modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1, required: Boolean = false,
    caps: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    OutlinedTextField(
        value = form[key], onValueChange = { form[key] = it },
        label = { Text(if (required) "$label *" else label) },
        singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = caps),
        shape = RoundedCornerShape(12.dp), modifier = modifier,
    )
}

/** Lista suspensa: [options] = (valor, texto). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownF(label: String, options: List<Pair<String, String>>, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second.orEmpty(), onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (v, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { onChange(v); open = false }) }
        }
    }
}

/** Data (AAAA-MM-DD na API, dd/MM/aaaa na tela) escolhida num calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateF(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var show by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = if (value.isBlank()) "" else com.phdevtech.cicloserp.util.Fmt.date(value), onValueChange = {}, readOnly = true,
            label = { Text(label) }, trailingIcon = { Icon(Icons.Outlined.CalendarMonth, null) },
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val initial = runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        val st = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton({
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton({ onChange(""); show = false }) { Text("Limpar") } },
        ) { DatePicker(st) }
    }
}

/** Hora (HH:mm) escolhida num relógio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeF(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var show by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.Schedule, null) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val h = value.take(2).toIntOrNull() ?: 9
        val m = value.drop(3).take(2).toIntOrNull() ?: 0
        val st = rememberTimePickerState(h, m, true)
        AlertDialog(
            onDismissRequest = { show = false },
            text = { TimePicker(st) },
            confirmButton = { TextButton({ onChange("%02d:%02d".format(st.hour, st.minute)); show = false }) { Text("OK") } },
            dismissButton = { TextButton({ onChange(""); show = false }) { Text("Limpar") } },
        )
    }
}

/** Escolha do cliente numa janela com busca (GET customers). */
@Composable
fun CustomerPickerF(label: String, name: String, onPick: (Customer) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var show by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = name, onValueChange = {}, readOnly = true, label = { Text("$label *") },
            trailingIcon = { Icon(Icons.Outlined.Search, null) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val repo = repository()
        val scope = rememberCoroutineScope()
        var q by remember { mutableStateOf("") }
        var rows by remember { mutableStateOf<List<Customer>?>(null) }
        var err by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(q) {
            kotlinx.coroutines.delay(if (q.isEmpty()) 0 else 300)
            try { rows = repo.customers(q, false); err = null } catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.message }
        }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Escolher cliente") },
            text = {
                Column {
                    OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text("Nome, CNPJ/CPF ou cidade") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                    Box(Modifier.height(300.dp).padding(top = 8.dp)) {
                        when {
                            err != null -> Text(err!!, color = MaterialTheme.colorScheme.error)
                            rows == null -> LoadingBox()
                            rows!!.isEmpty() -> Text("Nenhum cliente encontrado.")
                            else -> LazyColumn {
                                items(rows!!, key = { it.id }) { c ->
                                    Column(Modifier.fillMaxWidth().clickable { onPick(c); show = false }.padding(vertical = 10.dp)) {
                                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                        c.document?.let { Text(com.phdevtech.cicloserp.util.Fmt.doc(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ show = false }) { Text("Cancelar") } },
        )
    }
}

/** Pergunta de confirmação antes de uma ação sem volta (excluir). */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar") } },
    )
}

/** Roda uma gravação mostrando "salvando" e o erro do servidor; chama [onOk] se der certo. */
class SaveRunner(private val scope: kotlinx.coroutines.CoroutineScope) {
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    fun run(block: suspend () -> Unit, onOk: () -> Unit) {
        if (saving) return
        saving = true; error = null
        scope.launch {
            try { block(); onOk() } catch (e: CancellationException) { throw e } catch (e: ApiException) { error = e.message } catch (e: Exception) {
                error = e.message ?: "Não foi possível salvar."
            } finally { saving = false }
        }
    }
}

@Composable
fun rememberSaveRunner(): SaveRunner {
    val scope = rememberCoroutineScope()
    return remember { SaveRunner(scope) }
}
