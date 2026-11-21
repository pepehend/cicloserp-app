package com.phdevtech.cicloserp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdevtech.cicloserp.data.api.ApiException
import com.phdevtech.cicloserp.data.model.Customer
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
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

/**
 * Folha de formulário igual à janela (.modal) do sistema web no celular: título com X, divisor, campos rolando,
 * e rodapé fixo com "Cancelar" e o botão vermelho de salvar lado a lado.
 */
@Composable
fun FormScaffold(
    title: String, onBack: () -> Unit, saving: Boolean, error: String?, onSave: () -> Unit,
    saveLabel: String = "Salvar", content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(containerColor = Web.surface) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Web.text, fontSize = 20.sp, fontWeight = FontWeight(600))
                IconButton(onClick = onBack) { Icon(Icons.Outlined.Close, "Fechar", tint = Web.text) }
            }
            HorizontalDivider(color = Web.line)
            Column(
                Modifier.weight(1f).fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp), content = content,
            )
            HorizontalDivider(color = Web.line)
            if (error != null) Text(error, Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp), color = Web.danger, fontSize = 13.8.sp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WebButton("Cancelar", onBack, Modifier.weight(1f), enabled = !saving)
                WebButton(if (saving) "Salvando…" else saveLabel, onSave, Modifier.weight(1f), primary = true, enabled = !saving)
            }
        }
    }
}

/** Legenda de bloco (quando não há fieldset). */
@Composable
fun SectionLabel(text: String) {
    Text(text, Modifier.padding(top = 8.dp), color = Web.text, fontWeight = FontWeight(620), fontSize = 15.sp)
}

@Composable
fun TextF(
    form: FormState, key: String, label: String, modifier: Modifier = Modifier.fillMaxWidth(),
    keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1, required: Boolean = false,
    caps: KeyboardCapitalization = KeyboardCapitalization.Sentences, placeholder: String = "", help: String? = null,
) {
    WebField(form[key], { form[key] = it }, modifier, label, placeholder, help, required, keyboard, lines, caps)
}

/** Lista suspensa (<select>): [options] = (valor, texto). */
@Composable
fun DropdownF(
    label: String, options: List<Pair<String, String>>, value: String, modifier: Modifier = Modifier.fillMaxWidth(),
    required: Boolean = false, help: String? = null, onChange: (String) -> Unit,
) {
    WebSelect(options, value, onChange, modifier, label, required = required, help = help)
}

/** Data (AAAA-MM-DD na API, dd/MM/aaaa na tela) escolhida num calendário. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateF(label: String?, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    var show by remember { mutableStateOf(false) }
    WebPickBox(if (value.isBlank()) "" else Fmt.date(value), { if (enabled) show = true }, modifier, label, placeholder = "dd/mm/aaaa", icon = Icons.Outlined.CalendarMonth)
    if (show) {
        val initial = runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        val st = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton({
                    st.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    show = false
                }) { Text("OK", color = Web.accent) }
            },
            dismissButton = { TextButton({ onChange(""); show = false }) { Text("Limpar", color = Web.accent) } },
        ) { DatePicker(st) }
    }
}

/** Hora (HH:mm) escolhida num relógio. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeF(label: String?, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    var show by remember { mutableStateOf(false) }
    WebPickBox(value, { if (enabled) show = true }, modifier, label, placeholder = "--:--", icon = Icons.Outlined.Schedule)
    if (show) {
        val h = value.take(2).toIntOrNull() ?: 9
        val m = value.drop(3).take(2).toIntOrNull() ?: 0
        val st = rememberTimePickerState(h, m, true)
        AlertDialog(
            onDismissRequest = { show = false },
            text = { TimePicker(st) },
            confirmButton = { TextButton({ onChange("%02d:%02d".format(st.hour, st.minute)); show = false }) { Text("OK", color = Web.accent) } },
            dismissButton = { TextButton({ onChange(""); show = false }) { Text("Limpar", color = Web.accent) } },
        )
    }
}

/** Escolha do cliente numa janela com busca (GET customers). */
@Composable
fun CustomerPickerF(label: String, name: String, onPick: (Customer) -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    var show by remember { mutableStateOf(false) }
    WebPickBox(name, { show = true }, modifier, label, required = true)
    if (show) {
        val repo = repository()
        var q by remember { mutableStateOf("") }
        var rows by remember { mutableStateOf<List<Customer>?>(null) }
        var err by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(q) {
            kotlinx.coroutines.delay(if (q.isEmpty()) 0 else 300)
            try { rows = repo.customers(q, false); err = null } catch (e: CancellationException) { throw e } catch (e: Exception) { err = e.message }
        }
        AlertDialog(
            onDismissRequest = { show = false },
            containerColor = Web.surface,
            title = { Text("Escolher cliente", fontWeight = FontWeight(600)) },
            text = {
                Column {
                    WebField(q, { q = it }, Modifier.fillMaxWidth(), placeholder = "Nome, CNPJ/CPF ou cidade")
                    Box(Modifier.height(300.dp).padding(top = 8.dp)) {
                        when {
                            err != null -> Text(err!!, color = Web.danger)
                            rows == null -> LoadingBox()
                            rows!!.isEmpty() -> Text("Nenhum cliente encontrado.", color = Web.muted)
                            else -> LazyColumn {
                                items(rows!!, key = { it.id }) { c ->
                                    Column(Modifier.fillMaxWidth().clickable { onPick(c); show = false }.padding(vertical = 10.dp)) {
                                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                        c.document?.let { Text(Fmt.doc(it), style = MaterialTheme.typography.bodySmall, color = Web.muted) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton({ show = false }) { Text("Cancelar", color = Web.accent) } },
        )
    }
}

/** Pergunta de confirmação antes de uma ação sem volta (excluir). */
@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Web.surface, title = { Text(title, fontWeight = FontWeight(600)) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirmLabel, color = Web.danger) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = Web.accent) } },
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
