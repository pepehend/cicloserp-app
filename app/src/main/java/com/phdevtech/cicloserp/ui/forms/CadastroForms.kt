package com.phdevtech.cicloserp.ui.forms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.data.text
import com.phdevtech.cicloserp.ui.common.BackBar
import com.phdevtech.cicloserp.ui.common.ConfirmDialog
import com.phdevtech.cicloserp.ui.common.CustomerPickerF
import com.phdevtech.cicloserp.ui.common.DateF
import com.phdevtech.cicloserp.ui.common.DropdownF
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.FieldHelp
import com.phdevtech.cicloserp.ui.common.Fieldset
import com.phdevtech.cicloserp.ui.common.FormScaffold
import com.phdevtech.cicloserp.ui.common.FormState
import com.phdevtech.cicloserp.ui.common.LabeledCheck
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.Seg
import com.phdevtech.cicloserp.ui.common.TextF
import com.phdevtech.cicloserp.ui.common.TimeF
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebField
import com.phdevtech.cicloserp.ui.common.rememberSaveRunner
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.Web
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private fun JsonObject.s(key: String) = str(key).orEmpty()

private val EMPTY = JsonObject(emptyMap())
private val UNITS = listOf("un", "kg", "g", "L", "ml", "cx", "pct", "dz", "m", "fardo").map { it to it }

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

@Composable
private fun DeleteLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(text, color = Web.danger) }
}

// ---------------------------------------------------------------- Cliente

private val CUSTOMER_FIELDS = listOf("type", "document", "name", "trade_name", "state_registration", "email", "phone", "cep", "street", "number", "complement", "district", "city", "state", "notes")

/** Janela "Novo cliente / Editar cliente" (POST customer_save), com "Buscar dados" no CNPJ e preenchimento pelo CEP. */
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
            title = if (id == null) "Novo cliente" else "Editar cliente", onBack = onBack, saving = run.saving, error = run.error, saveLabel = "Salvar cliente",
            onSave = {
                run.run({
                    val extra = if (id != null) mapOf<String, JsonElement>("id" to JsonPrimitive(id), "active" to JsonPrimitive(active)) else emptyMap()
                    repo.save("customer_save", form.toJson(extra = extra))
                }, onSaved)
            },
        ) {
            Seg(listOf("Empresa (CNPJ)", "Pessoa física (CPF)"), if (pj) 0 else 1, { form["type"] = if (it == 0) "pj" else "pf" })
            TextF(form, "document", if (pj) "CNPJ" else "CPF", keyboard = KeyboardType.Number, placeholder = if (pj) "00.000.000/0000-00" else "000.000.000-00")
            if (pj) {
                Column {
                    WebButton("Buscar dados", {
                        val digits = form["document"].filter { it.isDigit() }
                        if (digits.length != 14) { lookup = "Informe os 14 números do CNPJ."; return@WebButton }
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
                    }, icon = Icons.Outlined.Search)
                    FieldHelp(lookup ?: "Digite o CNPJ: os dados são preenchidos automaticamente.")
                }
            }
            if (pj) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "name", "Razão social", Modifier.weight(1f), required = true, caps = KeyboardCapitalization.Words)
                    TextF(form, "trade_name", "Nome fantasia", Modifier.weight(1f), caps = KeyboardCapitalization.Words)
                }
            } else TextF(form, "name", "Nome", required = true, caps = KeyboardCapitalization.Words)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextF(form, "phone", "Telefone / WhatsApp", Modifier.weight(1f), keyboard = KeyboardType.Phone, placeholder = "(31) 99999-9999")
                TextF(form, "email", "E-mail", Modifier.weight(1f), keyboard = KeyboardType.Email, caps = KeyboardCapitalization.None)
            }
            if (pj) Row { TextF(form, "state_registration", "Inscrição estadual", Modifier.fillMaxWidth(0.5f).padding(end = 6.dp)) }
            Fieldset("Endereço") {
                Column(Modifier.fillMaxWidth(0.5f).padding(end = 6.dp)) {
                    TextF(form, "cep", "CEP", keyboard = KeyboardType.Number, help = "Digite o CEP para preencher o endereço")
                }
                // Com 8 números, completa rua, bairro, cidade e UF pelo CEP
                val cepDigits = form["cep"].filter { it.isDigit() }
                LaunchedEffect(cepDigits) {
                    if (cepDigits.length == 8) runCatching { repo.cep(cepDigits) }.getOrNull()?.let { r ->
                        listOf("street", "district", "city", "state").forEach { k -> r.text(k)?.let { v -> if (form[k].isBlank()) form[k] = v } }
                    }
                }
                TextF(form, "street", "Rua", caps = KeyboardCapitalization.Words)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "number", "Número", Modifier.weight(1f))
                    TextF(form, "complement", "Complemento", Modifier.weight(1f))
                }
                TextF(form, "district", "Bairro", caps = KeyboardCapitalization.Words)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "city", "Cidade", Modifier.weight(3f), caps = KeyboardCapitalization.Words)
                    TextF(form, "state", "UF", Modifier.weight(1f), caps = KeyboardCapitalization.Characters)
                }
            }
            TextF(form, "notes", "Observações", lines = 3)
            if (id != null) {
                LabeledCheck(active, { active = it }, "Cliente ativo")
                if (canDelete) DeleteLink("Excluir cliente") { confirmDelete = true }
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

/** Janela "Novo produto / Novo serviço" (POST product_save), com as abas Geral e Observações. */
@Composable
fun ProductFormScreen(id: Int?, service: Boolean, canDelete: Boolean, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val repo = repository()
    val loadingNoun = if (service) "serviço" else "produto"
    LoadForm(if (id == null) "Novo $loadingNoun" else "Editar item", onBack, id, { if (id == null) EMPTY else repo.raw("product", id) }) { raw ->
        val isService = service || raw.str("type") == "servico"
        val noun = if (isService) "serviço" else "produto"
        val form = remember(raw) {
            FormState(PRODUCT_FIELDS.associateWith { raw.s(it) }.toMutableMap().apply {
                if (this["unit"].isNullOrBlank()) this["unit"] = "un"
                put("avg_cost", raw.s("avg_cost").let { v -> if (v.toDoubleOrNull() == 0.0) "" else v })
                put("initial_stock", "")
            })
        }
        val originalCost = remember(raw) { raw.s("avg_cost").let { v -> if (v.toDoubleOrNull() == 0.0) "" else v } }
        var active by remember(raw) { mutableStateOf(raw.str("active") != "0") }
        var tab by remember { mutableStateOf(0) }
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
            Seg(listOf("Geral", "Observações"), tab, { tab = it })
            if (tab == 0) {
                TextF(form, "name", "Nome", required = true)
                if (!isService) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextF(form, "brand", "Marca", Modifier.weight(1f), placeholder = "Ex.: SKF, Atlas Copco", caps = KeyboardCapitalization.Words)
                        TextF(form, "mfr_code", "Código do fabricante", Modifier.weight(1f), placeholder = "Código na marca acima", caps = KeyboardCapitalization.Characters)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "category", "Categoria", Modifier.weight(1f))
                    DropdownF("Unidade de estoque", UNITS, form["unit"], Modifier.weight(1f), help = "Como você conta: un, kg, g, L…") { form["unit"] = it }
                }
                if (!isService) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextF(form, "min_stock", "Estoque mínimo", Modifier.weight(1f), keyboard = KeyboardType.Decimal, placeholder = "0", help = "Avisa quando chegar nesse valor")
                        if (id == null) TextF(form, "initial_stock", "Estoque inicial", Modifier.weight(1f), keyboard = KeyboardType.Decimal, placeholder = "0", help = "Quantidade que você já tem hoje")
                        else Box(Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(form, "sku", "Código interno (SKU)", Modifier.weight(1f), caps = KeyboardCapitalization.Characters)
                    if (!isService) TextF(form, "barcode", "Código de barras", Modifier.weight(1f), keyboard = KeyboardType.Number) else Box(Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextF(
                        form, "avg_cost",
                        if (isService) "Custo (R$)" else if (id == null) "Custo inicial por unidade (R$)" else "Valor de compra (R$)", Modifier.weight(1f),
                        keyboard = KeyboardType.Decimal, placeholder = if (id == null && !isService) "opcional" else "0,00",
                        help = if (id == null && !isService) "Se ainda não lançou uma compra, informe o custo" else null,
                    )
                    TextF(form, "sale_price", "Preço de venda (R$)", Modifier.weight(1f), keyboard = KeyboardType.Decimal, placeholder = "0,00")
                }
                if (!isService) TextF(form, "location", "Localização no estoque")
                if (id != null) LabeledCheck(active, { active = it }, if (isService) "Serviço ativo" else "Produto ativo")
            } else {
                TextF(form, "description", "Observações", lines = 6, placeholder = "Anotações sobre o $noun")
            }
            if (id != null && canDelete) DeleteLink("Excluir $noun") { confirmDelete = true }
        }
        if (confirmDelete && id != null) ConfirmDialog(
            "Excluir $noun?", "Se já foi usado em vendas, orçamentos ou ordens, será só desativado para manter o histórico.", "Excluir",
            onConfirm = { confirmDelete = false; run.run({ repo.delete("product_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Equipamento

private val EQUIPMENT_FIELDS = listOf("customer_id", "name", "brand", "model", "serial", "category", "warranty_until", "valid_until", "notes")

/** Janela "Novo equipamento" (POST equipment_save) com as abas Geral e Especificações (rótulo e valor). */
@Composable
fun EquipmentFormScreen(id: Int?, presetCustomerId: Int?, canDelete: Boolean, onBack: () -> Unit, onSaved: () -> Unit, onDeleted: () -> Unit) {
    val repo = repository()
    LoadForm(if (id == null) "Novo equipamento" else "Editar equipamento", onBack, id, { if (id == null) EMPTY else repo.equipmentRaw(id) }) { raw ->
        val form = remember(raw) {
            FormState(EQUIPMENT_FIELDS.associateWith { raw.s(it) }.toMutableMap().apply {
                if (id == null && presetCustomerId != null) put("customer_id", presetCustomerId.toString())
            })
        }
        // Especificações já gravadas: lista de {label, value}
        val specs = remember(raw) {
            mutableStateListOf<Pair<String, String>>().apply {
                (raw["specs"] as? JsonArray)?.mapNotNull { it.asObj() }?.forEach { add(it.s("label") to it.s("value")) }
            }
        }
        var customerName by remember(raw) { mutableStateOf(raw.str("customer_trade_name")?.takeIf { it.isNotBlank() } ?: raw.s("customer_name")) }
        var tab by remember { mutableStateOf(0) }
        var confirmDelete by remember { mutableStateOf(false) }
        val run = rememberSaveRunner()
        LaunchedEffect(presetCustomerId) {
            if (id == null && presetCustomerId != null) runCatching { repo.raw("customer", presetCustomerId) }.getOrNull()?.let { customerName = it.s("name") }
        }

        FormScaffold(
            title = if (id == null) "Novo equipamento" else "Editar equipamento", onBack = onBack, saving = run.saving, error = run.error, saveLabel = "Salvar equipamento",
            onSave = {
                run.run({
                    val extra = mutableMapOf<String, JsonElement>()
                    if (id != null) extra["id"] = JsonPrimitive(id)
                    extra["specs"] = JsonArray(specs.filter { it.first.isNotBlank() || it.second.isNotBlank() }.map { (l, v) ->
                        buildJsonObject { put("label", l.trim()); put("value", v.trim()) }
                    })
                    repo.save("equipment_save", form.toJson(extra = extra))
                }, onSaved)
            },
        ) {
            Seg(listOf("Geral", "Especificações"), tab, { tab = it })
            if (tab == 0) {
                CustomerPickerF("Cliente", customerName, { form["customer_id"] = it.id.toString(); customerName = it.name })
                TextF(form, "name", "Nome do equipamento", required = true, placeholder = "Ex.: Compressor de parafuso", caps = KeyboardCapitalization.Words)
                TextF(form, "serial", "Identificador", placeholder = "Nº de série ou código", caps = KeyboardCapitalization.Characters)
                TextF(form, "category", "Categoria", placeholder = "Ex.: Compressores")
                TextF(form, "brand", "Marca", caps = KeyboardCapitalization.Words)
                TextF(form, "model", "Modelo")
                DateF("Data de validade", form["valid_until"], { form["valid_until"] = it })
                DateF("Garantia até", form["warranty_until"], { form["warranty_until"] = it })
                TextF(form, "notes", "Descrição", lines = 3)
            } else {
                if (specs.isEmpty()) Text("Nenhuma especificação. Adicione itens como potência, pressão ou tensão.", color = Web.muted, fontSize = 13.8.sp)
                specs.forEachIndexed { i, (l, v) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        WebField(l, { specs[i] = it to specs[i].second }, Modifier.weight(1f), label = "Rótulo", placeholder = "Ex.: Potência")
                        WebField(v, { specs[i] = specs[i].first to it }, Modifier.weight(1f), label = "Valor", placeholder = "Ex.: 37 kW")
                        IconButton(onClick = { specs.removeAt(i) }) { Icon(Icons.Outlined.Close, "Remover", tint = Web.muted) }
                    }
                }
                WebButton("Adicionar", { specs.add("" to "") }, icon = Icons.Outlined.Add)
            }
            if (id != null && canDelete) DeleteLink("Excluir equipamento") { confirmDelete = true }
        }
        if (confirmDelete && id != null) ConfirmDialog(
            "Excluir equipamento?", "Se já foi usado numa ordem de serviço, será só desativado para manter o histórico.", "Excluir",
            onConfirm = { confirmDelete = false; run.run({ repo.delete("equipment_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
        )
    }
}

// ---------------------------------------------------------------- Nova ordem de serviço

private fun JsonObject.options(key: String, label: (JsonObject) -> String = { it.s("name") }): List<Pair<String, String>> =
    ((this[key] as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { it.asObj() }.map { it.s("id") to label(it) }

/**
 * Página "Nova ordem de serviço" (POST service_order_save): voltar "← Ordens de serviço", título e um cartão com
 * cliente, equipamento, tipo, origem, descrição, técnico, agenda, prazo e observações.
 */
@Composable
fun NewOrderScreen(meId: Int, presetCustomerId: Int?, onBack: () -> Unit, onSaved: (Int?) -> Unit) {
    val repo = repository()
    val form = remember { FormState(mapOf("customer_id" to (presetCustomerId?.toString() ?: ""), "technician_id" to meId.toString(), "date" to "", "time" to "09:00", "origin" to "chamado")) }
    var customerName by remember { mutableStateOf("") }
    var lists by remember { mutableStateOf(EMPTY) }
    var listsError by remember { mutableStateOf<String?>(null) }
    val run = rememberSaveRunner()
    val customerId = form["customer_id"].toIntOrNull()

    LaunchedEffect(presetCustomerId) {
        if (presetCustomerId != null) runCatching { repo.raw("customer", presetCustomerId) }.getOrNull()?.let { customerName = it.s("name") }
    }
    LaunchedEffect(customerId) {
        try { lists = repo.orderForm(customerId ?: 0); listsError = null } catch (e: Exception) { listsError = e.message }
    }

    Scaffold(containerColor = Web.bg) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar", tint = Web.text) }
            Column {
                Text("← Ordens de serviço", color = Web.accent, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onBack))
                Text("Nova ordem de serviço", color = Web.text, fontSize = 21.75.sp, fontWeight = FontWeight(650))
            }
            WebCard(padding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CustomerPickerF("Cliente", customerName, { form["customer_id"] = it.id.toString(); form["equipment_id"] = ""; form["quote_id"] = ""; customerName = it.name })
                    DropdownF("Equipamento", listOf("" to "Nenhum equipamento") + lists.options("equipments") { e -> e.s("name") + (e.text("serial")?.let { " · $it" } ?: "") }, form["equipment_id"]) { form["equipment_id"] = it }
                    DropdownF("Tipo de tarefa", lists.options("task_types"), form["task_type_id"]) { form["task_type_id"] = it }
                    Fieldset("Origem") {
                        listOf("chamado" to "Chamado avulso", "orcamento" to "Orçamento do cliente").forEach { (v, t) ->
                            Row(Modifier.fillMaxWidth().clickable { form["origin"] = v }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(form["origin"] == v, { form["origin"] = v }, colors = RadioButtonDefaults.colors(selectedColor = Web.accent, unselectedColor = Web.muted))
                                Text(t, Modifier.padding(start = 4.dp), fontSize = 15.sp)
                            }
                        }
                        if (form["origin"] == "orcamento") {
                            DropdownF("Orçamento", lists.options("quotes") { q -> "#${q.s("id")} · ${q.text("lead_name") ?: q.s("status")}" }, form["quote_id"]) { form["quote_id"] = it }
                        }
                    }
                    TextF(form, "description", "Descrição do serviço", lines = 3, required = true, placeholder = "O que o cliente pediu ou relatou")
                    DropdownF("Técnico", listOf("" to "Sem técnico") + lists.options("technicians"), form["technician_id"]) { form["technician_id"] = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateF("Agendar para", form["date"], { form["date"] = it }, Modifier.weight(3f))
                        TimeF("Hora", form["time"], { form["time"] = it }, Modifier.weight(2f))
                    }
                    DateF("Prazo", form["due_date"], { form["due_date"] = it })
                    TextF(form, "notes", "Observações internas", lines = 2)
                    (run.error ?: listsError)?.let { Text(it, color = Web.danger, fontSize = 13.8.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WebButton("Cancelar", onBack, enabled = !run.saving)
                        WebButton(if (run.saving) "Salvando…" else "Salvar ordem", {
                            var newId: Int? = null
                            run.run({
                                val extra = mutableMapOf<String, JsonElement>()
                                if (form["date"].isNotBlank()) extra["scheduled_at"] = JsonPrimitive("${form["date"]} ${form["time"].ifBlank { "09:00" }}:00")
                                val body = form.toJson(extra = extra).toMutableMap().apply { remove("date"); remove("time") }
                                newId = repo.save("service_order_save", JsonObject(body))
                            }, { onSaved(newId) })
                        }, primary = true, enabled = !run.saving)
                    }
                }
            }
        }
    }
}
