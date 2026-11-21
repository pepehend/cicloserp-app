package com.phdevtech.cicloserp.data.model

import com.phdevtech.cicloserp.data.asArr
import com.phdevtech.cicloserp.data.asStr
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate

/**
 * Perguntas condicionais e progresso do questionário, iguais ao servidor
 * (Questionnaires::condMatch / condValue e OrderExec::visibleMap / filled / missing).
 * O servidor confere tudo de novo ao salvar; aqui é só para a tela mostrar as perguntas certas.
 */
object QuestionLogic {

    val TYPES = linkedMapOf(
        "texto" to "Texto", "numerica" to "Numérica", "check" to "Check", "monetaria" to "Monetária",
        "data" to "Data", "itens" to "Itens", "documento" to "CPF/CNPJ", "hora" to "Hora",
        "multipla" to "Itens com múltipla escolha", "area" to "Área de texto", "assinatura" to "Assinatura", "foto" to "Foto",
    )

    private val COND_BY_TYPE = mapOf(
        "numerica" to listOf("menor", "maior", "menor_igual", "maior_igual", "igual", "diferente"),
        "monetaria" to listOf("menor", "maior", "menor_igual", "maior_igual", "igual", "diferente"),
        "data" to listOf("menor", "maior", "menor_igual", "maior_igual", "igual", "diferente"),
        "hora" to listOf("menor", "maior", "menor_igual", "maior_igual", "igual", "diferente"),
        "check" to listOf("igual"),
        "itens" to listOf("igual", "diferente"),
        "multipla" to listOf("contem_todos", "contem_um", "nao_contem_um"),
    )

    /** Quais perguntas aparecem com estas respostas (a mãe da condicional também precisa estar aparecendo). */
    fun visibleMap(questions: List<Question>, answers: Map<String, JsonElement>): Map<String, Boolean> {
        val vis = mutableMapOf<String, Boolean>()
        val byId = mutableMapOf<String, Question>()
        for (q in questions) {
            val c = q.cond
            val ok = if (c == null) true else {
                val parent = byId[c.question]
                parent != null && vis[c.question] == true && condMatch(parent, c, answers[c.question])
            }
            vis[q.id] = ok
            byId[q.id] = q
        }
        return vis
    }

    fun filled(q: Question, v: JsonElement?): Boolean = when {
        v == null || v is JsonNull -> false
        v is JsonArray -> v.isNotEmpty()
        v is JsonObject -> v.isNotEmpty()
        q.type == "check" -> v.asStr() == "Sim"
        else -> !v.asStr().isNullOrBlank()
    }

    /** [preenchidas, total] entre as perguntas que aparecem. */
    fun progress(questions: List<Question>, answers: Map<String, JsonElement>): Pair<Int, Int> {
        val vis = visibleMap(questions, answers)
        var f = 0; var t = 0
        for (q in questions) {
            if (vis[q.id] != true) continue
            t++
            if (filled(q, answers[q.id])) f++
        }
        return f to t
    }

    /** Obrigatórias (que aparecem) sem resposta. */
    fun missing(questions: List<Question>, answers: Map<String, JsonElement>): List<String> {
        val vis = visibleMap(questions, answers)
        return questions.filter { it.required && vis[it.id] == true && !filled(it, answers[it.id]) }.map { it.label }
    }

    fun condMatch(parent: Question, cond: Question.Cond, answer: JsonElement?): Boolean {
        val type = parent.type
        val allowed = COND_BY_TYPE[type].orEmpty()
        var op = cond.op
        if (op.isEmpty()) op = if ("igual" in allowed) "igual" else "contem_um"
        if (op !in allowed) return false
        val want = condValue(parent, cond.value) ?: return false
        if (type == "check") return (if (answer.asStr() == "Sim") "sim" else "nao") == want
        if (type == "multipla") {
            @Suppress("UNCHECKED_CAST")
            val wantList = want as List<String>
            val got = (answer as? JsonArray)?.mapNotNull { it.asStr()?.trim()?.lowercase() }.orEmpty()
            if (got.isEmpty()) return false
            val hits = wantList.count { it.lowercase() in got }
            return when (op) {
                "contem_todos" -> hits == wantList.size
                "contem_um" -> hits > 0
                else -> hits == 0
            }
        }
        if (answer is JsonArray || answer is JsonObject) return false
        val a = answer.asStr()?.trim().orEmpty()
        if (a.isEmpty()) return false
        if (type == "itens") {
            val eq = a.lowercase() == (want as String).lowercase()
            return if (op == "igual") eq else !eq
        }
        val cmp: Int = if (type == "numerica" || type == "monetaria") {
            val n = condNum(a) ?: return false
            n.compareTo((want as String).toDouble())
        } else {
            val av = condValue(parent, JsonPrimitive(a)) as? String ?: return false
            av.compareTo(want as String).coerceIn(-1, 1)
        }
        return when (op) {
            "menor" -> cmp < 0
            "maior" -> cmp > 0
            "menor_igual" -> cmp <= 0
            "maior_igual" -> cmp >= 0
            "igual" -> cmp == 0
            "diferente" -> cmp != 0
            else -> false
        }
    }

    /** Resposta esperada no formato guardado: String, ou List<String> para múltipla; null = inválida. */
    fun condValue(parent: Question, v: JsonElement?): Any? {
        when (parent.type) {
            "multipla" -> {
                val raw = if (v is JsonArray) v.mapNotNull { it.asStr() } else v.asStr().orEmpty().split('§', '\n')
                val list = raw.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                if (list.any { it !in parent.options }) return null
                return list.ifEmpty { null }
            }
            "check" -> {
                val t = v.asStr()?.trim()?.lowercase().orEmpty()
                return when (t) {
                    "s", "sim", "verdadeiro", "true", "1" -> "sim"
                    "n", "nao", "não", "falso", "false", "0" -> "nao"
                    else -> null
                }
            }
        }
        if (v is JsonArray || v is JsonObject) return null
        var s = v.asStr()?.trim().orEmpty()
        if (s.isEmpty()) return null
        return when (parent.type) {
            "itens" -> s.takeIf { it in parent.options }
            "numerica", "monetaria" -> condNum(s)?.let { fmtNum(it) }
            "data" -> {
                Regex("""^(\d{2})/(\d{2})/(\d{4})$""").find(s)?.let { m -> s = "${m.groupValues[3]}-${m.groupValues[2]}-${m.groupValues[1]}" }
                s.takeIf { Regex("""^\d{4}-\d{2}-\d{2}$""").matches(it) && runCatching { LocalDate.parse(it) }.isSuccess }
            }
            "hora" -> {
                Regex("""^(\d{1,2}):(\d{2})$""").find(s)?.let { m -> s = m.groupValues[1].padStart(2, '0') + ":" + m.groupValues[2] }
                s.takeIf { Regex("""^([01]\d|2[0-3]):[0-5]\d$""").matches(it) }
            }
            else -> null
        }
    }

    /** "1.234,56", "R$ 10", "3.5" → número (mesma regra do PHP: com vírgula, o ponto é milhar). */
    fun condNum(v: String): Double? {
        var s = v.trim().replace(" ", "").replace("R$", "")
        if (s.contains(',')) s = s.replace(".", "").replace(',', '.')
        return s.toDoubleOrNull()
    }

    private fun fmtNum(n: Double): String {
        val s = String.format(java.util.Locale.ROOT, "%.4f", n)
        return s.trimEnd('0').trimEnd('.')
    }

    /** Lista de textos de uma resposta de múltipla escolha. */
    fun asList(v: JsonElement?): List<String> = v.asArr().mapNotNull { it.asStr() }
}
