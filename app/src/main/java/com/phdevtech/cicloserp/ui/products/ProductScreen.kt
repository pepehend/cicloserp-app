package com.phdevtech.cicloserp.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdevtech.cicloserp.data.arr
import com.phdevtech.cicloserp.data.asObj
import com.phdevtech.cicloserp.data.dbl
import com.phdevtech.cicloserp.data.obj
import com.phdevtech.cicloserp.data.str
import com.phdevtech.cicloserp.data.text
import com.phdevtech.cicloserp.ui.common.BackBar
import com.phdevtech.cicloserp.ui.common.Col
import com.phdevtech.cicloserp.ui.common.ConfirmDialog
import com.phdevtech.cicloserp.ui.common.ErrorBox
import com.phdevtech.cicloserp.ui.common.FormState
import com.phdevtech.cicloserp.ui.common.Load
import com.phdevtech.cicloserp.ui.common.Loader
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.TRow
import com.phdevtech.cicloserp.ui.common.TextF
import com.phdevtech.cicloserp.ui.common.DropdownF
import com.phdevtech.cicloserp.ui.common.WebButton
import com.phdevtech.cicloserp.ui.common.WebCard
import com.phdevtech.cicloserp.ui.common.WebTable
import com.phdevtech.cicloserp.ui.common.rememberSaveRunner
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.theme.Web
import com.phdevtech.cicloserp.util.Fmt
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import androidx.compose.ui.text.input.KeyboardType

private val MOVEMENT_LABELS = mapOf(
    "entrada" to "Entrada", "nota" to "Entrada por nota", "saida" to "Saída / uso", "perda" to "Perda", "ajuste" to "Ajuste de inventário",
    "venda" to "Venda", "estorno_venda" to "Estorno de venda", "estorno_nota" to "Estorno de nota", "ajuste_custo" to "Ajuste de custo",
)

private fun qty(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else "%.3f".format(v).trimEnd('0').trimEnd(',', '.')
private fun JsonObject.n(key: String) = dbl(key) ?: 0.0

/**
 * Ficha do produto/serviço (GET product): "← Produtos", nome, linha de resumo, Movimentar/Editar/Desativar/Excluir,
 * indicadores (estoque, custo, preço, margem, valor em estoque), referências cruzadas, tributação e histórico.
 */
@Composable
fun ProductScreen(
    id: Int, onBack: () -> Unit, canEdit: Boolean, canDelete: Boolean, canMove: Boolean, isAdmin: Boolean,
    onEdit: () -> Unit, onDeleted: () -> Unit,
) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    val loader = remember(id) { Loader(scope) { repo.raw("product", id) } }
    androidx.lifecycle.compose.LifecycleResumeEffect(id) { loader.load(); onPauseOrDispose { } }
    var confirmDelete by remember { mutableStateOf(false) }
    var movement by remember { mutableStateOf(false) }
    val run = rememberSaveRunner()

    Scaffold(containerColor = Web.bg, topBar = { BackBar("", onBack) }) { pad ->
        Box(Modifier.padding(pad)) {
            PullToRefreshBox(loader.refreshing, { loader.load(pull = true) }, Modifier.fillMaxSize()) {
                when (val s = loader.state) {
                    Load.Loading -> LoadingBox()
                    is Load.Error -> ErrorBox(s.message, { loader.load() })
                    is Load.Ok -> {
                        val p = s.data
                        val service = p.str("type") == "servico"
                        val active = p.str("active") != "0"
                        val stock = p.n("stock"); val cost = p.n("unit_cost").let { if (it == 0.0) p.n("avg_cost") else it }; val price = p.n("sale_price")
                        val markup = p.dbl("markup")
                        val margin = if (price > 0) (price - cost) / price * 100 else null
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                Text(if (service) "← Serviços" else "← Produtos", color = Web.accent, fontSize = 14.sp)
                                Text(p.str("name").orEmpty(), color = Web.text, fontSize = 21.75.sp, fontWeight = FontWeight(650))
                                val sub = listOfNotNull(
                                    if (service) "Serviço" else "Produto", p.text("brand"), p.text("mfr_code"), p.text("category"),
                                    p.text("sku")?.let { "SKU $it" }, p.text("location")?.let { "Localização: $it" },
                                ).joinToString(" · ")
                                Text(sub, color = Web.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canMove && !service) WebButton("Movimentar", { movement = true }, icon = Icons.Outlined.SwapHoriz)
                                if (canEdit) WebButton("Editar", onEdit, primary = true)
                                if (isAdmin && canEdit) WebButton(if (active) "Desativar" else "Reativar", {
                                    run.run({
                                        // product_save regrava todos os campos: manda de volta o que já existe, trocando só "active"
                                        val keep = listOf("name", "sku", "barcode", "category", "brand", "mfr_code", "location", "unit", "sale_price", "min_stock", "description", "markup")
                                        val body = HashMap<String, kotlinx.serialization.json.JsonElement>()
                                        keep.forEach { k -> p.str(k)?.let { v -> body[k] = JsonPrimitive(v) } }
                                        body["id"] = JsonPrimitive(id); body["type"] = JsonPrimitive(if (service) "servico" else "produto"); body["active"] = JsonPrimitive(!active)
                                        repo.save("product_save", JsonObject(body))
                                    }, { loader.load() })
                                }, danger = true)
                            }
                            if (canDelete) WebButton("Excluir", { confirmDelete = true }, danger = true, icon = Icons.Outlined.DeleteOutline)
                            run.error?.let { Text(it, color = Web.danger, fontSize = 13.8.sp) }

                            // Indicadores (2 por linha)
                            val kpis = buildList {
                                if (!service) add(Kpi4("Estoque atual", "${qty(stock)} ${p.text("unit") ?: "un"}", "mínimo ${qty(p.n("min_stock"))}", Web.text))
                                add(Kpi4(if (service) "Custo" else "Custo por ${p.text("unit") ?: "un"}", Fmt.money(cost), if (service) null else "custo médio", Web.text))
                                add(Kpi4("Preço de venda", Fmt.money(price), if (markup != null) "automático: custo + ${qty(markup)}%" else "manual", Web.text))
                                if (margin != null) add(Kpi4("Margem", "${"%.0f".format(margin)}%", "lucro ${Fmt.money(price - cost)} por ${p.text("unit") ?: "un"}", if (margin < 0) Web.danger else Web.ok))
                                if (!service) add(Kpi4("Valor em estoque", Fmt.money(stock * cost), null, Web.text))
                            }
                            kpis.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { k ->
                                        Column(Modifier.weight(1f).background(Web.surface, RoundedCornerShape(10.dp)).border(1.dp, Web.line, RoundedCornerShape(10.dp)).padding(16.dp)) {
                                            Text(k.label, color = Web.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text(k.value, color = k.color, fontSize = 22.sp, fontWeight = FontWeight(650), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                                            k.sub?.let { Text(it, color = Web.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)) }
                                        }
                                    }
                                    if (row.size == 1) Box(Modifier.weight(1f))
                                }
                            }

                            if (!service) {
                                val refs = p.arr("refs").mapNotNull { it.asObj() }
                                WebCard {
                                    Text("Referências cruzadas", fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                    Spacer(Modifier.height(6.dp))
                                    if (refs.isEmpty()) Text("Nenhuma. Cadastre códigos equivalentes de outros fabricantes em Editar → Referências cruzadas.", color = Web.muted, fontSize = 14.4.sp)
                                    refs.forEach { r -> Text(listOfNotNull(r.text("manufacturer"), r.str("code")).joinToString(" · "), color = Web.text, modifier = Modifier.padding(vertical = 2.dp)) }
                                }
                                val fiscal = p.obj("fiscal")
                                WebCard {
                                    Text("Tributação", fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                    Spacer(Modifier.height(6.dp))
                                    val fl = listOfNotNull(
                                        fiscal?.text("ncm")?.let { "NCM $it" }, fiscal?.text("cest")?.let { "CEST $it" }, fiscal?.text("cfop")?.let { "CFOP $it" },
                                        fiscal?.text("origem")?.let { "Origem $it" }, fiscal?.text("unidade")?.let { "Unidade tributável $it" },
                                    )
                                    if (fl.isEmpty()) Text("Sem dados fiscais. Preencha em Editar → Tributação (NCM, CEST e Origem também vêm das notas de compra importadas).", color = Web.muted, fontSize = 14.4.sp)
                                    else Text(fl.joinToString(" · "), color = Web.text)
                                }
                                val mv = p.arr("movements").mapNotNull { it.asObj() }
                                WebCard {
                                    Text("Histórico", fontSize = 16.5.sp, fontWeight = FontWeight(620), color = Web.text)
                                    Spacer(Modifier.height(8.dp))
                                    WebTable(
                                        listOf(Col("Data", 130.dp), Col("Tipo", 140.dp), Col("Qtd.", 80.dp, num = true), Col("Custo unit.", 110.dp, num = true)),
                                        mv.map { m ->
                                            val q = m.n("quantity")
                                            TRow(m.str("id") ?: "", listOf(
                                                Fmt.dateTime(m.str("created_at")), MOVEMENT_LABELS[m.str("type")] ?: m.str("type").orEmpty(),
                                                (if (q > 0) "+" else "") + qty(q), m.dbl("unit_cost")?.let { Fmt.money(it) }.orEmpty(),
                                            ), warnCols = if (q < 0) setOf(2) else emptySet())
                                        },
                                        empty = "Nenhuma movimentação ainda.",
                                    )
                                }
                            }
                        }
                        if (confirmDelete) ConfirmDialog(
                            if (service) "Excluir serviço?" else "Excluir produto?", "Se já foi usado, será só desativado para manter o histórico. Produto com estoque precisa ser zerado antes.", "Excluir",
                            onConfirm = { confirmDelete = false; run.run({ repo.delete("product_delete", id) }, onDeleted) }, onDismiss = { confirmDelete = false },
                        )
                        if (movement) MovementDialog(onDismiss = { movement = false }) { kind, q, c, note ->
                            run.run({ repo.movement(id, kind, q, c, note) }, { movement = false; loader.load() })
                        }
                    }
                }
            }
        }
    }
}

private class Kpi4(val label: String, val value: String, val sub: String?, val color: Color)

/** Lançamento manual de estoque: entrada (com custo), saída/uso ou perda. */
@Composable
private fun MovementDialog(onDismiss: () -> Unit, onSave: (kind: String, quantity: String, unitCost: String?, note: String?) -> Unit) {
    val form = remember { FormState(mapOf("kind" to "entrada", "quantity" to "", "unit_cost" to "", "note" to "")) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Web.surface,
        title = { Text("Movimentar estoque", fontWeight = FontWeight(600)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownF("Tipo", listOf("entrada" to "Entrada", "saida" to "Saída / uso", "perda" to "Perda"), form["kind"]) { form["kind"] = it }
                TextF(form, "quantity", "Quantidade", keyboard = KeyboardType.Decimal, required = true)
                if (form["kind"] == "entrada") TextF(form, "unit_cost", "Custo unitário (R$)", keyboard = KeyboardType.Decimal)
                TextF(form, "note", "Observação")
            }
        },
        confirmButton = { TextButton({ onSave(form["kind"], form["quantity"], form["unit_cost"], form["note"]) }) { Text("Lançar", color = Web.accent) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = Web.accent) } },
    )
}

@Suppress("unused")
private val keepArr = JsonArray(emptyList())
