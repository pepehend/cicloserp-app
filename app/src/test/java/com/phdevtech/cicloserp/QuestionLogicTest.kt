package com.phdevtech.cicloserp

import com.phdevtech.cicloserp.data.api.ApiClient
import com.phdevtech.cicloserp.data.model.Question
import com.phdevtech.cicloserp.data.model.QuestionLogic
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regras das perguntas condicionais: iguais às de Questionnaires::condMatch / OrderExec::visibleMap do servidor. */
class QuestionLogicTest {

    private fun q(id: String, type: String, required: Boolean = false, options: List<String> = emptyList(), cond: Question.Cond? = null) =
        Question(id, "Pergunta $id", type, required, null, options, false, cond)

    private fun s(v: String): JsonElement = JsonPrimitive(v)

    @Test
    fun checkSemMarcarContaComoFalso() {
        val qs = listOf(q("p1", "check"), q("p2", "area", cond = Question.Cond("p1", "igual", s("nao"))))
        assertEquals(true, QuestionLogic.visibleMap(qs, emptyMap())["p2"])
        assertEquals(false, QuestionLogic.visibleMap(qs, mapOf("p1" to s("Sim")))["p2"])
    }

    @Test
    fun condicionalNumericaCompara() {
        val qs = listOf(q("p1", "numerica"), q("p2", "texto", cond = Question.Cond("p1", "maior", s("10"))))
        assertEquals(false, QuestionLogic.visibleMap(qs, emptyMap())["p2"])           // sem resposta não atende
        assertEquals(false, QuestionLogic.visibleMap(qs, mapOf("p1" to s("9,5")))["p2"])
        assertEquals(true, QuestionLogic.visibleMap(qs, mapOf("p1" to s("10,5")))["p2"])
    }

    @Test
    fun multiplaContemUmETodos() {
        val opts = listOf("Filtro", "Correia", "Fusível")
        val parent = q("p1", "multipla", options = opts)
        val um = Question.Cond("p1", "contem_um", JsonArray(listOf(s("Filtro"), s("Correia"))))
        val todos = Question.Cond("p1", "contem_todos", JsonArray(listOf(s("Filtro"), s("Correia"))))
        val ans = JsonArray(listOf(s("Filtro")))
        assertTrue(QuestionLogic.condMatch(parent, um, ans))
        assertFalse(QuestionLogic.condMatch(parent, todos, ans))
        assertFalse(QuestionLogic.condMatch(parent, um, null))
    }

    @Test
    fun maeEscondidaEscondeAFilha() {
        val qs = listOf(
            q("p1", "check"),
            q("p2", "numerica", cond = Question.Cond("p1", "igual", s("nao"))),
            q("p3", "texto", cond = Question.Cond("p2", "maior", s("1"))),
        )
        // p1 marcado: p2 some, e p3 (que depende de p2) também, mesmo com resposta guardada
        val vis = QuestionLogic.visibleMap(qs, mapOf("p1" to s("Sim"), "p2" to s("5")))
        assertEquals(false, vis["p2"]); assertEquals(false, vis["p3"])
    }

    @Test
    fun progressoEObrigatoriasIgnoramPerguntasEscondidas() {
        val qs = listOf(q("p1", "check", required = true), q("p2", "area", required = true, cond = Question.Cond("p1", "igual", s("nao"))))
        val ans = mapOf("p1" to s("Sim"))
        assertEquals(1 to 1, QuestionLogic.progress(qs, ans))
        assertTrue(QuestionLogic.missing(qs, ans).isEmpty())
        assertEquals(listOf("Pergunta p2"), QuestionLogic.missing(qs, emptyMap()).drop(1))
    }

    @Test
    fun numerosNoFormatoBrasileiro() {
        assertEquals(1234.56, QuestionLogic.condNum("1.234,56")!!, 0.0001)
        assertEquals(10.0, QuestionLogic.condNum("R$ 10")!!, 0.0001)
        assertEquals(3.5, QuestionLogic.condNum("3.5")!!, 0.0001)
        assertNull(QuestionLogic.condNum("abc"))
    }

    @Test
    fun enderecoDoServidorNormalizado() {
        assertEquals("http://190.102.41.159/", ApiClient.normalizeBaseUrl("190.102.41.159")?.toString())
        assertEquals("https://erp.exemplo.com.br/sistema/", ApiClient.normalizeBaseUrl("erp.exemplo.com.br/sistema/index.php")?.toString())
        assertEquals("http://10.0.2.2:8099/", ApiClient.normalizeBaseUrl("10.0.2.2:8099")?.toString())
        assertNull(ApiClient.normalizeBaseUrl("   "))
        assertNotNull(ApiClient.normalizeBaseUrl("http://localhost:8088"))
    }
}
