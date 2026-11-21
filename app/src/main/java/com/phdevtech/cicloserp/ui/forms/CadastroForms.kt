package com.phdevtech.cicloserp.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.arr
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.int
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.data.text
import com.phdevtech.cicloserp.ui.common.BackBar
import com.phdevtech.cicloserp.ui.common.ConfirmDialog
import com.phdevtech.cicloserp.ui.common.CustomerPickerF
import com.phdevtech.cicloserp.ui.common.DateF
import com.phdevtech.cicloserp.ui.common.DropdownF
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.FormScaffold
import com.phdevtech.cicloserp.ui.common.FormState
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.SectionLabel
import com.phdevtech.cicloserp.ui.common.TextF
import com.phdevtech.cicloserp.ui.common.TimeF
import com.phdevtech.cicloserp.ui.common.rememberSaveRunner
import com.phdevtech.cicloserp.ui.common.repository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.material3.Scaffold

private fun JsonObject.s(key: String) = str(key).orEmpty()

/** Carrega o registro (ou um vazio, para cadastro novo) e entrega ao formulário. */
@Composable
private fun LoadForm(title: String, onBack: () -> Unit, key: Any?, fetch: suspend () -> JsonObject, body: @Composable (JsonObject) -> Unit) {
    val scope = rememberCoroutineScope()
    val loader = remember(key) { Loader(scope, fetch) }
    LaunchedEffect(key) { loader.load() }
    when (val s = loader.state) {
        Load.Loading -> Scaffold(topBar = { BackBar(title, onBack) }) { Box(Modifier.padding(it)) { LoadingBox() } }
        is Load.Error -> Scaffold(topBar = { BackBar(title, onBack) }) { Box(Modifier.padding(it)) { ErrorBox(s.message, { loader.load() }) } }
        is Load.Ok -> body(s.data)
    }
}

private val EMPTY = JsonObject(emptyMap())

// ---------------------------------------------------------------- Cliente

private val CUSTOMER_FIELDS = listOf("type", "document", "name", "trade_name", "state_registration", "email", "phone", "cep", "street", "number", "complement", "district", "city", "state", "notes")

/** Cadastro e edição de cliente (POST customer_save), com busca de CNPJ na Receita (GET cnpj). */
@Composable
fun CustomerFormScreen(id: Int?, canDelete: Boolean, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val repo = repository()
    LoadForm(if (id == null) "Novo cliente" else "Editar cliente", onBack, id, { if (id == null) EMPTY else repo.raw("customer", id) }) { raw ->
        val form = remember(raw) { FormState(CUSTOMER_FIELDS.associateWith { raw.s(it) }.toMutableMap().apply { if (this["type"].isNullOrBlank()) this["type"] = "pj" }) }
        var active by remember(raw) { mutableStateOf(raw.str("active") != "0") }
        var confirmDelete by remember { mutableStateOf(false) }
        var lookup by remember { mutableStateOf<String?>(null) }
        val run = rememberSaveRunner()
        val scope = rememberCoroutineScope()
        val pj = form["type"] != "pf"

        FormScaffold(
            title = if (id == null) "Novo cliente" else "Editar cliente", onBack = onBack, saving = run.saving, error = run.error,
            onSave = {
                run.run({
                    val extra = if (id != null) mapOf<String, JsonElement>("id" to JsonPrimitive(id), "active" to JsonPrimitive(active)) else emptyMap()
                    repo.save("customer_save", form.toJson(extra = extra))
                }, onSaved)
            },
        ) {
            DropdownF("Tipo", listOf("pj" to "Pessoa jurídica", "pf" to "Pessoa física"), form["type"]) { form["type"] = it }
            TextF(form, "document", if (pj) "CNPJ" else "CPF", keyboard = KeyboardType.Number)
            if (pj) {
                OutlinedButton(onClick = {
                    val digits = form["document"].filter { it.isDigit() }
                    if (digits.length != 14) { lookup = "Informe os 14 números do CNPJ."; return@OutlinedButton }
                    lookup = "Buscando…"
                    scope.launch {
                        try {
                            val r = repo.cnpj(digits)
                            if (r == null) lookup = "CNPJ não encontrado." else {
                                listOf("name", "trade_name", "email", "phone", "cep", "street", "number", "complement", "district", "city", "state").forEach { k ->
                                    r.text(k)?.let { v -> form[k] = v }
                                }
                                lookup = null
                            }
                        } catch (e: Exception) { lookup = e.message ?: "Não foi possível consultar o CNPJ." }
                    }
                }) { Text("Buscar dados na Receita") }
                lookup?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextF(form, "name", if (pj) "Razão social" else "Nome", required = true, caps = KeyboardCapitalization.Words)
            if (pj) {
                TextF(form, "trade_name", "Nome fantasia", caps = KeyboardCapitalization.Words)
                TextF(form, "state_registration", "Inscrição estadual")
            }
            SectionLabel("Contato")
            TextF(form, "phone", "Telefone", keyboard = KeyboardType.Phone)
            TextF(form, "email", "E-mail", keyboard = KeyboardType.Email, caps = KeyboardCapitalization.None)
            SectionLabel("Endereço")
            TextF(form, "cep", "CEP", keyboard = KeyboardType.Number)
            TextF(form, "street", "Rua", caps = KeyboardCapitalization.Words)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextF(form, "number", "Número", Modifier.weight(1f))
                TextF(form, "complement", "Complemento", Modifier.weight(2f))
            }
            TextF(form, "district", "Bairro", caps = KeyboardCapitalization.Words)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextF(form, "city", "Cidade", Modifier.weight(3f), caps = KeyboardCapitalization.Words)
                TextF(form, "state", "UF", Modifier.weight(1f), caps = KeyboardCapitalization.Characters)
            }
            TextF(form, "notes", "Observações", lines = 3)
            if (id != null) {
                LabeledCheck(active, { active = it }, "Cliente ativo")
                if (canDelete) TextButton(onClick = { confirmDelete = true }) { Text("Excluir cliente", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (confirmDelete && id != null) ConfirmDialog(
            "Excluir cliente?", "Se ele já tem vendas, orçamentos ou ordens, será só desativado para manter o histórico.", "Excluir",
            onConfirm = { confirmDelete = false; run.run({ repo.delete("customer_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Produto e serviço

private val PRODUCT_FIELDS = listOf("name", "sku", "barcode", "category", "brand", "mfr_code", "location", "unit", "sale_price", "min_stock", "description", "markup")
private val PRODUCT_NUMERIC = setOf("sale_price", "min_stock", "markup", "avg_cost", "initial_stock")

/** Cadastro e edição de produto ou serviço (POST product_save). */
@Composable
fun ProductFormScreen(id: Int?, service: Boolean, canDelete: Boolean, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val repo = repository()
    val loadingNoun = if (service) "serviço" else "produto"
    LoadForm(if (id == null) "Novo $loadingNoun" else "Editar item", onBack, id, { if (id == null) EMPTY else repo.raw("product", id) }) { raw ->
        val isService = service || raw.str("type") == "servico"
        val noun = if (isService) "serviço" else "produto"
        val form = remember(raw) {
            FormState(PRODUCT_FIELDS.associateWith { raw.s(it) }.toMutableMap().apply {
                if (this["unit"].isNullOrBlank()) this["unit"] = if (isService) "un" else "un"
                put("avg_cost", raw.s("avg_cost").let { v -> if (v.toDoubleOrNull() == 0.0) "" else v })
                put("initial_stock", "")
            })
        }
        val originalCost = remember(raw) { raw.s("avg_cost").let { v -> if (v.toDoubleOrNull() == 0.0) "" else v } }
        var active by remember(raw) { mutableStateOf(raw.str("active") != "0") }
        var confirmDelete by remember { mutableStateOf(false) }
        val run = rememberSaveRunner()

        FormScaffold(
            title = if (id == null) "Novo $noun" else "Editar $noun", onBack = onBack, saving = run.saving, error = run.error,
            onSave = {
                run.run({
                    val json = form.toJson(PRODUCT_NUMERIC).toMutableMap()
                    json["type"] = JsonPrimitive(if (isService) "servico" else "produto")
                    // Custo e estoque inicial só vão quando há o que gravar (o custo na edição vira movimento "ajuste de custo")
                    if (id != null) {
                        json.remove("initial_stock")
                        if (form["avg_cost"] == originalCost) json.remove("avg_cost")
                        json["id"] = JsonPrimitive(id); json["active"] = JsonPrimitive(active)
                    } else if (form["initial_stock"].isBlank()) json.remove("initial_stock")
                    if (json["avg_cost"]?.let { (it as? JsonPrimitive)?.content.isNullOrBlank() } == true) json.remove("avg_cost")
                    if (json["markup"]?.let { (it as? JsonPrimitive)?.content.isNullOrBlank() } == true) json.remove("markup")
                    repo.save("product_save", JsonObject(json))
                }, onSaved)
            },
        ) {
            TextF(form, "name", "Nome", required = true, caps = KeyboardCapitalization.Sentences)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextF(form, "sale_price", "Preço de venda (R$)", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                TextF(form, "avg_cost", if (isService) "Custo (R$)" else "Valor de compra (R$)", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
            }
            TextF(form, "sku", "SKU / código", caps = KeyboardCapitalization.Characters)
            TextF(form, "category", "Categoria")
            if (!isService) {
                TextF(form, "brand", "Marca", caps = KeyboardCapitalization.Words)
                TextF(form, "mfr_code", "Código do fabricante", caps = KeyboardCapitalization.Characters)
                TextF(form, "barcode", "Código de barras", keyboard = KeyboardType.Number)
                TextF(form, "location", "Localização no estoque")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "unit", "Unidade", Modifier.weight(1f), caps = KeyboardCapitalization.None)
                    TextF(form, "min_stock", "Estoque mínimo", Modifier.weight(1f), keyboard = KeyboardType.Decimal)
                }
                if (id == null) TextF(form, "initial_stock", "Estoque inicial", keyboard = KeyboardType.Decimal)
            } else {
                TextF(form, "unit", "Unidade", caps = KeyboardCapitalization.None)
            }
            TextF(form, "description", "Observações", lines = 3)
            if (id != null) {
                LabeledCheck(active, { active = it }, if (isService) "Serviço ativo" else "Produto ativo")
                if (canDelete) TextButton(onClick = { confirmDelete = true }) { Text("Excluir $noun", color = MaterialTheme.colorScheme.error) }
            }
        }
        if (confirmDelete && id != null) ConfirmDialog(
            "Excluir $noun?", "Se já foi usado em vendas, orçamentos ou ordens, será só desativado para manter o histórico.", "Excluir",
            onConfirm = { confirmDelete = false; run.run({ repo.delete("product_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Equipamento

private val EQUIPMENT_FIELDS = listOf("customer_id", "name", "brand", "model", "serial", "category", "warranty_until", "valid_until", "notes")

/** Cadastro e edição de equipamento (POST equipment_save). As especificações já gravadas são reenviadas para não se perderem. */
@Composable
fun EquipmentFormScreen(id: Int?, presetCustomerId: Int?, canDelete: Boolean, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val repo = repository()
    LoadForm(if (id == null) "Novo equipamento" else "Editar equipamento", onBack, id, { if (id == null) EMPTY else repo.equipmentRaw(id) }) { raw ->
        val form = remember(raw) {
            FormState(EQUIPMENT_FIELDS.associateWith { raw.s(it) }.toMutableMap().apply {
                if (id == null && presetCustomerId != null) put("customer_id", presetCustomerId.toString())
            })
        }
        var customerName by remember(raw) { mutableStateOf(raw.str("customer_trade_name")?.takeIf { it.isNotBlank() } ?: raw.s("customer_name")) }
        var confirmDelete by remember { mutableStateOf(false) }
        val run = rememberSaveRunner()
        // Cliente vindo da ficha do cliente: mostra o nome
        LaunchedEffect(presetCustomerId) {
            if (id == null && presetCustomerId != null) runCatching { repo.raw("customer", presetCustomerId) }.getOrNull()?.let { customerName = it.s("name") }
        }

        FormScaffold(
            title = if (id == null) "Novo equipamento" else "Editar equipamento", onBack = onBack, saving = run.saving, error = run.error,
            onSave = {
                run.run({
                    val extra = mutableMapOf<String, JsonElement>()
                    if (id != null) extra["id"] = JsonPrimitive(id)
                    raw["specs"]?.let { extra["specs"] = it }
                    repo.save("equipment_save", form.toJson(extra = extra))
                }, onSaved)
            },
        ) {
            CustomerPickerF("Cliente", customerName, { form["customer_id"] = it.id.toString(); customerName = it.name })
            TextF(form, "name", "Equipamento", required = true, caps = KeyboardCapitalization.Words)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextF(form, "brand", "Marca", Modifier.weight(1f), caps = KeyboardCapitalization.Words)
                TextF(form, "model", "Modelo", Modifier.weight(1f))
            }
            TextF(form, "serial", "Nº de série", caps = KeyboardCapitalization.Characters)
            TextF(form, "category", "Categoria")
            DateF("Garantia até", form["warranty_until"], { form["warranty_until"] = it })
            DateF("Validade", form["valid_until"], { form["valid_until"] = it })
            TextF(form, "notes", "Observações", lines = 3)
            if (id != null && canDelete) TextButton(onClick = { confirmDelete = true }) { Text("Excluir equipamento", color = MaterialTheme.colorScheme.error) }
        }
        if (confirmDelete && id != null) ConfirmDialog(
            "Excluir equipamento?", "Se já foi usado numa ordem de serviço, será só desativado para manter o histórico.", "Excluir",
            onConfirm = { confirmDelete = false; run.run({ repo.delete("equipment_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Nova tarefa (ordem de serviço)

private fun JsonObject.options(key: String, label: (JsonObject) -> String = { it.s("name") }): List<Pair<String, String>> =
    (this[key] as? JsonArray).orEmpty().mapNotNull { it.asObj() }.map { it.s("id") to label(it) }

private fun JsonArray?.orEmpty() = this ?: JsonArray(emptyList())

/** Nova tarefa = nova ordem de serviço (POST service_order_save): cliente, equipamento, tipo, técnico, agenda e descrição. */
@Composable
fun NewOrderScreen(meId: Int, presetCustomerId: Int?, onBack: () -> Unit, onSaved: (Int?) -> Unit) {
    val repo = repository()
    val form = remember { FormState(mapOf("customer_id" to (presetCustomerId?.toString() ?: ""), "technician_id" to meId.toString(), "date" to "", "time" to "09:00")) }
    var customerName by remember { mutableStateOf("") }
    var lists by remember { mutableStateOf(EMPTY) }
    var listsError by remember { mutableStateOf<String?>(null) }
    val run = rememberSaveRunner()
    val customerId = form["customer_id"].toIntOrNull()

    LaunchedEffect(presetCustomerId) {
        if (presetCustomerId != null) runCatching { repo.raw("customer", presetCustomerId) }.getOrNull()?.let { customerName = it.s("name") }
    }
    // Equipamentos, técnicos e tipos de tarefa (equipamentos dependem do cliente escolhido)
    LaunchedEffect(customerId) {
        try { lists = repo.orderForm(customerId ?: 0); listsError = null } catch (e: Exception) { listsError = e.message }
    }

    FormScaffold(
        title = "Nova tarefa", onBack = onBack, saving = run.saving, error = run.error ?: listsError, saveLabel = "Criar tarefa",
        onSave = {
            var newId: Int? = null
            run.run({
                val extra = mutableMapOf<String, JsonElement>("origin" to JsonPrimitive("chamado"))
                if (form["date"].isNotBlank()) extra["scheduled_at"] = JsonPrimitive("${form["date"]} ${form["time"].ifBlank { "09:00" }}:00")
                val body = form.toJson(extra = extra).toMutableMap().apply { remove("date"); remove("time") }
                newId = repo.save("service_order_save", JsonObject(body))
            }, { onSaved(newId) })
        },
    ) {
        CustomerPickerF("Cliente", customerName, { form["customer_id"] = it.id.toString(); form["equipment_id"] = ""; customerName = it.name })
        DropdownF("Equipamento", listOf("" to "Nenhum") + lists.options("equipments") { e -> e.s("name") + (e.text("serial")?.let { " · $it" } ?: "") }, form["equipment_id"]) { form["equipment_id"] = it }
        DropdownF("Tipo de tarefa", listOf("" to "Não informado") + lists.options("task_types"), form["task_type_id"]) { form["task_type_id"] = it }
        DropdownF("Técnico", listOf("" to "Sem técnico") + lists.options("technicians"), form["technician_id"]) { form["technician_id"] = it }
        TextF(form, "description", "Serviço solicitado", lines = 3, required = true)
        SectionLabel("Agendamento (opcional)")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateF("Data", form["date"], { form["date"] = it }, Modifier.weight(3f))
            TimeF("Hora", form["time"], { form["time"] = it }, Modifier.weight(2f))
        }
        DateF("Prazo", form["due_date"], { form["due_date"] = it })
        TextF(form, "notes", "Orientação ao técnico", lines = 3)
    }
}

